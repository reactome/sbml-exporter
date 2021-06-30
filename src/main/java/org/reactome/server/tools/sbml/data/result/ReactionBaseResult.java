package org.reactome.server.tools.sbml.data.result;


import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.reactome.server.graph.domain.result.CustomQuery;

import java.util.List;

public class ReactionBaseResult implements CustomQuery {

    private String rle;
    private List<String> goTerms;
    private List<String> ecNumbers;
    private List<String> literatureRefs;
    private List<String> xrefs;
    private List<String> diseases;

    private List<ParticipantResult> inputs;
    private List<ParticipantResult> outputs;
    private List<ParticipantResult> catalysts;
    private List<ParticipantResult> positiveRegulators;
    private List<ParticipantResult> negativeRegulators;

    public String getRle() {
        return rle;
    }

    public void setRle(String rle) {
        this.rle = rle;
    }

    public List<String> getGoTerms() {
        return goTerms;
    }

    public void setGoTerms(List<String> goTerms) {
        this.goTerms = goTerms;
    }

    public List<String> getEcNumbers() {
        return ecNumbers;
    }

    public void setEcNumbers(List<String> ecNumbers) {
        this.ecNumbers = ecNumbers;
    }

    public List<String> getLiteratureRefs() {
        return literatureRefs;
    }

    public void setLiteratureRefs(List<String> literatureRefs) {
        this.literatureRefs = literatureRefs;
    }

    public List<String> getXrefs() {
        return xrefs;
    }

    public void setXrefs(List<String> xrefs) {
        this.xrefs = xrefs;
    }

    public List<String> getDiseases() {
        return diseases;
    }

    public void setDiseases(List<String> diseases) {
        this.diseases = diseases;
    }

    public List<ParticipantResult> getInputs() {
        return inputs;
    }

    public void setInputs(List<ParticipantResult> inputs) {
        this.inputs = inputs;
    }

    public List<ParticipantResult> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<ParticipantResult> outputs) {
        this.outputs = outputs;
    }

    public List<ParticipantResult> getCatalysts() {
        return catalysts;
    }

    public void setCatalysts(List<ParticipantResult> catalysts) {
        this.catalysts = catalysts;
    }

    public List<ParticipantResult> getPositiveRegulators() {
        return positiveRegulators;
    }

    public void setPositiveRegulators(List<ParticipantResult> positiveRegulators) {
        this.positiveRegulators = positiveRegulators;
    }

    public List<ParticipantResult> getNegativeRegulators() {
        return negativeRegulators;
    }

    public void setNegativeRegulators(List<ParticipantResult> negativeRegulators) {
        this.negativeRegulators = negativeRegulators;
    }

    @Override
    public CustomQuery build(Record r) {
        ReactionBaseResult reactionBaseResult = new ReactionBaseResult();
        if (!r.get("rle").isNull()) reactionBaseResult.setRle((r.get("rle").asString()));
        if (!r.get("goTerms").isNull()) reactionBaseResult.setGoTerms(r.get("goTerms").asList(Value::asString));
        if (!r.get("ecNumbers").isNull()) reactionBaseResult.setEcNumbers(r.get("ecNumbers").asList(Value::asString));
        if (!r.get("literatureRefs").isNull()) reactionBaseResult.setLiteratureRefs(r.get("literatureRefs").asList(Value::asString));
        if (!r.get("xrefs").isNull()) reactionBaseResult.setXrefs(r.get("xrefs").asList(Value::asString));
        if (!r.get("diseases").isNull()) reactionBaseResult.setDiseases(r.get("diseases").asList(Value::asString));
        if (!r.get("inputs").isNull()) reactionBaseResult.setInputs(r.get("inputs").asList(ParticipantResult::build));
        if (!r.get("outputs").isNull()) reactionBaseResult.setOutputs(r.get("outputs").asList(ParticipantResult::build));
        if (!r.get("positiveRegulators").isNull()) reactionBaseResult.setPositiveRegulators(r.get("positiveRegulators").asList(ParticipantResult::build));
        if (!r.get("negativeRegulators").isNull()) reactionBaseResult.setNegativeRegulators(r.get("negativeRegulators").asList(ParticipantResult::build));
        return reactionBaseResult;
    }
}
