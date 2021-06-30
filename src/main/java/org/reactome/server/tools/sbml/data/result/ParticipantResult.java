package org.reactome.server.tools.sbml.data.result;

import org.neo4j.driver.Value;

public class ParticipantResult {

    private Integer n;  //stoichiometry
    private String peStId;

    public Integer getStoichiometry() {
        return n;
    }

    public String getPhysicalEntity() {
        return peStId;
    }

    public void setStoichiometry(Integer n) {
        this.n = n;
    }

    public void setPhysicalEntity(String peStId) {
        this.peStId = peStId;
    }

    public static ParticipantResult build(Value v) {
        ParticipantResult participantResult = new ParticipantResult();
        participantResult.setPhysicalEntity(v.get("peStId").asString(null));
        participantResult.setStoichiometry(v.get("n").asInt(0));
        return participantResult;
    }
}
