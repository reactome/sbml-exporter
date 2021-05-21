package org.reactome.sbml.rel;

import static org.gk.model.ReactomeJavaConstants.accession;
import static org.gk.model.ReactomeJavaConstants.compartment;
import static org.gk.model.ReactomeJavaConstants.created;
import static org.gk.model.ReactomeJavaConstants.hasModifiedResidue;
import static org.gk.model.ReactomeJavaConstants.identifier;
import static org.gk.model.ReactomeJavaConstants.psiMod;
import static org.gk.model.ReactomeJavaConstants.pubMedIdentifier;
import static org.gk.model.ReactomeJavaConstants.referenceDatabase;
import static org.gk.model.ReactomeJavaConstants.summation;
import static org.gk.model.ReactomeJavaConstants.text;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.InvalidAttributeException;
import org.reactome.server.graph.domain.model.AbstractModifiedResidue;
import org.reactome.server.graph.domain.model.Affiliation;
import org.reactome.server.graph.domain.model.Compartment;
import org.reactome.server.graph.domain.model.DatabaseObject;
import org.reactome.server.graph.domain.model.GO_BiologicalProcess;
import org.reactome.server.graph.domain.model.InstanceEdit;
import org.reactome.server.graph.domain.model.LiteratureReference;
import org.reactome.server.graph.domain.model.Pathway;
import org.reactome.server.graph.domain.model.Person;
import org.reactome.server.graph.domain.model.PsiMod;
import org.reactome.server.graph.domain.model.Summation;
import org.reactome.server.graph.domain.model.TranslationalModification;
import org.reactome.server.tools.sbml.data.model.IdentifierBase;
import org.reactome.server.tools.sbml.data.model.ParticipantDetails;
import org.reactome.server.tools.sbml.data.model.ReactionBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to convert a GKInstance into an object in package org.reactome.server.graph.domain.model
 * so that it can be used by the original SBML converter.
 * @author wug
 *
 */
@SuppressWarnings("unchecked")
public class InstanceToModelConverter {
    private static Logger logger = LoggerFactory.getLogger(InstanceToModelConverter.class);
    // This should be fixed and hard coded since it must be true!
    private final String MODEL_PACKAGE_NAME = "org.reactome.server.graph.domain.model";
    // Converted values are catched
    private Map<GKInstance, DatabaseObject> instToObj;
    private ReactionHandler reactionHandler;
    
    public InstanceToModelConverter() {
        instToObj = new HashMap<>();
        reactionHandler = new ReactionHandler(this);
    }
    
    void reset() {
        instToObj.clear();
    }
    
