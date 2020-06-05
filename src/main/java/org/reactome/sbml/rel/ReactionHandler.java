package org.reactome.sbml.rel;

import static org.gk.model.ReactomeJavaConstants.accession;
import static org.gk.model.ReactomeJavaConstants.input;
import static org.gk.model.ReactomeJavaConstants.literatureReference;
import static org.gk.model.ReactomeJavaConstants.output;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.InvalidAttributeException;
import org.reactome.server.graph.domain.model.PhysicalEntity;
import org.reactome.server.tools.sbml.data.model.Participant;
import org.reactome.server.tools.sbml.data.model.ReactionBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unchecked")
public class ReactionHandler {
    private static final Logger logger = LoggerFactory.getLogger(ReactionHandler.class);
    private static final String EC_URL = "https://identifiers.org/ec-code/";
    private static final String PUBMED_URL = "http://www.ncbi.nlm.nih.gov/pubmed/";
    private InstanceToModelConverter converter;
    
    public ReactionHandler(InstanceToModelConverter converter) {
        this.converter = converter;
    }
    
    /**
     * The following implementation is based on this cypher query in class DAtaFactory:
     *      "OPTIONAL MATCH (rle)-[:goBiologicalProcess]->(gobp:GO_BiologicalProcess)  " +
            "OPTIONAL MATCH (rle)-[:catalystActivity]->(cat:CatalystActivity) " +
            "OPTIONAL MATCH (cat)-[:activity]->(gomf:GO_MolecularFunction)  " +
            "WITH DISTINCT rle, COLLECT(DISTINCT \"https://identifiers.org/ec-code/\" + gomf.ecNumber) AS ecNumbers, COLLECT(DISTINCT gomf.url) AS gomfs, COLLECT(DISTINCT gobp.url) AS gobps " +
            "WITH DISTINCT rle, ecNumbers, CASE SIZE(gomfs) WHEN 0 THEN gobps ELSE gomfs END as goTerms " +
     * @param rle
     * @param reactionBase
     * @throws Exception
     */
    void handleGO(GKInstance rle, ReactionBase reactionBase) throws Exception {
        // Handle the ec numbers here since they are associated with GO
        Set<String> ecNumbers = new HashSet<>();
        Collection<GKInstance> goTerms = new HashSet<>();
        List<GKInstance> cas = rle.getAttributeValuesList(ReactomeJavaConstants.catalystActivity);
        for (GKInstance ca : cas) {
            List<GKInstance> activity = ca.getAttributeValuesList(ReactomeJavaConstants.activity);
            goTerms.addAll(activity);
        }
        if (goTerms.size() > 0) {
            Set<String> ecValues = new HashSet<>();
            for (GKInstance goTerm : goTerms) 
                ecValues.addAll(goTerm.getAttributeValuesList(ReactomeJavaConstants.ecNumber));
            ecValues.forEach(v -> ecNumbers.add(EC_URL + v));
        }
        else 
            goTerms = rle.getAttributeValuesList(ReactomeJavaConstants.goBiologicalProcess);
        List<String> goTermList = new ArrayList<>();
        for (GKInstance goTerm : goTerms) {
            String url = converter.getUrl(goTerm, accession);
            goTermList.add(url);
        }
        reactionBase.setGoTerms(goTermList);
        reactionBase.setEcNumbers(ecNumbers.stream().sorted().collect(Collectors.toList()));
    }
    
    private List<Participant> convertToParticipants(Collection<GKInstance> pes) {
        List<Participant> parts = new ArrayList<>();
        pes.stream()
              .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
              .forEach((inst, stoi) -> {
                  Participant part = new Participant();
                  part.setStoichiometry(stoi.intValue());
                  PhysicalEntity pe = null;
                  try {
                      pe = (PhysicalEntity) converter.convert(inst);
                  } catch (Exception e) {
                      logger.error(e.getMessage(), e);
                  }
                  part.setPhysicalEntity(pe);
                  parts.add(part);
              });
        return parts;
    }
    
