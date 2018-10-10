package org.reactome.server.tools.sbml.data;

import org.reactome.server.graph.exception.CustomQueryException;
import org.reactome.server.graph.service.AdvancedDatabaseObjectService;
import org.reactome.server.graph.utils.ReactomeGraphCore;
import org.reactome.server.tools.sbml.data.model.ParticipantDetails;
import org.reactome.server.tools.sbml.data.model.ReactionBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;

/**
 * Retrieves the data in an efficient way in order to speed up the conversion process
 *
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 */
public abstract class DataFactory {

    private static Logger logger = LoggerFactory.getLogger("sbml-exporter");

    private static final String REACTIONS_QUERY = "" +
            "OPTIONAL MATCH (rle1:ReactionLikeEvent{stId:{stId}}) " +
            "OPTIONAL MATCH (:Pathway{stId:{stId}})-[:hasEvent*]->(rle2:ReactionLikeEvent) " +
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
            "WITH DISTINCT rle, goTerms, ecNumbers, literatureRefs, xrefs, diseases, COLLECT(DISTINCT CASE pei WHEN NULL THEN NULL ELSE {n: i.stoichiometry,  pe: pei} END) AS inputs " +
            "OPTIONAL MATCH (rle)-[o:output]->(peo:PhysicalEntity) " +
            "WITH DISTINCT rle, goTerms, ecNumbers, literatureRefs, xrefs, diseases, inputs, COLLECT(DISTINCT CASE peo WHEN NULL THEN NULL ELSE {n: o.stoichiometry,  pe: peo} END) AS outputs " +
            "OPTIONAL MATCH (rle)-[:catalystActivity]->(:CatalystActivity)-[:physicalEntity]->(pec:PhysicalEntity) " +
            "WITH DISTINCT rle, goTerms, ecNumbers, literatureRefs, xrefs, diseases, inputs, outputs, COLLECT(DISTINCT CASE pec WHEN NULL THEN NULL ELSE {n: 0,  pe: pec} END) AS catalysts " +
            "OPTIONAL MATCH (rle)-[:regulatedBy]->(:PositiveRegulation)-[:regulator]->(pepr:PhysicalEntity) " +
            "WITH DISTINCT rle, goTerms, ecNumbers, literatureRefs, xrefs, diseases, inputs, outputs, catalysts, COLLECT(DISTINCT CASE pepr WHEN NULL THEN NULL ELSE {n: 0,  pe: pepr} END) AS positiveRegulators " +
            "OPTIONAL MATCH (rle)-[:regulatedBy]->(:NegativeRegulation)-[:regulator]->(penr:PhysicalEntity) " +
            "RETURN DISTINCT rle, goTerms, ecNumbers, literatureRefs, xrefs, diseases, inputs, outputs, catalysts, positiveRegulators, COLLECT(DISTINCT CASE penr WHEN NULL THEN NULL ELSE {n: 0,  pe: penr} END) AS negativeRegulators";

    private static final String PARTICIPANTS_QUERY = "" +
            "OPTIONAL MATCH (rle1:ReactionLikeEvent{stId:{stId}}) " +
            "OPTIONAL MATCH (:Pathway{stId:{stId}})-[:hasEvent*]->(rle2:ReactionLikeEvent) " +
            "WITH DISTINCT COLLECT(DISTINCT rle1) + COLLECT(DISTINCT rle2) AS rles " +
            "UNWIND rles AS rle " +
            "MATCH (rle)-[:input|output|catalystActivity|physicalEntity|regulatedBy|regulator*]->(pe:PhysicalEntity) " +
            "WITH COLLECT(DISTINCT pe) AS pes " +
            "UNWIND pes AS pe " +
            "MATCH path=(pe)-[:hasComponent|hasMember|repeatedUnit|referenceEntity*]->(re:ReferenceEntity) " +
            "WITH pe, re, REDUCE(s = 1, x IN RELATIONSHIPS(path) | s * x.stoichiometry) AS n " +
            "RETURN pe, " +
            "       COLLECT(DISTINCT {n: n, id: CASE re.variantIdentifier WHEN NULL THEN re.identifier ELSE re.variantIdentifier END}) AS ids, " +
            "       COLLECT(DISTINCT re.url) AS urls";

    public static Collection<ReactionBase> getReactionList(String eventStId) {
        AdvancedDatabaseObjectService ads = ReactomeGraphCore.getService(AdvancedDatabaseObjectService.class);
        try {
            return ads.getCustomQueryResults(ReactionBase.class, REACTIONS_QUERY, Collections.singletonMap("stId", eventStId));
        } catch (CustomQueryException e) {
            logger.error(e.getMessage(), e);
            System.exit(1);
            return null;
        }
    }

    public static Collection<ParticipantDetails> getParticipantDetails(String eventStId){
        AdvancedDatabaseObjectService ads = ReactomeGraphCore.getService(AdvancedDatabaseObjectService.class);
        try {
            return ads.getCustomQueryResults(ParticipantDetails.class, PARTICIPANTS_QUERY, Collections.singletonMap("stId", eventStId));
        } catch (CustomQueryException e) {
            logger.error(e.getMessage(), e);
            System.exit(1);
            return null;
        }
    }
}
