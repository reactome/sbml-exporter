package org.reactome.server.tools.sbml.fetcher;

import org.reactome.server.graph.exception.CustomQueryException;
import org.reactome.server.graph.service.AdvancedDatabaseObjectService;
import org.reactome.server.graph.utils.ReactomeGraphCore;
import org.reactome.server.tools.sbml.fetcher.model.ReactionBase;

import java.util.Collection;
import java.util.Collections;

public abstract class DataFactory {

    private static final String QUERY1 = "" +
            "OPTIONAL MATCH (rle1:ReactionLikeEvent{stId:{stId}}) " +
            "OPTIONAL MATCH (:Pathway{stId:{stId}})-[:hasEvent*]->(rle2:ReactionLikeEvent) " +
            "WITH DISTINCT COLLECT(DISTINCT rle1) + COLLECT(DISTINCT rle2) AS rles " +
            "UNWIND rles AS rle " +
            "OPTIONAL MATCH (rle)-[:goBiologicalProcess]->(gobp:GO_BiologicalProcess) " +
            "OPTIONAL MATCH (rle)-[:summation|literatureReference*]->(lit:LiteratureReference) " +
            "OPTIONAL MATCH (rle)-[:crossReference]->(xref:DatabaseIdentifier)" +
            "OPTIONAL MATCH (rle)-[:disease]->(d:Disease) " +
            "OPTIONAL MATCH (rle)-[i:input]->(pei:PhysicalEntity)-[:hasComponent|hasMember|repeatedUnit|referenceEntity*]->(rei:ReferenceEntity) " +
            "OPTIONAL MATCH (rle)-[o:output]->(peo:PhysicalEntity)-[:hasComponent|hasMember|repeatedUnit|referenceEntity*]->(reo:ReferenceEntity) " +
            "OPTIONAL MATCH (rle)-[:regulatedBy]->(:PositiveRegulation)-[:regulator]->(pepr:PhysicalEntity)-[:hasComponent|hasMember|repeatedUnit|referenceEntity*]->(repr:ReferenceEntity) " +
            "OPTIONAL MATCH (rle)-[:regulatedBy]->(:NegativeRegulation)-[:regulator]->(penr:PhysicalEntity)-[:hasComponent|hasMember|repeatedUnit|referenceEntity*]->(renr:ReferenceEntity) " +
            "OPTIONAL MATCH (rle)-[:catalystActivity]->(cat:CatalystActivity)-[:physicalEntity]->(pec:PhysicalEntity)-[:hasComponent|hasMember|repeatedUnit|referenceEntity*]->(rec:ReferenceEntity) " +
            "OPTIONAL MATCH (cat)-[:activity]->(gomf:GO_MolecularFunction) " +
            "OPTIONAL MATCH (pei)-[:compartment]->(cpei:Compartment) " +
            "OPTIONAL MATCH (peo)-[:compartment]->(cpeo:Compartment) " +
            "OPTIONAL MATCH (pec)-[:compartment]->(cpec:Compartment) " +
            "OPTIONAL MATCH (pepr)-[:compartment]->(cpepr:Compartment) " +
            "OPTIONAL MATCH (penr)-[:compartment]->(cpenr:Compartment) " +
            "WITH DISTINCT rle, " +
            "              CASE gobp WHEN NULL THEN (CASE gomf WHEN NULL THEN [] ELSE COLLECT(DISTINCT gomf.url) END) ELSE [gobp.url] END AS goTerms, " +
            "              COLLECT(\"https://identifiers.org/ec-code/\" + gomf.ecNumber) AS ecNumbers, " +
            "              COLLECT(lit.url) AS literatureRefs, " +
            "              COLLECT(xref.url) AS xrefs, " +
            "              COLLECT(d.url) AS diseases, " +
            "              i, pei, COLLECT(DISTINCT rei.databaseName + \":\" + rei.identifier) AS reis, COLLECT(DISTINCT cpei.dbId) AS cpeis, " +
            "              o, peo, COLLECT(DISTINCT reo.databaseName + \":\" + reo.identifier) AS reos, COLLECT(DISTINCT cpeo.dbId) AS cpeos, " +
            "              pec, COLLECT(DISTINCT rec.databaseName + \":\" + rec.identifier) AS recs, COLLECT(DISTINCT cpec.dbId) AS cpecs, " +
            "              pepr, COLLECT(DISTINCT repr.databaseName + \":\" + repr.identifier) AS reprs, COLLECT(DISTINCT cpepr.dbId) AS cpeprs, " +
            "              penr, COLLECT(DISTINCT renr.databaseName + \":\" + renr.identifier) AS renrs, COLLECT(DISTINCT cpenr.dbId) AS cpenrs " +
            "RETURN rle.dbId AS dbId," +
            "       rle.stId AS stId," +
            "       rle.displayName AS displayName," +
            "       rle.schemaClass AS schemaClass, " +
            "       goTerms, ecNumbers, literatureRefs, xrefs, diseases, " +
            "       COLLECT(DISTINCT CASE pei WHEN NULL THEN NULL ELSE " +
            "               { " +
            "                 n:    i.stoichiometry, " +
            "                 dbId: pei.dbId, " +
            "                 stId: pei.stId, " +
            "                 sc:   pei.schemaClass, " +
            "                 accs: reis " +
            "               } END) AS inputs, " +
            "       COLLECT(DISTINCT CASE peo WHEN NULL THEN NULL ELSE " +
            "               { " +
            "                 n:    o.stoichiometry, " +
            "                 dbId: peo.dbId, " +
            "                 stId: peo.stId, " +
            "                 sc:   peo.schemaClass, " +
            "                 accs: reos " +
            "               } END) AS outputs, " +
            "       COLLECT(DISTINCT CASE pec WHEN NULL THEN NULL ELSE " +
            "               { " +
            "                 n:    0, " +
            "                 dbId: pec.dbId, " +
            "                 stId: pec.stId, " +
            "                 sc:   pec.schemaClass, " +
            "                 accs: recs " +
            "               } END) AS catalysts, " +
            "       COLLECT(DISTINCT CASE pepr WHEN NULL THEN NULL ELSE " +
            "               { " +
            "                 n:    0, " +
            "                 dbId: pepr.dbId, " +
            "                 stId: pepr.stId, " +
            "                 sc:   pepr.schemaClass, " +
            "                 accs: reprs " +
            "               } END) AS positiveRegulators," +
            "       COLLECT(DISTINCT CASE penr WHEN NULL THEN NULL ELSE " +
            "               { " +
            "                 n:    0, " +
            "                 dbId: penr.dbId, " +
            "                 stId: penr.stId, " +
            "                 sc:   penr.schemaClass, " +
            "                 accs: renrs " +
            "               } END) AS negativeRegulators";