    void handleReactionParticipants(GKInstance rle, ReactionBase reactionBase) throws InvalidAttributeException, Exception {
        // Copy reaction participants now
        List<GKInstance> inputs = rle.getAttributeValuesList(input);
        List<Participant> inputParts = convertToParticipants(inputs);
        reactionBase.setInputs(inputParts);
        List<GKInstance> outputs = rle.getAttributeValuesList(output);
        List<Participant> outputParts = convertToParticipants(outputs);
        reactionBase.setOutputs(outputParts);
        // Regulations
        Collection<GKInstance> regulations = InstanceUtilities.getRegulations(rle);
        List<Participant> posRegParts = new ArrayList<>();
        List<Participant> negRegParts = new ArrayList<>();
        for (GKInstance regulation : regulations) {
            GKInstance regulator = (GKInstance) regulation.getAttributeValue(ReactomeJavaConstants.regulator);
            if (regulator == null)
                continue;
            Participant part = new Participant();
            part.setPhysicalEntity((PhysicalEntity)converter.convert(regulator));
            if (regulation.getSchemClass().isa(ReactomeJavaConstants.PositiveRegulation))
                posRegParts.add(part);
            else
                negRegParts.add(part);
        }
        reactionBase.setPositiveRegulators(posRegParts);
        reactionBase.setNegativeRegulators(negRegParts);
        // Catalyst
        List<GKInstance> cas = rle.getAttributeValuesList(ReactomeJavaConstants.catalystActivity);
        List<Participant> casParts = new ArrayList<>();
        for (GKInstance ca : cas) {
            GKInstance catalyst = (GKInstance) ca.getAttributeValue(ReactomeJavaConstants.physicalEntity);
            if (catalyst == null)
                continue;
            Participant part = new Participant();
            part.setPhysicalEntity((PhysicalEntity)converter.convert(catalyst));
            casParts.add(part);
        }
        reactionBase.setCatalysts(casParts);
    }

    /**
     * This is based on: 
     *      "OPTIONAL MATCH (rle)-[:summation|literatureReference*]->(lit:LiteratureReference) " +
            "WITH DISTINCT rle, goTerms, ecNumbers, COLLECT(DISTINCT lit.url) AS literatureRefs " +
       However, the literature reference attached to an Event is not handled here since it has been
       handled in other place, converter.handleInstance().
     * @param inst
     * @param base
     */
    void setLiteratureRefs(GKInstance inst, ReactionBase base) throws Exception {
        Set<GKInstance> refs = new HashSet<>();
        List<GKInstance> list = inst.getAttributeValuesList(ReactomeJavaConstants.summation);
        for (GKInstance sum : list) {
            refs.addAll(sum.getAttributeValuesList(literatureReference));
        }
        list = inst.getAttributeValuesList(ReactomeJavaConstants.literatureReference);
        refs.removeAll(list);
        List<String> urls = new ArrayList<>();
        for (GKInstance ref : refs) {
            if (ref.getSchemClass().isValidAttribute(ReactomeJavaConstants.pubMedIdentifier)) {
                Integer id = (Integer) ref.getAttributeValue(ReactomeJavaConstants.pubMedIdentifier);
                if (id != null)
                    urls.add(PUBMED_URL + id);
            }
            else if (ref.getSchemClass().isValidAttribute(ReactomeJavaConstants.uniformResourceLocator)) {
                String url = (String) ref.getAttributeValue(ReactomeJavaConstants.uniformResourceLocator);
                if (url != null)
                    urls.add(url);
            }
        }
        base.setLiteratureRefs(urls);
    }

    /**
     * @param inst
     * @param base
     */
    void setDiseases(GKInstance inst, ReactionBase base) throws Exception {
        List<GKInstance> list = inst.getAttributeValuesList(ReactomeJavaConstants.disease);
        List<String> urls = new ArrayList<>();
        for (GKInstance disease : list) {
            String url = converter.getUrl(disease, ReactomeJavaConstants.identifier);
            if (url != null)
                urls.add(url);
        }
        base.setDiseases(urls);
    }

    /**
     * This is based on:
     *      "OPTIONAL MATCH (rle)-[:crossReference]->(xref:DatabaseIdentifier) " +
            "WITH DISTINCT rle, goTerms, ecNumbers, literatureRefs, COLLECT(DISTINCT xref.url) AS xrefs " +
      To collect cross references.
     * @param inst
     * @param base
     */
    void setXrefs(GKInstance inst, ReactionBase base) throws Exception {
        List<GKInstance> list = inst.getAttributeValuesList(ReactomeJavaConstants.crossReference);
        List<String> urls = new ArrayList<>();
        for (GKInstance xref : list) {
            String url = converter.getUrl(xref, ReactomeJavaConstants.identifier);
            if (url != null)
                urls.add(url);
        }
        base.setXrefs(urls);
    }

}
