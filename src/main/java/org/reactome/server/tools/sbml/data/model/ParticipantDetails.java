package org.reactome.server.tools.sbml.data.model;

import org.reactome.server.graph.domain.model.*;
import org.reactome.server.tools.sbml.converter.SbmlConverter;

import java.util.List;
import java.util.StringJoiner;

@SuppressWarnings({"unused", "MismatchedQueryAndUpdateOfCollection"})
public class ParticipantDetails {

    private PhysicalEntity pe;
    private List<String> urls;
    private List<IdentifierBase> ids;

    public PhysicalEntity getPhysicalEntity() {
        return pe;
    }

    public void addUrl(String url) {
        urls.add(url);
    }


    public List<String> getUrls() {
        return urls;
    }

    private String getAccessions() {
        StringJoiner joiner = new StringJoiner(", ", "(", ")");
        for (IdentifierBase id : ids) joiner.add(id.toString());
        return joiner.toString();
    }

    private static final String PREFIX = "Derived from a Reactome ";

    public String getExplanation() {
        String prefix = PREFIX + pe.getSchemaClass() + ". ";

        if (pe instanceof SimpleEntity)
            return prefix + "This is a small compound";
        if (pe instanceof EntityWithAccessionedSequence)
            return prefix + "This is a protein";
        if (pe instanceof CandidateSet)
            return prefix + "A list of entities, one or more of which might perform the given function";
        if (pe instanceof DefinedSet)
            return prefix + "This is a list of alternative entities, any of which can perform the given function";
        if (pe instanceof OpenSet)
            return prefix + "A set of examples characterizing a very large but not explicitly enumerated set, e.g. mRNAs";
        if (pe instanceof Drug)
            return prefix + "A drug";
        if (pe instanceof Complex)
            return prefix + "Here is Reactomes nested structure for this complex: " + getAccessions() + ". " +
                    "Reactome uses a nested structure for complexes, which cannot be fully represented " +
                    "in SBML Level " + SbmlConverter.SBML_LEVEL + " Version " + SbmlConverter.SBML_VERSION + " core";

        return prefix + pe.getExplanation();
    }
}
