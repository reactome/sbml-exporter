package org.reactome.server.tools.sbml.data;

import org.reactome.server.graph.domain.model.PhysicalEntity;
import org.reactome.server.graph.exception.CustomQueryException;
import org.reactome.server.graph.service.AdvancedDatabaseObjectService;
import org.reactome.server.graph.service.DatabaseObjectService;
import org.reactome.server.tools.sbml.data.model.IdentifierBase;
import org.reactome.server.tools.sbml.data.model.Participant;
import org.reactome.server.tools.sbml.data.model.ParticipantDetails;
import org.reactome.server.tools.sbml.data.model.ReactionBase;
import org.reactome.server.tools.sbml.data.result.ParticipantDetailsResult;
import org.reactome.server.tools.sbml.data.result.ParticipantResult;
import org.reactome.server.tools.sbml.data.result.ReactionBaseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Retrieves the data in an efficient way in order to speed up the conversion process
 *
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 */
public abstract class DataFactory {

    private static Logger logger = LoggerFactory.getLogger("sbml-exporter");

    private static DatabaseObjectService ds;

    private static final String REACTIONS_QUERY = "" +
            "OPTIONAL MATCH (rle1:ReactionLikeEvent{stId:$stId}) " +
            "OPTIONAL MATCH (:Pathway{stId:$stId})-[:hasEvent*]->(rle2:ReactionLikeEvent) " +
            "WITH DISTINCT COLLECT(DISTINCT rle1) + COLLECT(DISTINCT rle2) AS rles " +
            "UNWIND rles AS rle " +
            "OPTIONAL MATCH (rle)-[:goBiologicalProcess]->(gobp:GO_BiologicalProcess)  " +
            "OPTIONAL MATCH (rle)-[:catalystActivity]->(cat:CatalystActivity) " +
            "OPTIONAL MATCH (cat)-[:activity]->(gomf:GO_MolecularFunction)  " +
            "WITH DISTINCT rle, COLLECT(DISTINCT \"https://identifiers.org/ec-code/\" + gomf.ecNumber) AS ecNumbers, COLLECT(DISTINCT gomf.url) AS gomfs, COLLECT(DISTINCT gobp.url) AS gobps " +
            "WITH DISTINCT rle, ecNumbers, CASE SIZE(gomfs) WHEN 0 THEN gobps ELSE gomfs END as goTerms " +
            "OPTIONAL MATCH (rle)-[:summation|literatureReference*]->(lit:LiteratureReference) " +
            "WITH DISTINCT rle, goTerms, ecNumbers, COLLECT(DISTINCT lit.url) AS literatureRefs " +
            "OPTIONAL MATCH (rle)-[:crossReference]->(xref:DatabaseIdentifier) " +
            "WITH DISTINCT rle, goTerms, ecNumbers, literatureRefs, COLLECT(DISTINCT xref.url) AS xrefs " +
            "OPTIONAL MATCH (rle)-[:disease]->(d:Disease) " +
            "WITH DISTINCT rle, goTerms, ecNumbers, literatureRefs, xrefs, COLLECT(DISTINCT d.url) AS diseases " +
            "OPTIONAL MATCH (rle)-[i:input]->(pei:PhysicalEntity) " +
            "WITH DISTINCT rle, goTerms, ecNumbers, literatureRefs, xrefs, diseases, COLLECT(DISTINCT CASE pei WHEN NULL THEN NULL ELSE {n: i.stoichiometry,  pe: pei.stId} END) AS inputs " +
            "OPTIONAL MATCH (rle)-[o:output]->(peo:PhysicalEntity) " +
            "WITH DISTINCT rle, goTerms, ecNumbers, literatureRefs, xrefs, diseases, inputs, COLLECT(DISTINCT CASE peo WHEN NULL THEN NULL ELSE {n: o.stoichiometry,  pe: peo.stId} END) AS outputs " +
            "OPTIONAL MATCH (rle)-[:catalystActivity]->(:CatalystActivity)-[:physicalEntity]->(pec:PhysicalEntity) " +
            "WITH DISTINCT rle, goTerms, ecNumbers, literatureRefs, xrefs, diseases, inputs, outputs, COLLECT(DISTINCT CASE pec WHEN NULL THEN NULL ELSE {n: 0,  pe: pec.stId} END) AS catalysts " +
            "OPTIONAL MATCH (rle)-[:regulatedBy]->(:PositiveRegulation)-[:regulator]->(pepr:PhysicalEntity) " +
            "WITH DISTINCT rle, goTerms, ecNumbers, literatureRefs, xrefs, diseases, inputs, outputs, catalysts, COLLECT(DISTINCT CASE pepr WHEN NULL THEN NULL ELSE {n: 0,  pe: pepr.stId} END) AS positiveRegulators " +
            "OPTIONAL MATCH (rle)-[:regulatedBy]->(:NegativeRegulation)-[:regulator]->(penr:PhysicalEntity) " +
            "RETURN DISTINCT rle.stId AS rle, goTerms, ecNumbers, literatureRefs, xrefs, diseases, inputs, outputs, catalysts, positiveRegulators, COLLECT(DISTINCT CASE penr WHEN NULL THEN NULL ELSE {n: 0,  pe: penr.stId} END) AS negativeRegulators";

