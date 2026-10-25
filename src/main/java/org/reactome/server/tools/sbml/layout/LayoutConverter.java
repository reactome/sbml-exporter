package org.reactome.server.tools.sbml.layout;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.reactome.server.tools.diagram.data.layout.Compartment;
import org.reactome.server.tools.diagram.data.layout.Coordinate;
import org.reactome.server.tools.diagram.data.layout.Diagram;
import org.reactome.server.tools.diagram.data.layout.DiagramObject;
import org.reactome.server.tools.diagram.data.layout.Edge;
import org.reactome.server.tools.diagram.data.layout.Node;
import org.reactome.server.tools.diagram.data.layout.NodeCommon;
import org.reactome.server.tools.diagram.data.layout.NodeProperties;
import org.reactome.server.tools.diagram.data.layout.ReactionPart;
import org.reactome.server.tools.diagram.data.layout.Segment;
import org.reactome.server.tools.sbml.converter.SbmlConverter;
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
import org.sbml.jsbml.ext.layout.Point;
import org.sbml.jsbml.ext.layout.ReactionGlyph;
import org.sbml.jsbml.ext.layout.SpeciesGlyph;
import org.sbml.jsbml.ext.layout.SpeciesReferenceGlyph;
import org.sbml.jsbml.ext.layout.SpeciesReferenceRole;
import org.sbml.jsbml.ext.layout.TextGlyph;

/**
 * Converts a pathway's diagram file to the SBML layout package (spec 001, FR-007).
 *
 * A port of the relational converter's {@code LayoutConverter} (on {@code main}, {@code org.reactome.sbml.rel}),
 * rule for rule, reading the release's diagram file instead of the relational database's diagram. Where it
 * cannot follow the relational one, by decision (FR-007, differences.md):
 * <ul>
 *     <li>the coordinates are the diagram file's own;</li>
 *     <li>a node's size is the stored one, with no font-based resizing ({@code tightNodes});</li>
 *     <li>a label's box is its glyph's box: the file has no label size.</li>
 * </ul>
 * The relational diagram listed its components in the order they were added to it, which is their ids' as ids are
 * handed out (and was, in R-HSA-112409): the glyphs are written in the order of their diagram ids, each kind in its
 * own pass. T021 checks the order on every event compared.
 *
 * Unlike the relational converter, the layout has an id and dimensions, which SBML requires and libSBML checks.
 *
 * Unlike the relational converter, which logged any failure and wrote what it had, a diagram that cannot be laid
 * out (a node without a box, a reaction without a position) fails the conversion, naming the diagram.
 */
public class LayoutConverter {

    public static final String LAYOUT_ID_PREFIX = "layout_";
    public static final String TEXT_PREFIX = "text_";
    // Every glyph id has more after LAYOUT_ID_PREFIX ("layout_"), so none can be this
    public static final String LAYOUT_ID = "layout";

    // The relational renderer's constants: org.gk.render.DefaultRenderConstants.EDGE_TYPE_WIDGET_WIDTH and
    // RenderableRNA.LOOP_WIDTH.
    static final int EDGE_TYPE_WIDGET_WIDTH = 12;
    static final int RNA_LOOP_WIDTH = 16;
    private static final double SHIFT_VALUE = 0.5d;

    private static final Comparator<DiagramObject> BY_ID = Comparator.comparing(DiagramObject::getId);

    private LayoutConverter() {
    }

    /**
     * Whether the diagram draws a reaction: only then has the event a layout, as today.
     */
    public static boolean hasReactions(Diagram diagram) {
        return new Drawn(diagram).edges.stream().anyMatch(LayoutConverter::isReaction);
    }

