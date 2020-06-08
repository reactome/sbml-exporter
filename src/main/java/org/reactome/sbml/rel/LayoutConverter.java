package org.reactome.sbml.rel;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.gk.graphEditor.PathwayEditor;
import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.PersistenceAdaptor;
import org.gk.model.ReactomeJavaConstants;
import org.gk.pathwaylayout.PathwayDiagramGeneratorViaAT;
import org.gk.persistence.DiagramGKBReader;
import org.gk.persistence.MySQLAdaptor;
import org.gk.render.Node;
import org.gk.render.Renderable;
import org.gk.render.RenderableChemical;
import org.gk.render.RenderableChemicalDrug;
import org.gk.render.RenderableCompartment;
import org.gk.render.RenderableComplex;
import org.gk.render.RenderableEntitySet;
import org.gk.render.RenderableGene;
import org.gk.render.RenderablePathway;
import org.gk.render.RenderableProtein;
import org.gk.render.RenderableProteinDrug;
import org.gk.render.RenderableRNA;
import org.gk.render.RenderableRNADrug;
import org.gk.render.RenderableReaction;
import org.reactome.server.tools.sbml.converter.SbmlConverter;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.ext.layout.BoundingBox;
import org.sbml.jsbml.ext.layout.CompartmentGlyph;
import org.sbml.jsbml.ext.layout.Curve;
import org.sbml.jsbml.ext.layout.Dimensions;
import org.sbml.jsbml.ext.layout.Layout;
import org.sbml.jsbml.ext.layout.LayoutConstants;
import org.sbml.jsbml.ext.layout.LayoutModelPlugin;
import org.sbml.jsbml.ext.layout.LineSegment;
import org.sbml.jsbml.ext.layout.Point;
import org.sbml.jsbml.ext.layout.ReactionGlyph;
import org.sbml.jsbml.ext.layout.SpeciesGlyph;
import org.sbml.jsbml.ext.layout.SpeciesReferenceGlyph;
import org.sbml.jsbml.ext.layout.SpeciesReferenceRole;
import org.sbml.jsbml.ext.layout.TextGlyph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to convert Reactome pathway diagram layout information to the SBML layout extension.
 * @author wug
 *
 */
@SuppressWarnings("unchecked")
public class LayoutConverter {
    public static final String LAYOUT_ID_PREFIX = "layout_";
    public static final String TEXT_PREFIX = "text_";
    private static final Logger logger = LoggerFactory.getLogger(LayoutConverter.class);
    private static final double SHIFT_VALUE = 0.5d;
    private MySQLAdaptor dba;

    public LayoutConverter() {
    }
    
    public void setDBA(MySQLAdaptor dba) {
        this.dba = dba;
    }

