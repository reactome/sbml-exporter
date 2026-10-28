package org.reactome.server.tools.sbml.data;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Retrieves the data in an efficient way in order to speed up the conversion process
 *
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 */
public final class DataFactory {

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
            // The summations' literature, as the relational converter wrote it: a LiteratureReference
            // by its PubMed id (the graph stores no url; this is graph-core's getUrl()), a URL by its
            // address, and the event's own references left to its annotation (Helper.addAnnotations).
            "OPTIONAL MATCH (rle)-[:summation]->(:Summation)-[:literatureReference]->(lit:Publication) WHERE NOT (rle)-[:literatureReference]->(lit) " +
            "WITH DISTINCT rle, goTerms, ecNumbers, COLLECT(DISTINCT CASE WHEN lit:URL THEN lit.uniformResourceLocator ELSE \"http://www.ncbi.nlm.nih.gov/pubmed/\" + lit.pubMedIdentifier END) AS literatureRefs " +
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
            // The parts as the relational converter found them: through components, members, candidates
            // and repeated units; a part reached by several paths counted on each path; and each part named by
            // its identifier, never an isoform's, in its database's access URL.
            "OPTIONAL MATCH path=(pe)-[:hasComponent|hasMember|hasCandidate|repeatedUnit|referenceEntity*]->(re:ReferenceEntity) " +
            "WITH pe, re, SUM(REDUCE(s = 1, x IN RELATIONSHIPS(path) | s * x.stoichiometry)) AS n " +
            "OPTIONAL MATCH (re)-[:referenceDatabase]->(db:ReferenceDatabase) " +
            "RETURN pe.stId AS pe, " +
            // One entry per reference entity, even when two share an identifier (isoforms), and none when
            // there is no reference entity, so a complex without one says "" rather than "()", as today
            "       COLLECT(CASE re WHEN NULL THEN NULL ELSE {n: n, id: re.identifier} END) AS ids, " +
            "       COLLECT(DISTINCT REPLACE(db.accessUrl, '###ID###', re.identifier)) AS urls";

    private DataFactory() {
    }

    public static Collection<ReactionBase> getReactionList(String eventStId, DatabaseObjectService ds, AdvancedDatabaseObjectService ads) {

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
                reactionBase.setCatalysts(getParticipantResults(ds, reactionBaseResult.getCatalysts()));
                reactionBase.setPositiveRegulators(getParticipantResults(ds, reactionBaseResult.getPositiveRegulators()));
                reactionBase.setNegativeRegulators(getParticipantResults(ds, reactionBaseResult.getNegativeRegulators()));
                reactionBases.add(reactionBase);
            }
            return reactionBases;
        } catch (CustomQueryException e) {
            // Not an empty list: that wrote a file with the model and none of its reactions,
            // and the run passed it as converted.
            throw new IllegalStateException("The reactions of " + eventStId + " could not be read", e);
        }
    }

    public static Collection<ParticipantDetails> getParticipantDetails(String eventStId, DatabaseObjectService ds, AdvancedDatabaseObjectService ads) {
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

                participantDetail.setPhysicalEntity(ds.findByIdNoRelations(participantDetailsResult.getPeStId()));
                participantDetails.add(participantDetail);
            }
            return participantDetails;
        } catch (CustomQueryException e) {
            // Not an empty list: that wrote a file with none of the event's species.
            throw new IllegalStateException("The participants of " + eventStId + " could not be read", e);
        }
    }

    public static List<Participant> getParticipantResults(DatabaseObjectService ds, List<ParticipantResult> participantsQueryResults) {
        List<Participant> participants = new ArrayList<>();
        for (ParticipantResult participantResult : participantsQueryResults) {
            Participant participant = new Participant();
            participant.setStoichiometry(participantResult.getStoichiometry());
            participant.setPhysicalEntity(ds.findByIdNoRelations(participantResult.getPhysicalEntity()));
            participants.add(participant);
        }
        return participants;
    }
}
