package org.reactome.sbml.rel;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.render.Renderable;
import org.gk.render.RenderablePathway;
import org.gk.render.RenderableReaction;
import org.gk.util.FileUtilities;
import org.reactome.server.graph.aop.LazyFetchAspect;
import org.reactome.server.graph.domain.model.DatabaseObject;
import org.reactome.server.graph.domain.model.Pathway;
import org.reactome.server.graph.domain.model.PhysicalEntity;
import org.reactome.server.graph.domain.model.ReactionLikeEvent;
import org.reactome.server.tools.sbml.converter.Helper;
import org.reactome.server.tools.sbml.converter.SbmlConverter;
import org.reactome.server.tools.sbml.data.model.ParticipantDetails;
import org.reactome.server.tools.sbml.data.model.ReactionBase;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.SBMLWriter;
import org.sbml.jsbml.TidySBMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * A customized SBMLConverter to handle objects directly loaded from a RelationDatabase.
 * @author wug
 *
 */
@SuppressWarnings("unchecked")
public class SbmlConverterForRel extends SbmlConverter {
    private static final Logger logger = LoggerFactory.getLogger(SbmlConverterForRel.class);
    private MySQLAdaptor dba;
    private InstanceToModelConverter instanceConverter;
    private LayoutConverter layoutConverter;
    private GKInstance topEvent;
    // Cache the diagram is useDiagram is true and the diagram is in the database for repearting query
    private RenderablePathway pathwayDiagram;

    public SbmlConverterForRel(String targetId) {
        this(targetId, 0); // Default version is 0, meaning it is not defined.
    }

    /**
     * Both stable id and DB_ID are supported in this subclass.
     * @param targetId
     * @param version
     */
    public SbmlConverterForRel(String targetId, Integer version) {
        super(targetId, version);
        setUpSpring();
        instanceConverter = new InstanceToModelConverter();
        layoutConverter = new LayoutConverter();
        Helper.setUseIdentifierURL(true);
    }
    
    private void setUpSpring() {
        ApplicationContext context = new AnnotationConfigApplicationContext(DumbGraphNeo4jConfig.class);
        // Disable it. This has to be called.
        context.getBean(LazyFetchAspect.class).setEnableAOP(false);
    }