    public void addLayout(Model model,
                          GKInstance pathway,
                          RenderablePathway diagram) {
        if (diagram == null) {
            logger.error("No diagram is provided for adding layout for model: " + 
                          model.getName() + " converted from " + pathway + ".");
            return;
        }
        try {
            logger.info("Adding layout for " + pathway + "...");
            validateLayout(diagram);
            // The following code is modified from https://github.com/sbmlteam/jsbml/blob/master/extensions/layout/test/org/sbml/jsbml/ext/layout/test/CurveSegmentTest.java
            LayoutModelPlugin lModel = new LayoutModelPlugin(model);
            model.addExtension(LayoutConstants.namespaceURI,
                               lModel);
            Layout layout = lModel.createLayout();
            handleCompartments(layout, diagram);
            handleNodes(layout, diagram);
            handleReactions(layout, diagram);
            logger.info("Done layout.");
        }
        catch(Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
    
    private void validateLayout(RenderablePathway diagram) {
        // Force to make sure all points are correct validated
        PathwayEditor pathwayEditor = new PathwayEditor();
        pathwayEditor.setRenderable(diagram);
        PathwayDiagramGeneratorViaAT helper = new PathwayDiagramGeneratorViaAT();
        helper.paintOnImage(pathwayEditor);
        // Do a tight node for avoiding text overflow
        // Entities in predicted diagrams have weird "name copied from...",
        // which may bloat the sizes of nodes there.
        pathwayEditor.tightNodes(true);
        helper.paintOnImage(pathwayEditor);
    }
    
    private void handleReactions(Layout layout, RenderablePathway diagram) {
        List<Renderable> comps = diagram.getComponents();
        for (Renderable comp : comps) {
            if ((comp instanceof RenderableReaction) && (comp.getReactomeId() != null)) {
                RenderableReaction rxt = (RenderableReaction) comp;
                handleReaction(layout, rxt);
            }
        }
    }
    
    /**
     * Make sure we have input branch and output branch for one input or one output case
     * to make the SBML layout happy.
     * @param rxt
     */
    private void manipulateReactionPoints(RenderableReaction rxt) {
        List<java.awt.Point> backbonePoints = rxt.getBackbonePoints();
        if (backbonePoints.size() == 2) {
            // Automatically extend it to three points
            java.awt.Point first = backbonePoints.get(0);
            java.awt.Point second = backbonePoints.get(1);
            java.awt.Point middle = new java.awt.Point((first.x + second.x) / 2,
                                                       (first.y + second.y) / 2);
            backbonePoints.add(1, middle);
            rxt.validatePosition();
            rxt.validateConnectInfo();
        }
        List<Node> inputs = rxt.getInputNodes();
        if (inputs != null && inputs.size() == 1) {
            // Create an input branch by split the first two points
            java.awt.Point first = backbonePoints.get(0);
            java.awt.Point second = backbonePoints.get(1);
            java.awt.Point middle = new java.awt.Point((first.x + second.x) / 2,
                                                       (first.y + second.y) / 2);
            backbonePoints.set(0, middle);
            List<List<java.awt.Point>> inputBranches = new ArrayList<>();
            List<java.awt.Point> inputBranch = new ArrayList<>();
            inputBranch.add(first);
            inputBranches.add(inputBranch);
            rxt.setInputPoints(inputBranches);
        }
        List<Node> outputs = rxt.getOutputNodes();
        if (outputs != null && outputs.size() == 1) {
            java.awt.Point last = backbonePoints.get(backbonePoints.size() - 1);
            java.awt.Point penultimate = backbonePoints.get(backbonePoints.size() - 2);
            java.awt.Point middle = new java.awt.Point((last.x + penultimate.x) / 2,
                                                        (last.y + penultimate.y) / 2);
            backbonePoints.set(backbonePoints.size() - 1, middle);
            List<List<java.awt.Point>> outputBranches = new ArrayList<>();
            List<java.awt.Point> outputBranch = new ArrayList<>();
            outputBranches.add(outputBranch);
            outputBranch.add(last);
            rxt.setOutputPoints(outputBranches);
        }
    }
    
    /**
     * The implementation of this method is based on the logic in class org.gk.render.DefaultReactionRender.
     * @param layout
     * @param rxt
     */
    private void handleReaction(Layout layout, RenderableReaction rxt) {
        String rxtId = SbmlConverter.REACTION_PREFIX + rxt.getReactomeId();
        if (!layout.getModel().containsReaction(rxtId))
            return;
        manipulateReactionPoints(rxt);
        List<java.awt.Point> backbonePoints = rxt.getBackbonePoints();
        // Basic requirement for converting
        if (backbonePoints.size() < 3) {
            logger.error("The backbone of reaction with DB_ID " + rxt.getReactomeId() + " has less than 3 points.");
            return;
        }
        java.awt.Point inputHub = backbonePoints.get(0);
        java.awt.Point outputHub = backbonePoints.get(backbonePoints.size() - 1);
        java.awt.Point reactionHub = rxt.getPosition();
        String layoutId = LAYOUT_ID_PREFIX + rxt.getID();
        ReactionGlyph rg = layout.createReactionGlyph(layoutId);
        rg.setReaction(rxtId);
        // Backbone
        Curve curve = rg.createCurve();
        convertToCurve(backbonePoints, curve);
        // Input
        List<Node> inputs = rxt.getInputNodes();
        List<List<java.awt.Point>> inputPoints = rxt.getInputPoints();
        handleBranches(inputs,
                       inputPoints,
                       inputHub,
                       SpeciesReferenceRole.SUBSTRATE,
                       rg,
                       layoutId);
        // output
        List<Node> outputs = rxt.getOutputNodes();
        List<List<java.awt.Point>> outputPoints = rxt.getOutputPoints();
        handleBranches(outputs,
                       outputPoints,
                       outputHub,
                       SpeciesReferenceRole.PRODUCT,
                       rg,
                       layoutId);
        // Catalysts
        List<Node> catalysts = rxt.getHelperNodes();
        List<List<java.awt.Point>> catalystPoints = rxt.getHelperPoints();
        handleBranches(catalysts,
                       catalystPoints,
                       reactionHub,
                       SpeciesReferenceRole.MODIFIER, // This may not be right
                       rg,
                       layoutId);
        // Activators
        List<Node> activators = rxt.getActivatorNodes();
        List<List<java.awt.Point>> activatorPoints = rxt.getActivatorPoints();
        handleBranches(activators, 
                       activatorPoints, 
                       reactionHub, 
                       SpeciesReferenceRole.ACTIVATOR,
                       rg,
                       layoutId);
        // Inhibitors
        List<Node> inhibitors = rxt.getInhibitorNodes();
        List<List<java.awt.Point>> inhibitorPoints = rxt.getInhibitorPoints();
        handleBranches(inhibitors, 
                       inhibitorPoints, 
                       reactionHub, 
                       SpeciesReferenceRole.INHIBITOR,
                       rg,
                       layoutId);
    }

    private void handleBranches(List<Node> nodes,
                                List<List<java.awt.Point>> branches,
                                java.awt.Point hub,
                                SpeciesReferenceRole role,
                                ReactionGlyph rg,
                                String layoutId) {
        if (nodes == null || nodes.size() == 0)
            return;
        // There are two cases: if there is only one input, no inputPoints is expected
        // Otherwise, the same number of inputs and inputPoints is expected.
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            String speciesRefGraphId = layoutId + "_" + role.toString().toLowerCase()+ "_" + i;
            SpeciesReferenceGlyph speciesRefGraph = createSpeciesRefGraph(rg, node, speciesRefGraphId);
            // This is weird with Minerva. It seems that the parser there cannot take these two roles
            // and have to convert them into MODIFIER. However, the actual role is still correct.
//            speciesRefGraph.setRole(role == SpeciesReferenceRole.INHIBITOR || role == SpeciesReferenceRole.ACTIVATOR ?
//                                    SpeciesReferenceRole.MODIFIER :
//                                    role);
            List<? extends Point2D> branch = null;
            if (branches != null && branches.size() > 0) {
                branch = branches.get(i);
            }
            else {
                // We cannot have an empty curve. Make sure a little bit of shift from the input hub
                // by adding some padding (e.g. 0.001d since the value can be double
                Point2D shifted = shiftPoint(hub);
                branch = Collections.singletonList(shifted);
            }
            // Make sure this is reversed
            // Need a unique id
            List<Point2D> copy = new ArrayList<>(branch);
            copy.add(hub);
            if (role == SpeciesReferenceRole.SUBSTRATE || role == SpeciesReferenceRole.PRODUCT)
                Collections.reverse(copy);
            Curve curve = speciesRefGraph.createCurve();
            convertToCurve(copy, curve);
        }
    }
    
    private Point2D shiftPoint(java.awt.Point point) {
        Point2D shifted = new Point2D.Double();
        shifted.setLocation(point.x + SHIFT_VALUE,
                            point.y + SHIFT_VALUE);
        return shifted;
    }

    private SpeciesReferenceGlyph createSpeciesRefGraph(ReactionGlyph rg, Node input, String speciesRefGraphId) {
        SpeciesReferenceGlyph speciesRefGraph = rg.createSpeciesReferenceGlyph(speciesRefGraphId);
        speciesRefGraph.setSpeciesGlyph(LAYOUT_ID_PREFIX + input.getID());
        return speciesRefGraph;
    }
    
    private void convertToCurve(List<? extends Point2D> points, Curve curve) {
        for (int i = 0; i < points.size() - 1; i++) {
            Point2D startPoint = points.get(i);
            Point2D endPoint = points.get(i + 1);
            LineSegment lineSegment = curve.createLineSegment(convertAWTPoint(startPoint),
                                                              convertAWTPoint(endPoint));
//            lineSegment.setMetaId("line_" + lineSegmentId ++);
        }
    }
    
    private Point convertAWTPoint(Point2D point) {
        Point rtn = new Point();
        rtn.setX(point.getX());
        rtn.setY(point.getY());
//        rtn.setId("point_" + pointObjCount ++);
        return rtn;
    }
    
    private void handleNodes(Layout layout, RenderablePathway diagram) {
        List<Renderable> comps = diagram.getComponents();
        for (Renderable comp : comps) {
            // This is a species
            if ((comp instanceof Node) && (!(comp instanceof RenderableCompartment))) {
                Node node = (Node) comp;
                if (node.getReactomeId() == null)
                    continue;
                String speciesId = SbmlConverter.SPECIES_PREFIX + node.getReactomeId();
                // Some nodes may not be in the SBML model (e.g. a pathway)
                if (!layout.getModel().containsSpecies(speciesId))
                    continue;
                // A species may be drawn multiple times (e.g. ATP). Therefore,
                // we cannot use speciesId since layout should not be duplicated.
                String layoutId = LAYOUT_ID_PREFIX + node.getID();
                SpeciesGlyph sg = layout.createSpeciesGlyph(layoutId);
                sg.setSpecies(speciesId);
                sg.setBoundingBox(createBox(node.getBounds()));
                TextGlyph textGlyph = handleText(layoutId, layout, node, speciesId);
                // It seems that the text cannot be honored at Minerva. Reset the _displayName
                Species species = layout.getModel().getSpecies(speciesId);
                species.setName(textGlyph.getText());
                // Need to reset SBO term for minerva
                String sboTerm = getSBOTerm(comp);
                species.setSBOTerm(sboTerm);
            }
        }
    }
    
    private TextGlyph handleText(String layoutId,
                            Layout layout,
                            Node node,
                            String speciesId) {
        // Text label
        String textId = TEXT_PREFIX + layoutId;
        TextGlyph textGlyph = layout.createTextGlyph(textId);
        textGlyph.setBoundingBox(createBox(node.getTextBounds()));
        // Remove compartment information from species
        String name = node.getDisplayName();
        textGlyph.setText(name);
        textGlyph.setGraphicalObject(layoutId);
        return textGlyph;
    }
    
    private void handleCompartments(Layout layout, RenderablePathway diagram) {
        List<Renderable> comps = diagram.getComponents();
        Set<String> handled = new HashSet<>();
        for (Renderable comp : comps) {
            if (comp instanceof RenderableCompartment) {
                String compartmentId = SbmlConverter.COMPARTMENT_PREFIX + comp.getReactomeId();
                if (!layout.getModel().containsCompartment(compartmentId))
                    continue;
                String id = LAYOUT_ID_PREFIX + comp.getID();
                CompartmentGlyph cg = layout.createCompartmentGlyph(id);
                cg.setCompartment(compartmentId);
                // Get the bounding box
                Rectangle bounding = comp.getBounds();
                cg.setBoundingBox(createBox(bounding));
                handleText(id, layout, (Node)comp, compartmentId);
                handled.add(compartmentId);
            }
        }
        // This is a hack to avoid drawing compartments automatically in Minerva
        ListOf<Compartment> compartments = layout.getModel().getListOfCompartments();
        if (handled.size() == compartments.size())
            return;
        int count = 0;
        Rectangle diagramBounds = getDiagramBounds(comps);
        // Put them at the bottom
        Rectangle location = new Rectangle();
        location.x = (int)diagramBounds.getCenterX();
        location.y = (int) diagramBounds.getMaxY();
        location.width = 1;
        location.height = 1;
        BoundingBox box = createBox(location);
        for (int i = 0; i < compartments.size(); i++) {
            Compartment compartment = compartments.get(i);
            if (handled.contains(compartment.getId()))
                continue;
            CompartmentGlyph cg = layout.createCompartmentGlyph(LAYOUT_ID_PREFIX + "hidden_compartment_" + count++);
            cg.setCompartment(compartment);
            cg.setBoundingBox(box.clone());
        }
    }
    
    private Rectangle getDiagramBounds(List<Renderable> comps) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (Renderable comp : comps) {
            if (!(comp instanceof Node))
                continue;
            Rectangle rect = comp.getBounds();
            if (minX > rect.x) minX = rect.x;
            if (minY > rect.y) minY = rect.y;
            if (maxX < rect.getMaxX()) maxX = (int) rect.getMaxX();
            if (maxY < rect.getMaxY()) maxY = (int) rect.getMaxY();
        }
        return new Rectangle(minX, minY, maxX - minX, maxY - minY);
    }
    
