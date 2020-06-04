package org.reactome.sbml.rel;

import static org.gk.model.ReactomeJavaConstants.accession;
import static org.gk.model.ReactomeJavaConstants.compartment;
import static org.gk.model.ReactomeJavaConstants.hasModifiedResidue;
import static org.gk.model.ReactomeJavaConstants.identifier;
import static org.gk.model.ReactomeJavaConstants.psiMod;
import static org.gk.model.ReactomeJavaConstants.pubMedIdentifier;
import static org.gk.model.ReactomeJavaConstants.referenceDatabase;
import static org.gk.model.ReactomeJavaConstants.summation;
import static org.gk.model.ReactomeJavaConstants.text;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.InvalidAttributeException;
import org.reactome.server.graph.domain.model.AbstractModifiedResidue;
import org.reactome.server.graph.domain.model.Compartment;
import org.reactome.server.graph.domain.model.DatabaseObject;
import org.reactome.server.graph.domain.model.LiteratureReference;
import org.reactome.server.graph.domain.model.PhysicalEntity;
import org.reactome.server.graph.domain.model.PsiMod;
import org.reactome.server.graph.domain.model.Summation;
import org.reactome.server.graph.domain.model.TranslationalModification;
import org.reactome.server.tools.sbml.data.model.IdentifierBase;
import org.reactome.server.tools.sbml.data.model.ParticipantDetails;

/**
 * This class is used to convert a GKInstance into an object in package org.reactome.server.graph.domain.model
 * so that it can be used by the original SBML converter.
 * @author wug
 *
 */
@SuppressWarnings("unchecked")
public class InstanceToModelConverter {
    // This should be fixed and hard coded since it must be true!
    private final String MODEL_PACKAGE_NAME = "org.reactome.server.graph.domain.model";
    
    public InstanceToModelConverter() {
    }
    