    public void setDBA(MySQLAdaptor dba) {
        this.dba = dba;
        layoutConverter.setDBA(this.dba);
        // Need to have a fake pathway for the superclass
        try {
            GKInstance instance = fetchEvent(targetStId);
            // This may be a reaction
            DatabaseObject databaseObject = instanceConverter.convert(instance);
            if (databaseObject instanceof Pathway) {
                pathway = (Pathway) databaseObject;
                instanceConverter.fillInPathwayDetails(instance, pathway);
            }
            topEvent = instance;
            pathwayDiagram = null; // Just in case
        }
        catch(Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private GKInstance fetchEvent(String eventId) throws Exception {
        GKInstance instance = null;
        if (targetStId.startsWith("R-")) // This is a stable id
            instance = fetchEventForStableId(dba, targetStId);
        else
            instance = dba.fetchInstance(new Long(targetStId));
        if (instance == null)
            throw new IllegalArgumentException("Cannot find an Event with id " + targetStId + " in the provided database.");
        return instance;
    }

    private GKInstance fetchEventForStableId(MySQLAdaptor dba, String stableId) throws Exception {
        Collection<GKInstance> stableIdInst = dba.fetchInstanceByAttribute(ReactomeJavaConstants.StableIdentifier,
                                                                           ReactomeJavaConstants.identifier,
                                                                           "=",
                                                                           stableId);
        if (stableIdInst == null || stableIdInst.size() == 0)
            return null;
        Collection<GKInstance> events = dba.fetchInstanceByAttribute(ReactomeJavaConstants.Event,
                                                                     ReactomeJavaConstants.stableIdentifier,
                                                                     "=",
                                                                     stableIdInst.iterator().next());
        if (events == null || events.size() == 0)
            return null;
        return events.iterator().next();
    }

    @Override
    public SBMLDocument convert() {
        // Have to make sure this is a dba available
        if (dba == null)
            throw new IllegalStateException("No MySQLAdaptor specified.");
        if (targetStId == null)
            throw new IllegalStateException("No target id specified.");
        instanceConverter.reset();
        logger.info("Starting converting " + targetStId + "...");
        SBMLDocument doc =  super.convert();
        logger.info("Finished converting " + targetStId + ".");
        if (pathwayDiagram != null) { // This should do for using pathway diagram only
            layoutConverter.addLayout(doc.getModel(), 
                                      topEvent,
                                      pathwayDiagram);
        }
        return doc;
    }

    @Override
    protected Collection<ParticipantDetails> getParticipantDetails() {
        List<ParticipantDetails> rtn = new ArrayList<>();
        try {
            Set<GKInstance> reactions = getReactions();
            Set<GKInstance> pes = new HashSet<>();
            for (GKInstance rxt : reactions)
                pes.addAll(InstanceUtilities.getReactionParticipants(rxt));
            for (GKInstance pe : pes) {
                // Need the attributes for PhysicalEntity
                DatabaseObject databaseObj = instanceConverter.convert(pe);
                if (!(databaseObj instanceof PhysicalEntity)) {
                    throw new IllegalStateException(databaseObj + " cannot be converted into a PhysicalEntity.");
                }
                ParticipantDetails details = new ParticipantDetails();
                PhysicalEntity peObj = (PhysicalEntity) databaseObj;
                details.setPhysicalEntity(peObj);
                instanceConverter.fillInPEDetails(pe, details);
                rtn.add(details);
            }
        }
        catch(Exception e) {
            logger.error(e.getMessage(), e);
        }
        return rtn;
    }
    
    private Set<GKInstance> getReactions() throws Exception {
        RenderablePathway diagram = layoutConverter.getDiagram(topEvent);
        if (diagram != null && layoutConverter.hasReactions(diagram)) {
            this.pathwayDiagram = diagram;
            // We will convert all contained reactions regardless if they are laid out in the diagram.
//            return getReactionsInDiagram(diagram);
        }
        Set<GKInstance> contained = InstanceUtilities.getContainedEvents(topEvent);
        contained.add(topEvent); // In case event itself is a RLE
        return contained.stream()
                .filter(e -> e.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent))
                .collect(Collectors.toSet());
    }
    
    private Set<GKInstance> getReactionsInDiagram(RenderablePathway pathwayDiagram) throws Exception {
        List<Renderable> comps = pathwayDiagram.getComponents();
        Set<GKInstance> rtn = new HashSet<>();
//        List<Long> testIds = Arrays.asList(9678128L);
        for (Renderable comp : comps) {
            if ((comp instanceof RenderableReaction) && (comp.getReactomeId() != null)) {
                // For test
//                if (!testIds.contains(comp.getReactomeId()))
//                    continue;
                GKInstance rxt = dba.fetchInstance(comp.getReactomeId());
                if (rxt == null) {
                    logger.error("Reaction drawn with DB_ID = " + comp.getReactomeId() + " is not in the database.");
                    continue;
                }
                rtn.add(rxt);
            }
        }
        return rtn;
    }

    @Override
    protected Collection<ReactionBase> getReactionList() {
        List<ReactionBase> rtn = new ArrayList<>();
        try {
            Set<GKInstance> reactions = getReactions();
            for (GKInstance reaction : reactions) {
                DatabaseObject dob = instanceConverter.convert(reaction);
                if (!(dob instanceof ReactionLikeEvent))
                    throw new IllegalStateException(dob + " cannot be converted into a ReactionlikeEvent.");
                ReactionLikeEvent rle = (ReactionLikeEvent) dob;
                ReactionBase reactionBase = new ReactionBase();
                reactionBase.setRle(rle);
                instanceConverter.fillInReactionDetails(reaction, reactionBase);
                rtn.add(reactionBase);
            }
        }
        catch(Exception e) {
            logger.error(e.getMessage(), e);
        }
        return rtn;
    }

    public static void main(String[] args) throws Exception {
        MySQLAdaptor dba = new MySQLAdaptor("localhost",
                                            "gk_current_ver76",
                                            "",
                                            "");
        String targetStId = "R-MMU-211119";
//        targetStId = "3927939"; // Reactions with entities having inferFrom
//        targetStId = "5268354"; // Has ecnumber
//        targetStId = "5423632"; // Disease reaction
//        targetStId = "5269406"; // CrossReferences
        
        targetStId = "R-HSA-400253"; // A full pathway: circadia clock
//        targetStId = "R-HSA-9678108"; // SARS-CoV-1 Infection
        targetStId = "R-HSA-9694516"; // SARS-CoV-2
//        targetStId = "R-HSA-187037"; // Signaling by NTRK1 (TRKA): busy pathway with process nodes
//        targetStId = "R-HSA-69620"; // Cell Cycle Checkpoints
//        targetStId = "R-HSA-73884"; // A big pathway: no SBGN diagram
        
        SbmlConverterForRel converter = new SbmlConverterForRel(targetStId);
        converter.setDBA(dba);
        SBMLDocument doc = converter.convert();
        
        String fileName = "output/" + targetStId + ".xml";
        File file = new File(fileName);
        SBMLWriter writer = new TidySBMLWriter();
        writer.write(doc, file);
        
        // Read back as a test to make sure the grammer is correct
        SBMLReader reader = new SBMLReader();
        doc = reader.readSBML(file);
        System.out.println("Read back: " + doc.getLevelAndVersion());
        
        // Try to pull out all rdf:resource lines
//        FileUtilities fu = new FileUtilities();
//        fu.setInput(fileName);
//        String line = null;
//        Set<String> lines = new HashSet<>();
//        while ((line = fu.readLine()) != null) {
//            line = line.trim();
//            if (line.contains("rdf:resource")) {
//                int index1 = line.indexOf("\"");
//                int index2 = line.lastIndexOf("\"");
//                lines.add(line.substring(index1 + 1, index2));
//            }
//        }
//        fu.close();
//        lines.stream().sorted().forEach(System.out::println);
    }

}