    private BoundingBox createBox(Rectangle rect) {
        BoundingBox box = new BoundingBox();
        Point point = new Point();
        point.setX(rect.x);
        point.setY(rect.y);
        box.setPosition(point);
        Dimensions dimensions = new Dimensions();
        dimensions.setWidth(rect.getWidth());
        dimensions.setHeight(rect.getHeight());
        box.setDimensions(dimensions);
        return box;
    }

    public RenderablePathway getDiagram(GKInstance pathway) throws Exception {
        PersistenceAdaptor dba = pathway.getDbAdaptor();
        Collection<GKInstance> diagrams = dba.fetchInstanceByAttribute(ReactomeJavaConstants.PathwayDiagram,
                                                                       ReactomeJavaConstants.representedPathway,
                                                                       "=",
                                                                       pathway);
        if (diagrams == null || diagrams.size() == 0)
            return null;
        GKInstance diagram = diagrams.iterator().next();
        return new DiagramGKBReader().openDiagram(diagram);
    }
    
    /**
     * The following term is based on minera class lcsb/mapviewer/converter/model/sbml/species/SBOTermSpeciesType.java
     *   ANTISENSE_RNA(AntisenseRna.class, new String[] { "SBO:0000334" }),
     *   COMPLEX(Complex.class, new String[] { "SBO:0000297" }),
     *   DEGRADED(Degraded.class, new String[] { "SBO:0000291" }),
     *   DRUG(Drug.class, new String[] { "SBO:0000298" }),
     *   GENE(Gene.class, new String[] { "SBO:0000243" }),
     *   GENERIC_PROTEIN(GenericProtein.class, new String[] { "SBO:0000252" }),
     *   TRUNCATED_PROTEIN(TruncatedProtein.class, new String[] { "SBO:0000421" }),
     *   ION(Ion.class, new String[] { "SBO:0000327" }),
     *   ION_CHANNEL(IonChannelProtein.class, new String[] { "SBO:0000284" }),
     *   PHENOTYPE(Phenotype.class, new String[] { "SBO:0000358" }),
     *   RECEPTOR(ReceptorProtein.class, new String[] { "SBO:0000244" }),
     *   RNA(Rna.class, new String[] { "SBO:0000278" }),
     *   SIMPLE_MOLECULE(SimpleMolecule.class, new String[] { "SBO:0000247", "SBO:0000299" }),
     *   UNKNOWN(Unknown.class, new String[] { "SBO:0000285" }),
     * @param r
     * @return
     */
    private String getSBOTerm(Renderable r) {
        if (r instanceof RenderableProtein)
            return "SBO:0000252";
        if (r instanceof RenderableComplex)
            return "SBO:0000297";
        if (r instanceof RenderableChemical)
            return "SBO:0000247";
        if (r instanceof RenderableRNA)
            return "SBO:0000278";
        if (r instanceof RenderableGene)
            return "SBO:0000243";
        if (r instanceof RenderableChemicalDrug ||
            r instanceof RenderableRNADrug || 
            r instanceof RenderableProteinDrug)
            return "SBO:0000298";
        // Special case
        if (r instanceof RenderableEntitySet) {
            try {
                GKInstance inst = dba.fetchInstance(r.getReactomeId());
                if (InstanceUtilities.hasDrug(inst))
                    return "SBO:0000298";
            }
            catch(Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
        return "SBO:0000285";
    }
    
}
