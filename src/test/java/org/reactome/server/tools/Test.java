package org.reactome.server.tools;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */
@RunWith(value=Suite.class)
@Suite.SuiteClasses(value={WriteSBMLTest.class, WriteSBMLNoPathwayTest.class})
public class Test {

}
