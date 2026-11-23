package org.reactome.server.tools.sbml.data.result;

import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.reactome.server.graph.domain.result.CustomQuery;
import org.reactome.server.tools.sbml.data.model.IdentifierBase;

import java.util.List;

public class ParticipantDetailsResult implements CustomQuery {

    private String peStId;
    private List<String> urls;
    private List<IdentifierBase> ids;

    public String getPeStId() {
        return peStId;
    }

    public void setPeStId(String peStId) {
        this.peStId = peStId;
    }

    public List<String> getUrls() {
        return urls;
    }

    public void setUrls(List<String> urls) {
        this.urls = urls;
    }

    public List<IdentifierBase> getIds() {
        return ids;
    }

    public void setIds(List<IdentifierBase> ids) {
        this.ids = ids;
    }

    @Override
    public CustomQuery build(Record r) {
        ParticipantDetailsResult participantDetailsResult = new ParticipantDetailsResult();
        participantDetailsResult.setPeStId(r.get("pe").asString(null));
        participantDetailsResult.setUrls(r.get("urls").asList(Value::asString));
        participantDetailsResult.setIds(r.get("ids").asList(IdentifierBase::build));
        return participantDetailsResult;
    }
}
