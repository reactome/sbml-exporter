package org.reactome.server.tools.sbml.fetcher.model;

import org.reactome.server.graph.domain.model.Compartment;
import org.reactome.server.graph.domain.model.ReactionLikeEvent;

import java.util.List;

public class ReactionBase {

    private ReactionLikeEvent rle;

    private List<String> goTerms;
    private List<String> ecNumbers;
    private List<String> literatureRefs;
    private List<String> xrefs;
    private List<String> diseases;

    private List<Participant> inputs;
    private List<Participant> outputs;
    private List<Participant> catalysts;
    private List<Participant> positiveRegulators;
    private List<Participant> negativeRegulators;

    public Long getDbId() {
        return rle.getDbId();
    }

    public String getStId() {
        return rle.getStId();
    }

    public String getDisplayName() {
        return rle.getDisplayName();
    }

    public String getSchemaClass() {
        return rle.getSchemaClass();
    }

    public ReactionLikeEvent getReactionLikeEvent() {
        return rle;
    }

    public List<Compartment> getCompartments(){
        return rle.getCompartment();
    }

    public List<String> getGoTerms() {
        return goTerms;
    }

    public List<String> getEcNumbers() {
        return ecNumbers;
    }

    public List<String> getLiteratureRefs() {
        return literatureRefs;
    }

    public List<String> getCrossReferences() {
        return xrefs;
    }

    public List<String> getDiseases() {
        return diseases;
    }

    public List<Participant> getInputs() {
        return inputs;
    }

    public List<Participant> getOutpus() {
        return outputs;
    }

    public List<Participant> getCatalysts() {
        return catalysts;
    }

    public List<Participant> getPositiveRegulators() {
        return positiveRegulators;
    }

    public List<Participant> getNegativeRegulators() {
        return negativeRegulators;
    }
}
