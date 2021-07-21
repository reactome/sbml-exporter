package org.reactome.server.tools.sbml.data.model;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import org.reactome.server.graph.domain.model.CandidateSet;
import org.reactome.server.graph.domain.model.Complex;
import org.reactome.server.graph.domain.model.DefinedSet;
import org.reactome.server.graph.domain.model.Drug;
import org.reactome.server.graph.domain.model.EntityWithAccessionedSequence;
import org.reactome.server.graph.domain.model.PhysicalEntity;
import org.reactome.server.graph.domain.model.SimpleEntity;
import org.reactome.server.tools.sbml.converter.SbmlConverter;

@SuppressWarnings({"unused", "MismatchedQueryAndUpdateOfCollection"})
public class ParticipantDetails {

    private PhysicalEntity pe;
    private List<String> urls;
    private List<IdentifierBase> ids;

    public PhysicalEntity getPhysicalEntity() {
        return pe;
    }
    
    public void setPhysicalEntity(PhysicalEntity pe) {
        this.pe = pe;
    }

    public void addUrl(String url) {
        if (urls == null)
            urls = new ArrayList<>();
        urls.add(url);
    }

    public void addIdentifierBase(IdentifierBase base) {
        if (ids == null)
            ids = new ArrayList<>();
        ids.add(base);
    }

    public List<String> getUrls() {
        return urls;
    }

    private String getAccessions() {
        if (ids == null) return "";
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
        if (pe instanceof Drug)
            return prefix + "A drug";
        if (pe instanceof Complex)
            return prefix + "Here is Reactomes nested structure for this complex: " + getAccessions() + ". " +
                    "Reactome uses a nested structure for complexes, which cannot be fully represented " +
                    "in SBML Level " + SbmlConverter.SBML_LEVEL + " Version " + SbmlConverter.SBML_VERSION + " core";

        return prefix + pe.getExplanation();
    }
}
