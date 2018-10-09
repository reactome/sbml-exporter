package org.reactome.server.tools.sbml.fetcher;

import org.reactome.server.graph.exception.CustomQueryException;
import org.reactome.server.graph.service.AdvancedDatabaseObjectService;
import org.reactome.server.graph.utils.ReactomeGraphCore;
import org.reactome.server.tools.sbml.fetcher.model.ReactionBase;

import java.util.Collection;
import java.util.Collections;

/**
 * Retrieves the data in an efficient way in order to speed up the conversion process
 *
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 */
public abstract class DataFactory {

    private static final String QUERY = "" +
            "OPTIONAL MATCH (rle1:ReactionLikeEvent{stId:{stId}}) " +
            "OPTIONAL MATCH (:Pathway{stId:{stId}})-[:hasEvent*]->(rle2:ReactionLikeEvent)  " +
            "WITH DISTINCT COLLECT(DISTINCT rle1) + COLLECT(DISTINCT rle2) AS rles  " +
            "UNWIND rles AS rle  " +
            "OPTIONAL MATCH (rle)-[:goBiologicalProcess]->(gobp:GO_BiologicalProcess)  " +
            "OPTIONAL MATCH (rle)-[:catalystActivity]->(cat:CatalystActivity) " +
            "OPTIONAL MATCH (cat)-[:activity]->(gomf:GO_MolecularFunction)  " +
            "WITH DISTINCT rle, COLLECT(DISTINCT \"https://identifiers.org/ec-code/\" + gomf.ecNumber) AS ecNumbers, COLLECT(DISTINCT gomf.url) AS gomfs, COLLECT(DISTINCT gobp.url) AS gobps " +
            "WITH DISTINCT rle, ecNumbers, CASE SIZE(gomfs) WHEN 0 THEN gobps ELSE gomfs END as goTerms " +
            "OPTIONAL MATCH (rle)-[:summation|literatureReference*]->(lit:LiteratureReference) " +
            "WITH DISTINCT rle, goTerms, ecNumbers, COLLECT(DISTINCT lit.url) AS literatureRefs " +
            "OPTIONAL MATCH (rle)-[:crossReference]->(xref:DatabaseIdentifier) " +
            "WITH DISTINCT rle, goTerms, ecNumbers, literatureRefs, COLLECT(DISTINCT xref.url) AS xrefs " +
            "OPTIONAL MATCH (rle)-[:disease]->(d:Disease)  " +
            "WITH DISTINCT rle, goTerms, ecNumbers, literatureRefs, xrefs, COLLECT(DISTINCT d.url) AS diseases " +
             //Inputs
            "OPTIONAL MATCH (rle)-[i:input]->(pei:PhysicalEntity)-[:hasComponent|hasMember|repeatedUnit|referenceEntity*]->(rei:ReferenceEntity) " +
            "WITH DISTINCT rle, goTerms, ecNumbers, literatureRefs, xrefs, diseases, i, pei, " +
            "              COLLECT(DISTINCT CASE rei.variantIdentifier WHEN NULL THEN rei.identifier ELSE rei.variantIdentifier END) AS ids, COLLECT(DISTINCT rei.url) AS urls " +
            "WITH DISTINCT rle, goTerms, ecNumbers, literatureRefs, xrefs, diseases, " +
            "              COLLECT(DISTINCT CASE pei WHEN NULL THEN NULL ELSE {n: i.stoichiometry,  pe: pei, ids: ids, urls: urls} END) AS inputs " +
             //Outputs
            "OPTIONAL MATCH (rle)-[o:output]->(peo:PhysicalEntity)-[:hasComponent|hasMember|repeatedUnit|referenceEntity*]->(reo:ReferenceEntity) " +
            "WITH DISTINCT rle, goTerms, ecNumbers, literatureRefs, xrefs, diseases, inputs, o, peo, " +
            "              COLLECT(DISTINCT CASE reo.variantIdentifier WHEN NULL THEN reo.identifier ELSE reo.variantIdentifier END) AS ids, COLLECT(DISTINCT reo.url) AS urls " +
            "WITH DISTINCT rle, goTerms, ecNumbers, literatureRefs, xrefs, diseases, inputs, " +
            "              COLLECT(DISTINCT CASE peo WHEN NULL THEN NULL ELSE {n: o.stoichiometry,  pe: peo, ids: ids, urls: urls} END) AS outputs " +
             //Catalyst activities
            "OPTIONAL MATCH (rle)-[:catalystActivity]->(cat:CatalystActivity)-[:physicalEntity]->(pec:PhysicalEntity)-[:hasComponent|hasMember|repeatedUnit|referenceEntity*]->(rec:ReferenceEntity) " +
            "WITH DISTINCT rle, goTerms, ecNumbers, literatureRefs, xrefs, diseases, inputs, outputs, pec, " +
            "              COLLECT(DISTINCT CASE rec.variantIdentifier WHEN NULL THEN rec.identifier ELSE rec.variantIdentifier END) AS ids, COLLECT(DISTINCT rec.url) AS urls " +
            "WITH DISTINCT rle, goTerms, ecNumbers, literatureRefs, xrefs, diseases, inputs, outputs, " +
            "              COLLECT(DISTINCT CASE pec WHEN NULL THEN NULL ELSE {n: 0,  pe: pec, ids: ids, urls: urls} END) AS catalysts " +
             //Positive regulators
            "OPTIONAL MATCH (rle)-[:regulatedBy]->(:PositiveRegulation)-[:regulator]->(pepr:PhysicalEntity)-[:hasComponent|hasMember|repeatedUnit|referenceEntity*]->(repr:ReferenceEntity)  " +
            "WITH DISTINCT rle, goTerms, ecNumbers, literatureRefs, xrefs, diseases, inputs, outputs, catalysts, pepr, " +
            "              COLLECT(DISTINCT CASE repr.variantIdentifier WHEN NULL THEN repr.identifier ELSE repr.variantIdentifier END) AS ids, COLLECT(DISTINCT repr.url) AS urls " +
            "WITH DISTINCT rle, goTerms, ecNumbers, literatureRefs, xrefs, diseases, inputs, outputs, catalysts, " +
            "              COLLECT(DISTINCT CASE pepr WHEN NULL THEN NULL ELSE {n: 0,  pe: pepr, ids: ids, urls: urls} END) AS positiveRegulators " +
             //Negative regulators
            "OPTIONAL MATCH (rle)-[:regulatedBy]->(:NegativeRegulation)-[:regulator]->(penr:PhysicalEntity)-[:hasComponent|hasMember|repeatedUnit|referenceEntity*]->(renr:ReferenceEntity)  " +
            "WITH DISTINCT rle, goTerms, ecNumbers, literatureRefs, xrefs, diseases, inputs, outputs, catalysts, positiveRegulators, penr, " +
            "              COLLECT(DISTINCT CASE renr.variantIdentifier WHEN NULL THEN renr.identifier ELSE renr.variantIdentifier END) AS ids, COLLECT(DISTINCT renr.url) AS urls " +
            "" +
            "RETURN DISTINCT rle, goTerms, ecNumbers, literatureRefs, xrefs, diseases, inputs, outputs, catalysts, positiveRegulators, " +
            "              COLLECT(DISTINCT CASE penr WHEN NULL THEN NULL ELSE {n: 0,  pe: penr, ids: ids, urls: urls} END) AS negativeRegulators";


    public static Collection<ReactionBase> getReactionList(String eventStId) {
        AdvancedDatabaseObjectService ads = ReactomeGraphCore.getService(AdvancedDatabaseObjectService.class);
        try {
            return ads.getCustomQueryResults(ReactionBase.class, QUERY, Collections.singletonMap("stId", eventStId));
        } catch (CustomQueryException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return null;
    }
}