    /**
     * The diagram step's copies in a disease diagram, which the relational diagram never had.
     *
     * For a disease diagram not drawn as a normal one, the diagram step (reactome/diagram-converter's DiagramFetcher,
     * through reactome-base's PathwayDiagramXMLGenerator and DiseasePathwayImageEditor) copies the glyph of the
     * normal reaction of each disease reaction the stored diagram does not draw, and adds a disease node over each
     * normal node that reaction replaces. The relational converter read the stored diagram alone, so its layout has
     * none of them (spec 001, differences.md). The file does not list them (#49), but two things the code
     * guarantees of every copy tell them apart:
     * <ul>
     *     <li>its id is above every stored object's, so above every normal object's: a new object takes its id
     *     from a counter that the reader moves past each stored id it reads, and nothing resets;</li>
     *     <li>it lies exactly over the stored object it copies, which has a lower id: a reaction's copy keeps its
     *     position (shallowCopy), and a node's its centre (copyRenderInfo copies its position; a node whose label
     *     overflows is later resized about its centre, so its box may not match).</li>
     * </ul>
     * Measured over all 1,137 human pathways with a layout in release 97's published SBML (453 of them disease
     * diagrams): the objects with both are exactly those today's layouts leave out. Either alone is not enough: an id above every normal object's
     * would also take disease objects the curators drew last (12 diagrams), and lying over an earlier object would
     * take disease reactions the curators drew over their normal ones (2). Drawing the copies is suggested in
     * sbml-exporter #48.
     */
    static Set<Long> copies(Diagram diagram) {
        if (!Boolean.FALSE.equals(diagram.getForNormalDraw())) return Collections.emptySet();
        List<Node> nodes = diagram.getNodes() == null ? Collections.emptyList() : diagram.getNodes();
        List<Edge> edges = diagram.getEdges() == null ? Collections.emptyList() : diagram.getEdges();
        List<DiagramObject> all = new ArrayList<>(nodes);
        all.addAll(edges);
        if (diagram.getCompartments() != null) all.addAll(diagram.getCompartments());
        if (diagram.getNotes() != null) all.addAll(diagram.getNotes());
        if (diagram.getLinks() != null) all.addAll(diagram.getLinks());
        long normalMax = Long.MIN_VALUE;
        for (DiagramObject o : all) {
            if (!Boolean.TRUE.equals(o.getIsDisease())) normalMax = Math.max(normalMax, o.getId());
        }
        Set<Long> rtn = new HashSet<>();
        if (normalMax == Long.MIN_VALUE) return rtn; // Nothing normal to tell the copies by
        for (Edge e : edges) {
            if (isNewDisease(e, normalMax) && e.getPosition() != null
                    && edges.stream().anyMatch(o -> o.getId() < e.getId() && samePoint(o.getPosition(), e.getPosition()))) {
                rtn.add(e.getId());
            }
        }
        for (Node n : nodes) {
            if (isNewDisease(n, normalMax) && n.getPosition() != null
                    && nodes.stream().anyMatch(o -> o.getId() < n.getId() && samePoint(o.getPosition(), n.getPosition()))) {
                rtn.add(n.getId());
            }
        }
        return rtn;
    }

    private static boolean isNewDisease(DiagramObject o, long normalMax) {
        return Boolean.TRUE.equals(o.getIsDisease()) && o.getId() > normalMax;
    }

    private static boolean samePoint(Coordinate a, Coordinate b) {
        return a != null && a.getX().equals(b.getX()) && a.getY().equals(b.getY());
    }

    /** The diagram's nodes and edges without the diagram step's copies: what the relational diagram drew. */
    private static final class Drawn {
        final List<Node> nodes;
        final List<Edge> edges;

        Drawn(Diagram diagram) {
            Set<Long> copies = copies(diagram);
            nodes = diagram.getNodes() == null ? new ArrayList<>() :
                    diagram.getNodes().stream().filter(n -> !copies.contains(n.getId())).collect(Collectors.toList());
            edges = diagram.getEdges() == null ? new ArrayList<>() :
                    diagram.getEdges().stream().filter(e -> !copies.contains(e.getId())).collect(Collectors.toList());
        }
    }

    private static boolean isReaction(Edge edge) {
        return edge.getReactomeId() != null && "Reaction".equals(edge.getRenderableClass());
    }

    /**
     * @throws IllegalStateException when the diagram cannot be laid out
     */
    public static void addLayout(Model model, Diagram diagram) {
        try {
            layOut(model, diagram);
        } catch (RuntimeException e) {
            throw new IllegalStateException("The diagram " + diagram.getStableId() + " cannot be laid out", e);
        }
    }

    private static void layOut(Model model, Diagram diagram) {
        LayoutModelPlugin lModel = new LayoutModelPlugin(model);
        model.addExtension(LayoutConstants.namespaceURI, lModel);
        // Unlike the relational converter's, whose layout libSBML refuses (no id, no dimensions): the layout
        // spans the diagram from its origin.
        Layout layout = lModel.createLayout(LAYOUT_ID);
        Drawn drawn = new Drawn(diagram);
        double[] bounds = getDiagramBounds(diagram, drawn.nodes);
        layout.createDimensions(Math.max(0, bounds[2]), Math.max(0, bounds[3]), 0);
        handleCompartments(layout, diagram, bounds);
        handleNodes(layout, drawn.nodes);
        handleReactions(layout, drawn.edges);
    }

