package org.reactome.server.tools.sbml.fetcher.model;

import org.reactome.server.graph.domain.model.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Holds the denormalised data for a given participant (aka PhysicalEntity)
 *
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 */
@SuppressWarnings("unused")
public class Participant {

    private Integer n;  //stoichiometry
    private PhysicalEntity pe;
    private List<String> urls;
    private List<String> ids;

    public Integer getStoichiometry() {
        return n;
    }

    public PhysicalEntity getPhysicalEntity() {
        return pe;
    }

    public void addUrl(String url) {
        urls.add(url);
    }

    public List<String> getUrls() {
        return urls;
    }

    private String getAccessions(){
        return ids.stream().collect(Collectors.joining(", ", "(", ")"));
    }

    public String getExplanation() {
        if (pe instanceof SimpleEntity) return "This is a small compound";
        if (pe instanceof EntityWithAccessionedSequence) return "This is a protein";
        if (pe instanceof CandidateSet) return "A list of entities, one or more of which might perform the given function";
        if (pe instanceof DefinedSet)  return "This is a list of alternative entities, any of which can perform the given function";
        if (pe instanceof OpenSet) return "A set of examples characterizing a very large but not explicitly enumerated set, e.g. mRNAs";
        if (pe instanceof Drug) return "A drug";
        if (pe instanceof Complex) return "Here is Reactomes nested structure for this complex: " + getAccessions();
        return pe.getExplanation();
    }
}