    private static final String PARTICIPANTS_QUERY = "" +
            "OPTIONAL MATCH (rle1:ReactionLikeEvent{stId:$stId}) " +
            "OPTIONAL MATCH (:Pathway{stId:$stId})-[:hasEvent*]->(rle2:ReactionLikeEvent) " +
            "WITH DISTINCT COLLECT(DISTINCT rle1) + COLLECT(DISTINCT rle2) AS rles " +
            "UNWIND rles AS rle " +
            "MATCH (rle)-[:input|output|catalystActivity|physicalEntity|regulatedBy|regulator*]->(pe:PhysicalEntity) " +
            "WITH COLLECT(DISTINCT pe) AS pes " +
            "UNWIND pes AS pe " +
            //GEE do not have RE but they must be in the species (without identifier)
            "OPTIONAL MATCH path=(pe)-[:hasComponent|hasMember|repeatedUnit|referenceEntity*]->(re:ReferenceEntity) " +
            "WITH pe, re, REDUCE(s = 1, x IN RELATIONSHIPS(path) | s * x.stoichiometry) AS n " +
            "RETURN pe.stId AS pe, " +
            "       COLLECT(DISTINCT {" +
            "               n:  CASE n WHEN NULL THEN 1 ELSE n END, " +  //n is null when no RE present for a PE
            "               id: CASE re WHEN NULL THEN '' " +            //empty identifier when no RE present for a PE
            "                           ELSE CASE re.variantIdentifier WHEN NULL THEN re.identifier ELSE re.variantIdentifier END " +
            "                   END " +
            "       }) AS ids, " +
            "       COLLECT(DISTINCT re.url) AS urls";

    //work with DataFactoryHelper to autowire the DatabaseObjectService
    public DataFactory(DatabaseObjectService ds) {
        DataFactory.ds = ds;
    }

    public static Collection<ReactionBase> getReactionList(String eventStId, AdvancedDatabaseObjectService ads) {

        try {
            Collection<ReactionBaseResult> reactionBaseResults = ads.getCustomQueryResults(ReactionBaseResult.class, REACTIONS_QUERY, Collections.singletonMap("stId", eventStId));
            Collection<ReactionBase> reactionBases = new ArrayList<>();
            for (ReactionBaseResult reactionBaseResult : reactionBaseResults) {
                ReactionBase reactionBase = new ReactionBase();
                reactionBase.setRle(ds.findByIdNoRelations(reactionBaseResult.getRle()));
                reactionBase.setGoTerms(reactionBaseResult.getGoTerms());
                reactionBase.setEcNumbers(reactionBaseResult.getEcNumbers());
                reactionBase.setLiteratureRefs(reactionBaseResult.getLiteratureRefs());
                reactionBase.setXrefs(reactionBaseResult.getXrefs());
                reactionBase.setDiseases(reactionBaseResult.getDiseases());
                reactionBase.setInputs(getParticipantResults(ds, reactionBaseResult.getInputs()));
                reactionBase.setOutputs(getParticipantResults(ds, reactionBaseResult.getOutputs()));
                reactionBase.setCatalysts(getParticipantResults(ds, reactionBaseResult.getPositiveRegulators()));
                reactionBase.setNegativeRegulators(getParticipantResults(ds, reactionBaseResult.getPositiveRegulators()));
                reactionBases.add(reactionBase);
            }
            return reactionBases;
        } catch (CustomQueryException e) {
            logger.error(e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public static Collection<ParticipantDetails> getParticipantDetails(String eventStId, AdvancedDatabaseObjectService ads) {
      //  DatabaseObjectService ds = ReactomeGraphCore.getService(DatabaseObjectService.class);
        try {
            Collection<ParticipantDetailsResult> participantDetailsResults = ads.getCustomQueryResults(ParticipantDetailsResult.class, PARTICIPANTS_QUERY, Collections.singletonMap("stId", eventStId));
            Collection<ParticipantDetails> participantDetails = new ArrayList<>();
            for (ParticipantDetailsResult participantDetailsResult : participantDetailsResults) {
                ParticipantDetails participantDetail = new ParticipantDetails();

                for(IdentifierBase identifierBase: participantDetailsResult.getIds()){
                    participantDetail.addIdentifierBase(identifierBase);
                }
                for(String url: participantDetailsResult.getUrls()){
                    participantDetail.addUrl(url);
                }

                String peStId = participantDetailsResult.getPeStId();
                PhysicalEntity pe = ds.findByIdNoRelations(participantDetailsResult.getPeStId());
                System.out.println(peStId + "" + pe.getDisplayName());
                participantDetail.setPhysicalEntity(ds.findByIdNoRelations(participantDetailsResult.getPeStId()));
                participantDetails.add(participantDetail);
            }
            return participantDetails;
        } catch (CustomQueryException e) {
            logger.error(e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public static List<Participant> getParticipantResults(DatabaseObjectService ds, List<ParticipantResult> participantsQueryResults) {
        List<Participant> participants = new ArrayList<>();
        Participant participant = new Participant();
        for (ParticipantResult participantResult : participantsQueryResults) {
            participant.setStoichiometry(participantResult.getStoichiometry());
            participant.setPhysicalEntity(ds.findByIdNoRelations(participantResult.getPhysicalEntity()));
            participants.add(participant);
        }
        return participants;
    }
}