    private static <T extends DiagramObject> List<T> byId(List<T> objects) {
        List<T> rtn = objects == null ? new ArrayList<>() : new ArrayList<>(objects);
        rtn.sort(BY_ID);
        return rtn;
    }

    private static void handleCompartments(Layout layout, Diagram diagram, double[] bounds) {
        Set<String> handled = new HashSet<>();
        for (Compartment comp : byId(diagram.getCompartments())) {
            if (comp.getReactomeId() == null) continue;
            String compartmentId = SbmlConverter.COMPARTMENT_PREFIX + comp.getReactomeId();
            if (!layout.getModel().containsCompartment(compartmentId)) continue;
            String id = LAYOUT_ID_PREFIX + comp.getId();
            CompartmentGlyph cg = layout.createCompartmentGlyph(id);
            cg.setCompartment(compartmentId);
            cg.setBoundingBox(createBox(comp.getProp()));
            handleText(id, layout, comp, cg.getBoundingBox());
            handled.add(compartmentId);
        }
        // The relational converter's hack to keep Minerva from drawing the undrawn compartments: a glyph of
        // 1 x 1 at the bottom centre of the diagram for each.
        ListOf<org.sbml.jsbml.Compartment> compartments = layout.getModel().getListOfCompartments();
        if (handled.size() == compartments.size()) return;
        int count = 0;
        BoundingBox box = createBox((int) ((bounds[0] + bounds[2]) / 2), (int) bounds[3], 1, 1);
        for (org.sbml.jsbml.Compartment compartment : compartments) {
            if (handled.contains(compartment.getId())) continue;
            CompartmentGlyph cg = layout.createCompartmentGlyph(LAYOUT_ID_PREFIX + "hidden_compartment_" + count++);
            cg.setCompartment(compartment);
            cg.setBoundingBox(box.clone());
        }
    }

