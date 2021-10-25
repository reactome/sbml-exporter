package org.reactome.server.tools.sbml.data;

import org.reactome.server.graph.service.DatabaseObjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DataFactoryHelper extends DataFactory{

    @Autowired
    public DataFactoryHelper(DatabaseObjectService ds) {
        super(ds);
    }
}