    private static final String QUERY = "" +
            "OPTIONAL MATCH (rle1:ReactionLikeEvent{stId:{stId}}) " +
            "OPTIONAL MATCH (:Pathway{stId:{stId}})-[:hasEvent*]->(rle2:ReactionLikeEvent) " +
            "WITH DISTINCT COLLECT(DISTINCT rle1) + COLLECT(DISTINCT rle2) AS rles " +
            "UNWIND rles AS rle " +
            "OPTIONAL MATCH (rle)-[:goBiologicalProcess]->(gobp:GO_BiologicalProcess) " +
            "OPTIONAL MATCH (rle)-[:summation|literatureReference*]->(lit:LiteratureReference) " +
            "OPTIONAL MATCH (rle)-[:crossReference]->(xref:DatabaseIdentifier)" +
            "OPTIONAL MATCH (rle)-[:disease]->(d:Disease) " +
            "OPTIONAL MATCH (rle)-[i:input]->(pei:PhysicalEntity)-[:hasComponent|hasMember|repeatedUnit|referenceEntity*]->(rei:ReferenceEntity) " +
            "OPTIONAL MATCH (rle)-[o:output]->(peo:PhysicalEntity)-[:hasComponent|hasMember|repeatedUnit|referenceEntity*]->(reo:ReferenceEntity) " +
            "OPTIONAL MATCH (rle)-[:regulatedBy]->(:PositiveRegulation)-[:regulator]->(pepr:PhysicalEntity)-[:hasComponent|hasMember|repeatedUnit|referenceEntity*]->(repr:ReferenceEntity) " +
            "OPTIONAL MATCH (rle)-[:regulatedBy]->(:NegativeRegulation)-[:regulator]->(penr:PhysicalEntity)-[:hasComponent|hasMember|repeatedUnit|referenceEntity*]->(renr:ReferenceEntity) " +
            "OPTIONAL MATCH (rle)-[:catalystActivity]->(cat:CatalystActivity)-[:physicalEntity]->(pec:PhysicalEntity)-[:hasComponent|hasMember|repeatedUnit|referenceEntity*]->(rec:ReferenceEntity) " +
            "OPTIONAL MATCH (cat)-[:activity]->(gomf:GO_MolecularFunction) " +
            "WITH DISTINCT rle, " +
            "              CASE gobp WHEN NULL THEN (CASE gomf WHEN NULL THEN [] ELSE COLLECT(DISTINCT gomf.url) END) ELSE [gobp.url] END AS goTerms, " +
            "              COLLECT(DISTINCT \"https://identifiers.org/ec-code/\" + gomf.ecNumber) AS ecNumbers, " +
            "              COLLECT(lit.url) AS literatureRefs, " +
            "              COLLECT(xref.url) AS xrefs, " +
            "              COLLECT(d.url) AS diseases, " +
            "              i, pei, COLLECT(DISTINCT rei.url) AS ueis, COLLECT(DISTINCT CASE rei.variantIdentifier WHEN NULL THEN rei.identifier ELSE rei.variantIdentifier END) AS reis, " +
            "              o, peo, COLLECT(DISTINCT reo.url) AS ueos, COLLECT(DISTINCT CASE reo.variantIdentifier WHEN NULL THEN reo.identifier ELSE reo.variantIdentifier END) AS reos, " +
            "              pec, COLLECT(DISTINCT rec.url) AS uecs, COLLECT(DISTINCT CASE rec.variantIdentifier WHEN NULL THEN rec.identifier ELSE rec.variantIdentifier END) AS recs, " +
            "              pepr, COLLECT(DISTINCT repr.url) AS ueprs, COLLECT(DISTINCT CASE repr.variantIdentifier WHEN NULL THEN repr.identifier ELSE repr.variantIdentifier END) AS reprs, " +
            "              penr, COLLECT(DISTINCT renr.url) AS uenrs, COLLECT(DISTINCT CASE renr.variantIdentifier WHEN NULL THEN renr.identifier ELSE renr.variantIdentifier END) AS renrs " +
            "RETURN rle, goTerms, ecNumbers, literatureRefs, xrefs, diseases, " +
            "       COLLECT(DISTINCT CASE pei WHEN NULL THEN NULL ELSE " +
            "               { n:    i.stoichiometry, " +
            "                 pe:   pei, " +
            "                 urls: ueis, " +
            "                 accs: reis " +
            "               } END) AS inputs, " +
            "       COLLECT(DISTINCT CASE peo WHEN NULL THEN NULL ELSE " +
            "               { n:    o.stoichiometry, " +
            "                 pe:   peo, " +
            "                 urls: ueos, " +
            "                 accs: reos " +
            "               } END) AS outputs, " +
            "       COLLECT(DISTINCT CASE pec WHEN NULL THEN NULL ELSE " +
            "               { n:    0, " +
            "                 pe:   pec, " +
            "                 urls: uecs, " +
            "                 accs: recs " +
            "               } END) AS catalysts, " +
            "       COLLECT(DISTINCT CASE pepr WHEN NULL THEN NULL ELSE " +
            "               { n:    0, " +
            "                 pe:   pepr, " +
            "                 urls: ueprs, " +
            "                 accs: reprs " +
            "               } END) AS positiveRegulators," +
            "       COLLECT(DISTINCT CASE penr WHEN NULL THEN NULL ELSE " +
            "               { n:    0, " +
            "                 pe:   penr, " +
            "                 urls: uenrs, " +
            "                 accs: renrs " +
            "               } END) AS negativeRegulators";


    public static Collection<ReactionBase> getReactionList(String eventStId) {
        AdvancedDatabaseObjectService ads = ReactomeGraphCore.getService(AdvancedDatabaseObjectService.class);
        try {
            return ads.getCustomQueryResults(ReactionBase.class, QUERY, Collections.singletonMap("stId", eventStId));
        } catch (CustomQueryException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}