    /**
     * minX, minY, maxX, maxY over every node's, note's and compartment's box (the relational diagram's nodes, a
     * note being one), in whole units as the relational diagram's rectangles had them.
     */
    private static double[] getDiagramBounds(Diagram diagram, List<Node> nodes) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        List<NodeCommon> all = new ArrayList<>(nodes);
        if (diagram.getNotes() != null) all.addAll(diagram.getNotes());
        if (diagram.getCompartments() != null) all.addAll(diagram.getCompartments());
        for (NodeCommon node : all) {
            NodeProperties p = node.getProp();
            minX = Math.min(minX, p.getX().intValue());
            minY = Math.min(minY, p.getY().intValue());
            maxX = Math.max(maxX, (int) (p.getX() + p.getWidth()));
            maxY = Math.max(maxY, (int) (p.getY() + p.getHeight()));
        }
        return new double[]{minX, minY, maxX, maxY};
    }

    private static void handleNodes(Layout layout, List<Node> nodes) {
        for (Node node : byId(nodes)) {
            if (node.getReactomeId() == null) continue;
            String speciesId = SbmlConverter.SPECIES_PREFIX + node.getReactomeId();
            // Some nodes are not in the model (a pathway, say)
            if (!layout.getModel().containsSpecies(speciesId)) continue;
            // A species may be drawn several times (ATP, say): the glyph's id is the node's, not the species'.
            String layoutId = LAYOUT_ID_PREFIX + node.getId();
            SpeciesGlyph sg = layout.createSpeciesGlyph(layoutId);
            sg.setSpecies(speciesId);
            sg.setBoundingBox(getNodeBox(node));
            handleText(layoutId, layout, node, sg.getBoundingBox());
            // Minerva reads the species' kind from its SBO term
            Species species = layout.getModel().getSpecies(speciesId);
            species.setSBOTerm(getSBOTerm(node.getRenderableClass()));
        }
    }

    private static BoundingBox getNodeBox(Node node) {
        NodeProperties p = node.getProp();
        // An RNA's arrow points inside its box
        if ("RNA".equals(node.getRenderableClass()) || "RNADrug".equals(node.getRenderableClass())) {
            return createBox(p.getX(), p.getY() + RNA_LOOP_WIDTH / 2, p.getWidth(), p.getHeight() - RNA_LOOP_WIDTH);
        }
        return createBox(p);
    }

    private static void handleText(String layoutId, Layout layout, NodeCommon node, BoundingBox box) {
        TextGlyph textGlyph = layout.createTextGlyph(TEXT_PREFIX + layoutId);
        textGlyph.setBoundingBox(box.clone());
        textGlyph.setText(node.getDisplayName());
        textGlyph.setGraphicalObject(layoutId);
    }

    private static void handleReactions(Layout layout, List<Edge> edges) {
        for (Edge edge : byId(edges)) {
            if (isReaction(edge)) handleReaction(layout, edge);
        }
    }

    /**
     * The backbone as points, with the relational converter's rules for SBML's sake: a two-point backbone gets
     * its midpoint, and a reaction with a single input (output) gets an input (output) branch, its backbone's
     * first (last) stretch. Midpoints are whole units, as the relational diagram's points were.
     *
     * @return the backbone; the reaction's hub, its position or, once a two-point backbone has its midpoint, that
     * midpoint (the relational reaction's validatePosition moved it there); and each role's branches by the
     * part's index (null: the role has none)
     */
    static Points reactionPoints(Edge edge) {
        List<Point2D> backbone = new ArrayList<>();
        List<Segment> segments = edge.getSegments() == null ? Collections.emptyList() : edge.getSegments();
        for (Segment segment : segments) {
            if (backbone.isEmpty()) backbone.add(point(segment.getFrom()));
            backbone.add(point(segment.getTo()));
        }
        Points rtn = new Points(backbone);
        rtn.hub = point(edge.getPosition());
        rtn.inputs = branches(edge.getInputs());
        rtn.outputs = branches(edge.getOutputs());
        if (backbone.size() == 2) {
            backbone.add(1, middle(backbone.get(0), backbone.get(1)));
            rtn.hub = backbone.get(1);
        }
        if (backbone.size() < 2) return rtn;
        if (edge.getInputs() != null && edge.getInputs().size() == 1) {
            Point2D first = backbone.get(0);
            backbone.set(0, middle(first, backbone.get(1)));
            rtn.inputs = Collections.singletonList(Collections.singletonList(first));
        }
        if (edge.getOutputs() != null && edge.getOutputs().size() == 1) {
            int last = backbone.size() - 1;
            Point2D end = backbone.get(last);
            backbone.set(last, middle(end, backbone.get(last - 1)));
            rtn.outputs = Collections.singletonList(Collections.singletonList(end));
        }
        return rtn;
    }

    static final class Points {
        final List<Point2D> backbone;
        Point2D hub;
        List<List<Point2D>> inputs;
        List<List<Point2D>> outputs;

        Points(List<Point2D> backbone) {
            this.backbone = backbone;
        }
    }

    /**
     * Each part's branch points, or null when no part of the role has any: the relational diagram then had no
     * branches for the role.
     */
    private static List<List<Point2D>> branches(List<ReactionPart> parts) {
        if (parts == null || parts.stream().allMatch(p -> p.getPoints() == null || p.getPoints().isEmpty())) return null;
        List<List<Point2D>> rtn = new ArrayList<>();
        for (ReactionPart part : parts) {
            List<Point2D> branch = new ArrayList<>();
            if (part.getPoints() != null) part.getPoints().forEach(c -> branch.add(point(c)));
            rtn.add(branch);
        }
        return rtn;
    }

    private static Point2D point(Coordinate c) {
        return new Point2D.Double(c.getX(), c.getY());
    }

    private static Point2D middle(Point2D a, Point2D b) {
        return new Point2D.Double((int) ((a.getX() + b.getX()) / 2), (int) ((a.getY() + b.getY()) / 2));
    }

    private static void handleReaction(Layout layout, Edge edge) {
        String rxtId = SbmlConverter.REACTION_PREFIX + edge.getReactomeId();
        if (!layout.getModel().containsReaction(rxtId)) return;
        Points points = reactionPoints(edge);
        List<Point2D> backbone = points.backbone;
        // Basic requirement for converting
        if (backbone.size() < 3) return;
        Point2D inputHub = backbone.get(0);
        Point2D outputHub = backbone.get(backbone.size() - 1);
        Point2D reactionHub = points.hub;
        String layoutId = LAYOUT_ID_PREFIX + edge.getId();
        ReactionGlyph rg = layout.createReactionGlyph(layoutId);
        rg.setReaction(rxtId);
        rg.setBoundingBox(createBox(reactionHub.getX() - EDGE_TYPE_WIDGET_WIDTH / 2.0d,
                                    reactionHub.getY() - EDGE_TYPE_WIDGET_WIDTH / 2.0d,
                                    EDGE_TYPE_WIDGET_WIDTH, EDGE_TYPE_WIDGET_WIDTH));
        convertToCurve(backbone, rg.createCurve());
        handleBranches(layout, edge.getInputs(), points.inputs, inputHub, SpeciesReferenceRole.SUBSTRATE, rg, layoutId);
        handleBranches(layout, edge.getOutputs(), points.outputs, outputHub, SpeciesReferenceRole.PRODUCT, rg, layoutId);
        handleBranches(layout, edge.getCatalysts(), branches(edge.getCatalysts()), reactionHub, SpeciesReferenceRole.MODIFIER, rg, layoutId);
        handleBranches(layout, edge.getActivators(), branches(edge.getActivators()), reactionHub, SpeciesReferenceRole.ACTIVATOR, rg, layoutId);
        handleBranches(layout, edge.getInhibitors(), branches(edge.getInhibitors()), reactionHub, SpeciesReferenceRole.INHIBITOR, rg, layoutId);
    }

    private static void handleBranches(Layout layout,
                                       List<ReactionPart> parts,
                                       List<List<Point2D>> branches,
                                       Point2D hub,
                                       SpeciesReferenceRole role,
                                       ReactionGlyph rg,
                                       String layoutId) {
        if (parts == null || parts.isEmpty()) return;
        for (int i = 0; i < parts.size(); i++) {
            String speciesGlyphId = LAYOUT_ID_PREFIX + parts.get(i).getId();
            // A part not drawn as a species (not in the model) gets no glyph, which would point at nothing (libSBML
            // 6021011); the others keep their numbers. No diagram of release 97 has one once the diagram step's
            // copies are left out: this keeps the layout valid if one does.
            if (layout.getSpeciesGlyph(speciesGlyphId) == null) continue;
            String speciesRefGlyphId = layoutId + "_" + role.toString().toLowerCase(Locale.ROOT) + "_" + i;
            SpeciesReferenceGlyph srg = rg.createSpeciesReferenceGlyph(speciesRefGlyphId);
            srg.setSpeciesGlyph(speciesGlyphId);
            // As the relational converter: no role is set, since Minerva could not read activators and
            // inhibitors.
            List<Point2D> branch;
            if (branches != null) {
                branch = new ArrayList<>(branches.get(i));
            } else {
                // A curve cannot be empty: a point a little off the hub
                branch = new ArrayList<>();
                branch.add(new Point2D.Double(hub.getX() + SHIFT_VALUE, hub.getY() + SHIFT_VALUE));
            }
            branch.add(hub);
            if (role == SpeciesReferenceRole.SUBSTRATE || role == SpeciesReferenceRole.PRODUCT) Collections.reverse(branch);
            convertToCurve(branch, srg.createCurve());
        }
    }

    private static void convertToCurve(List<Point2D> points, Curve curve) {
        for (int i = 0; i < points.size() - 1; i++) {
            curve.createLineSegment(convertPoint(points.get(i)), convertPoint(points.get(i + 1)));
        }
    }

    private static Point convertPoint(Point2D point) {
        Point rtn = new Point();
        rtn.setX(point.getX());
        rtn.setY(point.getY());
        return rtn;
    }

    private static BoundingBox createBox(NodeProperties p) {
        return createBox(p.getX(), p.getY(), p.getWidth(), p.getHeight());
    }

    private static BoundingBox createBox(double x, double y, double width, double height) {
        BoundingBox box = new BoundingBox();
        Point point = new Point();
        point.setX(x);
        point.setY(y);
        box.setPosition(point);
        Dimensions dimensions = new Dimensions();
        dimensions.setWidth(width);
        dimensions.setHeight(height);
        box.setDimensions(dimensions);
        return box;
    }

    /**
     * The relational converter's SBO terms by renderable class, after Minerva's
     * ({@code lcsb/mapviewer/converter/model/sbml/species/SBOTermSpeciesType}). The relational classes for a
     * protein, chemical or RNA drug extend the protein's, chemical's and RNA's, which it tested first, so those
     * drugs take their plain kind's term; only a set holding a drug took the drug's.
     */
    static String getSBOTerm(String renderableClass) {
        if (renderableClass == null) return "SBO:0000285";
        switch (renderableClass) {
            case "Protein":
            case "ProteinDrug":
                return "SBO:0000252";
            case "Complex":
            case "ComplexDrug":
            // The relational RenderableCell extends RenderableComplex
            case "Cell":
                return "SBO:0000297";
            case "Chemical":
            case "ChemicalDrug":
                return "SBO:0000247";
            case "RNA":
            case "RNADrug":
                return "SBO:0000278";
            case "Gene":
                return "SBO:0000243";
            case "EntitySetDrug":
                return "SBO:0000298";
            case "EntitySet":
                // For a functional compartment, on Minerva's advice
                return "SBO:0000289";
            default:
                return "SBO:0000285";
        }
    }
}