    public DatabaseObject convert(GKInstance instance) throws Exception {
        DatabaseObject rtn = instToObj.get(instance);
        if (rtn != null)
            return rtn;
        String instClsName = instance.getSchemClass().getName();
        Class<?> modelCls = Class.forName(MODEL_PACKAGE_NAME + "." + instClsName);
        Object obj = modelCls.getDeclaredConstructor().newInstance();
        if (!(obj instanceof DatabaseObject)) {
            throw new IllegalArgumentException(instClsName + " is not defined.");
        }
        rtn = (DatabaseObject) obj;
        rtn.setDbId(instance.getDBID());
        rtn.setDisplayName(instance.getDisplayName());
        // Also need stable id
        GKInstance stableId = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.stableIdentifier);
        if (stableId != null)
            rtn.setStId((String)stableId.getAttributeValue(ReactomeJavaConstants.identifier));
        instToObj.put(instance, rtn);
        return rtn;
    }
    
    public void fillInReactionDetails(GKInstance rle, ReactionBase reactionBase) throws Exception {
        handleInstance(rle, reactionBase.getReactionLikeEvent(), Set.class);
        reactionHandler.handleReactionParticipants(rle, reactionBase);
        handleInstanceEdits(rle, reactionBase.getReactionLikeEvent());
        reactionHandler.handleGO(rle, reactionBase);
        reactionHandler.setLiteratureRefs(rle, reactionBase);
        reactionHandler.setDiseases(rle, reactionBase);
        reactionHandler.setXrefs(rle, reactionBase);
    }
    
    public void fillInPathwayDetails(GKInstance pathwayInst, Pathway pathwayObj) throws Exception {
        handleInstance(pathwayInst, pathwayObj, Set.class);
        handleInstanceEdits(pathwayInst, pathwayObj);
    }
    
    private void handleInstanceEdits(GKInstance rle, DatabaseObject rleObj) throws Exception {
        String[] attNames = {created,
//                ReactomeJavaConstants.modified,
                ReactomeJavaConstants.revised,
                ReactomeJavaConstants.authored};
        ValueAssigner<InstanceEdit> ieAssigner = (src, target) -> {
            ValueAssigner<Affiliation> affAssigner = (src2, target2) -> {
                // Just simple text
                List<String> names = src2.getAttributeValuesList(ReactomeJavaConstants.name);
                target2.setName(names);
            };
            ValueAssigner<Person> personAssigner = (src1, target1) -> {
                convertAttributeValues(src1, target1, ReactomeJavaConstants.affiliation, affAssigner);
                // First name and last name
                String firstName = (String) src1.getAttributeValue(ReactomeJavaConstants.firstname);
                target1.setFirstname(firstName);
                String surname = (String) src1.getAttributeValue(ReactomeJavaConstants.surname);
                target1.setSurname(surname);
            };
            convertAttributeValues(src, target, ReactomeJavaConstants.author, personAssigner);
            // We also need date
            String dateTime = (String) src.getAttributeValue(ReactomeJavaConstants.dateTime);
            target.setDateTime(dateTime);
        };
        for (String attName : attNames) {
            if (attName.equals(created))
                convertAttributeValues(rle, rleObj, attName, InstanceEdit.class, ieAssigner);
            else
                convertAttributeValues(rle, rleObj, attName, ieAssigner);
        }
        List<GKInstance> modified = rle.getAttributeValuesList(ReactomeJavaConstants.modified);
        if (modified != null && modified.size() > 0) {
            convertAttributeValues(modified.subList(modified.size() - 1, modified.size()), 
                                   rleObj,
                                   ReactomeJavaConstants.modified,
                                   InstanceEdit.class,
                                   ieAssigner);
        }
    }
    
    public void fillInPEDetails(GKInstance pe, ParticipantDetails details) throws Exception {
        handleInstance(pe, details.getPhysicalEntity(), List.class);
        handleReferencEntities(pe, details);
    }
    
    /**
     * We may use Reflection. However, it may be overkill for loading all properties. We will focus on
     * properties used for this SBML converting.
     * @param inst
     * @param instObj
     * @throws Exception
     */
    private void handleInstance(GKInstance inst, 
                                DatabaseObject instObj,
                                Class<?> inferredType) throws Exception {
        // Compartment
        ValueAssigner<Compartment> compAssigner = (src, target) -> target.setUrl(getUrl(src, accession));
        convertAttributeValues(inst,
                               instObj, 
                               compartment,
                               compAssigner);
        // Summation
        ValueAssigner<Summation> summAssigner = (src, target) -> target.setText((String) src.getAttributeValue(text));
        convertAttributeValues(inst, 
                               instObj, 
                               summation,
                               summAssigner);
        // LiteratureReference
        ValueAssigner<LiteratureReference> litAssigner = (src, target) -> {
            if (src.getSchemClass().isa(ReactomeJavaConstants.LiteratureReference)) {
                Integer pubmedId = (Integer) src.getAttributeValue(pubMedIdentifier);
                target.setPubMedIdentifier(pubmedId);
            }
        };
        convertAttributeValues(inst, instObj, ReactomeJavaConstants.literatureReference, litAssigner);
        // hasModifiedResidues
        ValueAssigner<AbstractModifiedResidue> modAssigner = (src, target) -> {
            if (target instanceof TranslationalModification) {
                // We need to do another layer of converting
                ValueAssigner<PsiMod> modAssigner1 = (src1, target1) -> target1.setUrl(getUrl(src1, identifier));
                // Since this is a single-valued attribute
                convertAttributeValues(src, target, psiMod, PsiMod.class, modAssigner1);
            }
        };
        // goBiolocaliProcess mainly for Event
        ValueAssigner<GO_BiologicalProcess> goAssigner = (src, target) -> {
            // Assign url for GO
            String url = getUrl(src, accession);
            if (url != null)
                target.setUrl(url);
        };
        convertAttributeValues(inst, 
                               instObj, 
                               ReactomeJavaConstants.goBiologicalProcess,
                               GO_BiologicalProcess.class, // This is used as single-valued
                               goAssigner);
        
        convertAttributeValues(inst, instObj, hasModifiedResidue, modAssigner);
        convertAttributeValues(inst, instObj, ReactomeJavaConstants.inferredTo, inferredType, null);
        convertAttributeValues(inst, instObj, ReactomeJavaConstants.inferredFrom, inferredType, null);
    }
    
    <T extends DatabaseObject> void convertAttributeValues(GKInstance pe,
                                                           DatabaseObject peObj,
                                                           String attName,
                                                           ValueAssigner<T> assigner) throws Exception {
        convertAttributeValues(pe, peObj, attName, List.class, assigner);
    }
    
    private <T extends DatabaseObject> void convertAttributeValues(GKInstance inst,
                                                                   DatabaseObject obj,
                                                                   String attName,
                                                                   Class<?> attType,
                                                                   ValueAssigner<T> assigner) throws Exception {
        if (!inst.getSchemClass().isValidAttribute(attName))
            return; // Nothing should be done.
        List<GKInstance> values = inst.getAttributeValuesList(attName);
        convertAttributeValues(values, obj, attName, attType, assigner);
    }

    private <T extends DatabaseObject> void convertAttributeValues(List<GKInstance> values,
                                                                   DatabaseObject obj,
                                                                   String attName,
                                                                   Class<?> attType,
                                                                   ValueAssigner<T> assigner) throws Exception {
        Method method = getSetMethod(obj, attName, attType);
        if (method == null)
            return; // Cannot do anything
        logger.debug(obj.getClass().getName() + "." + attName + " -> " + method);
        if (attType.equals(Set.class) || attType.equals(List.class)) {
            Collection<T> valueObjList = null;
            if (attType.equals(Set.class))
                valueObjList = new HashSet<>();
            else
                valueObjList = new ArrayList<>();
            if (values == null || values.size() == 0)
                method.invoke(obj, valueObjList);
            else {
                for (GKInstance value : values) {
                    // We have to cast a DatabaseObject into a specific subtype
                    // for the parametized TextAssigner.
                    T compObj = (T) convert(value);
                    valueObjList.add(compObj);
                    if (assigner != null)
                        assigner.assign(value, compObj);
                }
                method.invoke(obj, valueObjList);
            }
        }
        else {
            if (values == null || values.size() == 0)
                method.invoke(obj, (T) null); // Casting null is silly. But makes compiler happy.
            else {
                GKInstance value = values.get(0);
                T compObj = (T) convert(value);
                if (assigner != null)
                    assigner.assign(value, compObj);
                method.invoke(obj, compObj);
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
            String url = getUrl(refInst, ReactomeJavaConstants.identifier);
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
    
    String getUrl(GKInstance inst,
                  String idAttName) throws Exception {
        if (!(inst.getSchemClass().isValidAttribute(referenceDatabase)))
            logger.error(inst + " doesn't have a referenceDatabase slot.");
        GKInstance refDb = (GKInstance) inst.getAttributeValue(ReactomeJavaConstants.referenceDatabase);
        if (refDb == null)
            logger.error(inst + " doesn't have a referenceDatabase assigned.");
        String accessUrl = (String) refDb.getAttributeValue(ReactomeJavaConstants.accessUrl);
        if (accessUrl == null)
            logger.error(refDb + " doesn't have an accessUrl assigned.");
        String id = (String) inst.getAttributeValue(idAttName);
        if (id ==  null)
            logger.error(inst + " doesn't have a value assigned for " + idAttName + ".");
        if (accessUrl == null || id == null)
            return null;
        return accessUrl.replace("###ID###", id);
    }
    
    @FunctionalInterface interface ValueAssigner<T extends DatabaseObject> {
        public void assign(GKInstance source, T target) throws Exception;
    }
    
}