    public DatabaseObject convert(GKInstance instance) throws Exception {
        String instClsName = instance.getSchemClass().getName();
        Class<?> modelCls = Class.forName(MODEL_PACKAGE_NAME + "." + instClsName);
        Object obj = modelCls.newInstance();
        if (!(obj instanceof DatabaseObject)) {
            throw new IllegalArgumentException(instClsName + " is not defined.");
        }
        DatabaseObject rtn = (DatabaseObject) obj;
        rtn.setDbId(instance.getDBID());
        rtn.setDisplayName(instance.getDisplayName());
        // Also need stable id
        GKInstance stableId = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.stableIdentifier);
        if (stableId != null)
            rtn.setStId((String)stableId.getAttributeValue(ReactomeJavaConstants.identifier));
        return rtn;
    }
    
    public void fillInDetails(GKInstance pe, ParticipantDetails details) throws Exception {
        handlePhysicalEntity(pe, details.getPhysicalEntity());
        handleReferencEntities(pe, details);
    }
    
    /**
     * We may use Reflection. However, it may be overkill for loading all properties. We will focus on
     * properties used for this SBML converting.
     * @param pe
     * @param peObj
     * @throws Exception
     */
    private void handlePhysicalEntity(GKInstance pe, PhysicalEntity peObj) throws Exception {
        // Compartment
        ValueAssigner<Compartment> compAssigner = (src, target) -> target.setUrl(getUrl(src, accession));
        convertAttributeValues(pe,
                               peObj, 
                               compartment,
                               compAssigner);
        // Summation
        ValueAssigner<Summation> summAssigner = (src, target) -> target.setText((String) src.getAttributeValue(text));
        convertAttributeValues(pe, 
                               peObj, 
                               summation,
                               summAssigner);
        // LiteratureReference
        ValueAssigner<LiteratureReference> litAssigner = (src, target) -> {
            if (src.getSchemClass().isa(ReactomeJavaConstants.LiteratureReference)) {
                Integer pubmedId = (Integer) src.getAttributeValue(pubMedIdentifier);
                target.setPubMedIdentifier(pubmedId);
            }
        };
        convertAttributeValues(pe, peObj, ReactomeJavaConstants.literatureReference, litAssigner);
        // hasModifiedResidues
        ValueAssigner<AbstractModifiedResidue> modAssigner = (src, target) -> {
            if (target instanceof TranslationalModification) {
                // We need to do another layer of converting
                ValueAssigner<PsiMod> modAssigner1 = (src1, target1) -> target1.setUrl(getUrl(src1, identifier));
                // Since this is a single-valued attribute
                convertAttributeValues(src, target, psiMod, PsiMod.class, modAssigner1);
            }
        };
        convertAttributeValues(pe, peObj, hasModifiedResidue, modAssigner);
        // Infer to and from
        convertAttributeValues(pe, peObj, ReactomeJavaConstants.inferredTo, null);
        convertAttributeValues(pe, peObj, ReactomeJavaConstants.inferredFrom, null);
    }
    
    @FunctionalInterface
    private interface ValueAssigner<T extends DatabaseObject> {
        public void assign(GKInstance source, T target) throws Exception;
    }
    
    private <T extends DatabaseObject> void convertAttributeValues(GKInstance pe,
                                                                   DatabaseObject peObj,
                                                                   String attName,
                                                                   ValueAssigner<T> assigner) throws Exception {
        convertAttributeValues(pe, peObj, attName, List.class, assigner);
    }
    
    private <T extends DatabaseObject> void convertAttributeValues(GKInstance pe,
                                                                   DatabaseObject peObj,
                                                                   String attName,
                                                                   Class<?> attType,
                                                                   ValueAssigner<T> assigner) throws Exception {
        if (!pe.getSchemClass().isValidAttribute(attName))
            return; // Nothing should be done.
        Method method = getSetMethod(peObj, attName, attType);
        if (method == null)
            return; // Cannot do anything
        List<GKInstance> values = pe.getAttributeValuesList(attName);
        if (pe.getSchemClass().getAttribute(attName).isMultiple()) {
            if (values == null || values.size() == 0)
                method.invoke(peObj, Collections.EMPTY_LIST);
            else {
                List<T> valueObjList = new ArrayList<>();
                for (GKInstance value : values) {
                    // We have to cast a DatabaseObject into a specific subtype
                    // for the parametized TextAssigner.
                    T compObj = (T) convert(value);
                    valueObjList.add(compObj);
                    if (assigner != null)
                        assigner.assign(value, compObj);
                }
                method.invoke(peObj, valueObjList);
            }
        }
        else {
            if (values == null || values.size() == 0)
                method.invoke(peObj, (T) null); // Casting null is silly. But makes compiler happy.
            else {
                GKInstance value = values.get(0);
                T compObj = (T) convert(value);
                if (assigner != null)
                    assigner.assign(value, compObj);
                method.invoke(peObj, compObj);
            }
        }
    }
    
    private Method getSetMethod(DatabaseObject peObj,
                                String attName,
                                Class<?> type) throws Exception {
        String methodName = "set" + attName.substring(0, 1).toUpperCase() + attName.substring(1);
//        System.out.println(methodName + " -> " + peObj);
        Method rtn = peObj.getClass().getMethod(methodName, type);
        return rtn;
    }

    private void handleReferencEntities(GKInstance pe, ParticipantDetails details)
            throws Exception, InvalidAttributeException {
        List<GKInstance> refEntities = new ArrayList<>();
        // We need a list here and cannot use the methods provided in InstanceUtilities.
        String[] attributes = new String[] {ReactomeJavaConstants.hasComponent,
                                            ReactomeJavaConstants.hasMember,
                                            ReactomeJavaConstants.hasCandidate,
                                            ReactomeJavaConstants.repeatedUnit};
        // Perform a depth first search
        grepReferenceEntities(pe, refEntities, attributes);
        Map<GKInstance, Long> refInstToCount = refEntities.stream()
                                                          .collect(Collectors.groupingBy(Function.identity(),
                                                                                          Collectors.counting()));
        // Get ids from ReferenceEntities and the stoichiometries
        for (GKInstance refInst : refInstToCount.keySet()) {
            Long stoi = refInstToCount.get(refInst);
            String identifier = (String) refInst.getAttributeValue(ReactomeJavaConstants.identifier);
            IdentifierBase ibase = new IdentifierBase();
            ibase.setId(identifier);
            ibase.setN(stoi.intValue());
            details.addIdentifierBase(ibase);
            // Need urls for ReferenceEntities
            String url = getUrl(refInst, identifier);
            if (url != null)
                details.addUrl(url);
        }
    }
    
    private void grepReferenceEntities(GKInstance pe, 
                                       List<GKInstance> refEntities,
                                       String[] checkAtts) throws Exception {
        if (pe.getSchemClass().isValidAttribute(ReactomeJavaConstants.referenceEntity)) {
            GKInstance refEntity = (GKInstance) pe.getAttributeValue(ReactomeJavaConstants.referenceEntity);
            if (refEntity != null)
                refEntities.add(refEntity);
        }
        // If the above is true, usually there is no need here. However, this check may be more robust for the future
        for (String att : checkAtts) {
            if (!pe.getSchemClass().isValidAttribute(att))
                continue;
            List<GKInstance> values = pe.getAttributeValuesList(att);
            if (values == null || values.size() == 0)
                continue;
            for (GKInstance value : values)
                grepReferenceEntities(value, refEntities, checkAtts);
        }
    }
    
    private String getUrl(GKInstance inst,
                          String id) throws Exception {
        if (!(inst.getSchemClass().isValidAttribute(referenceDatabase)))
            throw new IllegalArgumentException(inst + " doesn't have a referenceDatabase slot.");
        GKInstance refDb = (GKInstance) inst.getAttributeValue(ReactomeJavaConstants.referenceDatabase);
        if (refDb == null)
            throw new IllegalArgumentException(inst + " doesn't have a referenceDatabase assigned.");
        String accessUrl = (String) refDb.getAttributeValue(ReactomeJavaConstants.accessUrl);
        if (accessUrl == null)
            throw new IllegalArgumentException(refDb + " doesn't have an accessUrl assigned.");
        return accessUrl.replace("###ID###", id);
    }
    
}
