package org.reactome.server.tools.sbml.layout;

import org.junit.jupiter.api.Test;
import org.reactome.server.tools.diagram.data.DiagramFactory;
import org.reactome.server.tools.diagram.data.layout.Diagram;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.TidySBMLWriter;
import org.sbml.jsbml.ext.layout.BoundingBox;
import org.sbml.jsbml.ext.layout.CompartmentGlyph;
import org.sbml.jsbml.ext.layout.Curve;
import org.sbml.jsbml.ext.layout.CurveSegment;
import org.sbml.jsbml.ext.layout.Layout;
import org.sbml.jsbml.ext.layout.LayoutConstants;
import org.sbml.jsbml.ext.layout.LayoutModelPlugin;
import org.sbml.jsbml.ext.layout.ReactionGlyph;
import org.sbml.jsbml.ext.layout.SpeciesGlyph;
import org.sbml.jsbml.ext.layout.SpeciesReferenceGlyph;
import org.sbml.jsbml.ext.layout.TextGlyph;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The relational LayoutConverter's rules, ported to the diagram files (spec 001, T020, FR-007): each test
 * names the rule it holds.
 */
class LayoutConverterTest {

    // Two species in a compartment, one reaction; each test adds what its rule needs.
    private static final String NODES =
            "{\"id\": 20, \"reactomeId\": 2, \"displayName\": \"B\", \"renderableClass\": \"Chemical\", " +
            " \"prop\": {\"x\": 300, \"y\": 100, \"width\": 40, \"height\": 30}}," +
            "{\"id\": 10, \"reactomeId\": 1, \"displayName\": \"A\", \"renderableClass\": \"Protein\", " +
            " \"prop\": {\"x\": 100, \"y\": 100, \"width\": 60, \"height\": 20}}";
    private static final String COMPARTMENTS =
            "{\"id\": 5, \"reactomeId\": 7, \"displayName\": \"cytosol\", \"renderableClass\": \"Compartment\", " +
            " \"prop\": {\"x\": 50, \"y\": 50, \"width\": 400, \"height\": 300}}";

    private static Diagram diagram(String nodes, String edges, String compartments) throws Exception {
        return DiagramFactory.getDiagram("{\"dbId\": 1, \"stableId\": \"R-TST-1\", \"nodes\": [" + nodes + "], " +
                "\"edges\": [" + edges + "], \"compartments\": [" + compartments + "]}");
    }

    private static String edge(long id, long reactomeId, String segments, String position, String parts) {
        return "{\"id\": " + id + ", \"reactomeId\": " + reactomeId + ", \"renderableClass\": \"Reaction\", " +
                "\"position\": " + position + ", \"segments\": [" + segments + "]" + (parts.isEmpty() ? "" : ", " + parts) + "}";
    }

    private static String seg(int x1, int y1, int x2, int y2) {
        return "{\"from\": {\"x\": " + x1 + ", \"y\": " + y1 + "}, \"to\": {\"x\": " + x2 + ", \"y\": " + y2 + "}}";
    }

    private static String at(int x, int y) {
        return "{\"x\": " + x + ", \"y\": " + y + "}";
    }

    /** A model with the given species, compartments and reactions (Reactome ids). */
    private static Model model(long[] species, long[] compartments, long[] reactions) {
        Model model = new SBMLDocument(3, 1).createModel("pathway_1");
        for (long c : compartments) model.createCompartment("compartment_" + c);
        for (long s : species) model.createSpecies("species_" + s);
        for (long r : reactions) model.createReaction("reaction_" + r);
        return model;
    }

    private static Layout layout(Model model) {
        return ((LayoutModelPlugin) model.getExtension(LayoutConstants.namespaceURI)).getLayout(0);
    }

    /** A curve as x1,y1-x2,y2 per segment. */
    private static List<String> points(Curve curve) {
        List<String> rtn = new ArrayList<>();
        for (CurveSegment s : curve.getListOfCurveSegments()) {
            rtn.add(fmt(s.getStart().getX()) + "," + fmt(s.getStart().getY()) + "-" + fmt(s.getEnd().getX()) + "," + fmt(s.getEnd().getY()));
        }
        return rtn;
    }

    private static String fmt(double d) {
        return d == Math.floor(d) ? String.valueOf((long) d) : String.valueOf(d);
    }

    private static String box(BoundingBox b) {
        return fmt(b.getPosition().getX()) + "," + fmt(b.getPosition().getY()) + " " + fmt(b.getDimensions().getWidth()) + "x" + fmt(b.getDimensions().getHeight());
    }

    // A reaction 1 + 1 -> ..., with a three-point backbone and both inputs branched.
    private static final String TWO_INPUTS = edge(30, 3, seg(200, 110, 230, 110) + "," + seg(230, 110, 260, 110), at(230, 110),
            "\"inputs\": [{\"id\": 20, \"points\": [" + at(190, 90) + "]}, {\"id\": 10, \"points\": [" + at(190, 130) + "]}]");

    private static Diagram disease(Boolean forNormalDraw, String nodes, String edges, String compartments) throws Exception {
        return DiagramFactory.getDiagram("{\"dbId\": 1, \"stableId\": \"R-TST-1\"" +
                (forNormalDraw == null ? "" : ", \"forNormalDraw\": " + forNormalDraw) + ", \"nodes\": [" + nodes + "], " +
                "\"edges\": [" + edges + "], \"compartments\": [" + compartments + "]}");
    }

    // A disease diagram: normal objects up to id 50 (the compartment), a disease node the curators drew (20), a
    // normal reaction (45), and the diagram step's copies (90 over node 10, 95 over reaction 45), whose ids its
    // counter took past the stored diagram's. The copied node keeps its original's position (centre) but has
    // another box, as a node whose label overflows gets; its tall box shows it is left out of the bounds.
    private static final String DRAWN_DISEASE = "{\"id\": 20, \"reactomeId\": 2, \"displayName\": \"B\", \"renderableClass\": \"Chemical\", " +
            "\"isDisease\": true, \"position\": {\"x\": 320, \"y\": 115}, \"prop\": {\"x\": 300, \"y\": 100, \"width\": 40, \"height\": 30}}";
    private static final String NORMAL = "{\"id\": 10, \"reactomeId\": 1, \"displayName\": \"A\", \"renderableClass\": \"Protein\", " +
            "\"isFadeOut\": true, \"position\": {\"x\": 130, \"y\": 110}, \"prop\": {\"x\": 100, \"y\": 100, \"width\": 60, \"height\": 20}}";
    private static final String COPY_NODE = "{\"id\": 90, \"reactomeId\": 8, \"displayName\": \"C\", \"renderableClass\": \"Protein\", " +
            "\"isDisease\": true, \"position\": {\"x\": 130, \"y\": 110}, \"prop\": {\"x\": 90, \"y\": 60, \"width\": 80, \"height\": 900}}";
    private static final String COPY_EDGE = edge(95, 3, seg(200, 110, 230, 110) + "," + seg(230, 110, 260, 110), at(230, 110),
            "\"isDisease\": true, \"inputs\": [{\"id\": 90}]");
    private static final String NORMAL_COMPARTMENT = COMPARTMENTS.replace("\"id\": 5,", "\"id\": 50,");
    private static final String NORMAL_EDGE = edge(45, 4, seg(200, 110, 230, 110) + "," + seg(230, 110, 260, 110), at(230, 110),
            "\"isFadeOut\": true");

    @Test
    void theDiagramStepsDiseaseCopiesAreLeftOut() throws Exception {
        Diagram diagram = disease(false, NORMAL + "," + DRAWN_DISEASE + "," + COPY_NODE, NORMAL_EDGE + "," + COPY_EDGE, NORMAL_COMPARTMENT);
        assertEquals(java.util.Set.of(90L, 95L), LayoutConverter.copies(diagram));
        // A copy always lies over an earlier reaction, so a diagram with copies draws one anyway
        assertTrue(LayoutConverter.hasReactions(diagram));

        Model model = model(new long[]{1, 2, 8}, new long[]{9}, new long[]{3});
        LayoutConverter.addLayout(model, diagram);
        Layout layout = layout(model);
        assertEquals(List.of("layout_10", "layout_20"), layout.getListOfSpeciesGlyphs().stream().map(SpeciesGlyph::getId).collect(Collectors.toList()));
        assertEquals(0, layout.getNumReactionGlyphs());
        assertTrue(layout.getListOfSpeciesGlyphs().stream().noneMatch(g -> g.getId().equals("layout_90")));
        // Nor are they in the bounds: the copy node reaches y 960, the compartment 350
        assertEquals("250,350 1x1", box(layout.getCompartmentGlyph("layout_hidden_compartment_0").getBoundingBox()));
    }

    @Test
    void aDiseaseObjectTheCuratorsDrewLastIsNoCopy() throws Exception {
        // Above every normal object, but over nothing earlier: drawn by the curators after the normal objects
        String late = DRAWN_DISEASE.replace("\"id\": 20,", "\"id\": 60,");
        String lateEdge = edge(61, 6, seg(400, 300, 430, 300) + "," + seg(430, 300, 460, 300), at(430, 300), "\"isDisease\": true");
        assertEquals(java.util.Set.of(), LayoutConverter.copies(disease(false, NORMAL + "," + late, NORMAL_EDGE + "," + lateEdge, NORMAL_COMPARTMENT)));
    }

    @Test
    void aDiseaseReactionTheCuratorsDrewOverItsNormalOneIsNoCopy() throws Exception {
        // Over the normal reaction, but below a normal object's id: stored, not copied
        String over = COPY_EDGE.replace("\"id\": 95,", "\"id\": 46,");
        assertEquals(java.util.Set.of(), LayoutConverter.copies(disease(false, NORMAL, NORMAL_EDGE + "," + over, NORMAL_COMPARTMENT)));
    }

    @Test
    void onlyADiseaseDiagramNotDrawnAsNormalHasCopies() throws Exception {
        assertEquals(java.util.Set.of(), LayoutConverter.copies(disease(true, NORMAL + "," + COPY_NODE, COPY_EDGE, NORMAL_COMPARTMENT)));
        assertEquals(java.util.Set.of(), LayoutConverter.copies(disease(null, NORMAL + "," + COPY_NODE, COPY_EDGE, NORMAL_COMPARTMENT)));
        // With nothing normal, nothing tells the copies apart
        String under = DRAWN_DISEASE.replace("\"position\": {\"x\": 320, \"y\": 115}", "\"position\": {\"x\": 130, \"y\": 110}");
        assertEquals(java.util.Set.of(), LayoutConverter.copies(disease(false, under + "," + COPY_NODE, "", "")));
    }

    @Test
    void aLayoutOnlyWhenTheDiagramDrawsAReaction() throws Exception {
        assertTrue(LayoutConverter.hasReactions(diagram(NODES, TWO_INPUTS, COMPARTMENTS)));
        assertFalse(LayoutConverter.hasReactions(diagram(NODES, "", COMPARTMENTS)));
        // As the relational converter's RenderableReaction: an edge of another class is not a reaction
        assertFalse(LayoutConverter.hasReactions(diagram(NODES, TWO_INPUTS.replace("\"Reaction\"", "\"Interaction\""), COMPARTMENTS)));
    }

    @Test
    void glyphsAreInDiagramIdOrderWithTheDiagramsIds() throws Exception {
        Model model = model(new long[]{1, 2}, new long[]{7}, new long[]{3, 4});
        String second = edge(25, 4, seg(0, 0, 10, 0) + "," + seg(10, 0, 20, 0), at(10, 0), "");
        LayoutConverter.addLayout(model, diagram(NODES, TWO_INPUTS + "," + second, COMPARTMENTS));
        Layout layout = layout(model);

        assertEquals(List.of("layout_10", "layout_20"), layout.getListOfSpeciesGlyphs().stream().map(SpeciesGlyph::getId).collect(Collectors.toList()));
        assertEquals(List.of("layout_25", "layout_30"), layout.getListOfReactionGlyphs().stream().map(ReactionGlyph::getId).collect(Collectors.toList()));
        assertEquals("species_1", layout.getSpeciesGlyph("layout_10").getSpecies());
        assertEquals("reaction_3", layout.getReactionGlyph("layout_30").getReaction());
        assertEquals("compartment_7", layout.getCompartmentGlyph("layout_5").getCompartment());
    }

    @Test
    void whatIsNotInTheModelIsNotDrawn() throws Exception {
        Model model = model(new long[]{1}, new long[]{}, new long[]{});
        LayoutConverter.addLayout(model, diagram(NODES, TWO_INPUTS, COMPARTMENTS));
        Layout layout = layout(model);

        assertEquals(1, layout.getNumSpeciesGlyphs());
        assertEquals(0, layout.getNumReactionGlyphs());
        assertEquals(0, layout.getNumCompartmentGlyphs());
    }

    @Test
    void aNodesGlyphIsItsBoxAndItsLabelTheSameBox() throws Exception {
        Model model = model(new long[]{1, 2}, new long[]{7}, new long[]{});
        LayoutConverter.addLayout(model, diagram(NODES, "", COMPARTMENTS));
        Layout layout = layout(model);

        assertEquals("100,100 60x20", box(layout.getSpeciesGlyph("layout_10").getBoundingBox()));
        TextGlyph text = layout.getTextGlyph("text_layout_10");
        assertEquals("A", text.getText());
        assertEquals("layout_10", text.getGraphicalObject());
        assertEquals("100,100 60x20", box(text.getBoundingBox()));
        // Compartments' labels come first, as their pass does
        assertEquals("text_layout_5", layout.getListOfTextGlyphs().get(0).getId());
        assertEquals("50,50 400x300", box(layout.getTextGlyph("text_layout_5").getBoundingBox()));
    }

    @Test
    void anRnasBoxLeavesOutItsLoop() throws Exception {
        Model model = model(new long[]{8}, new long[]{}, new long[]{});
        String rna = "{\"id\": 40, \"reactomeId\": 8, \"displayName\": \"R\", \"renderableClass\": \"RNA\", " +
                "\"prop\": {\"x\": 10, \"y\": 20, \"width\": 50, \"height\": 40}}";
        LayoutConverter.addLayout(model, diagram(rna, "", ""));
        assertEquals("10,28 50x24", box(layout(model).getSpeciesGlyph("layout_40").getBoundingBox()));
    }

    @Test
    void speciesTakeTheirDrawingsSboTerm() throws Exception {
        Model model = model(new long[]{1, 2}, new long[]{}, new long[]{});
        LayoutConverter.addLayout(model, diagram(NODES, "", ""));
        assertEquals("SBO:0000252", model.getSpecies("species_1").getSBOTermID());
        assertEquals("SBO:0000247", model.getSpecies("species_2").getSBOTermID());

        assertEquals("SBO:0000297", LayoutConverter.getSBOTerm("Complex"));
        // The relational RenderableCell extends RenderableComplex
        assertEquals("SBO:0000297", LayoutConverter.getSBOTerm("Cell"));
        assertEquals("SBO:0000278", LayoutConverter.getSBOTerm("RNA"));
        assertEquals("SBO:0000243", LayoutConverter.getSBOTerm("Gene"));
        assertEquals("SBO:0000289", LayoutConverter.getSBOTerm("EntitySet"));
        assertEquals("SBO:0000298", LayoutConverter.getSBOTerm("EntitySetDrug"));
        // The relational protein, chemical and RNA drugs extend the plain kinds, which were tested first
        assertEquals("SBO:0000252", LayoutConverter.getSBOTerm("ProteinDrug"));
        assertEquals("SBO:0000247", LayoutConverter.getSBOTerm("ChemicalDrug"));
        assertEquals("SBO:0000278", LayoutConverter.getSBOTerm("RNADrug"));
        assertEquals("SBO:0000285", LayoutConverter.getSBOTerm("ProcessNode"));
    }

    @Test
    void undrawnCompartmentsGetHiddenGlyphsAtTheBottomCentre() throws Exception {
        Model model = model(new long[]{}, new long[]{9, 7, 6}, new long[]{});
        LayoutConverter.addLayout(model, diagram(NODES, "", COMPARTMENTS));
        Layout layout = layout(model);

        List<CompartmentGlyph> glyphs = layout.getListOfCompartmentGlyphs();
        assertEquals(List.of("layout_5", "layout_hidden_compartment_0", "layout_hidden_compartment_1"),
                glyphs.stream().map(CompartmentGlyph::getId).collect(Collectors.toList()));
        // In the model's order; the diagram spans x 50-450, y 50-350
        assertEquals("compartment_9", glyphs.get(1).getCompartment());
        assertEquals("compartment_6", glyphs.get(2).getCompartment());
        assertEquals("250,350 1x1", box(glyphs.get(1).getBoundingBox()));
    }

    @Test
    void theLayoutHasAnIdAndSpansTheDiagram() throws Exception {
        // libSBML refuses a layout without them (6020305, 6020315), as today's are
        Model model = model(new long[]{1, 2}, new long[]{7}, new long[]{3});
        LayoutConverter.addLayout(model, diagram(NODES, TWO_INPUTS, COMPARTMENTS));
        Layout layout = layout(model);
        assertEquals("layout", layout.getId());
        assertEquals(450d, layout.getDimensions().getWidth());
        assertEquals(350d, layout.getDimensions().getHeight());
    }

    @Test
    void notesCountInTheDiagramsBounds() throws Exception {
        Model model = model(new long[]{}, new long[]{9}, new long[]{});
        String note = "{\"id\": 60, \"displayName\": \"a note\", \"renderableClass\": \"Note\", " +
                "\"prop\": {\"x\": 500, \"y\": 600, \"width\": 100, \"height\": 20}}";
        Diagram diagram = DiagramFactory.getDiagram("{\"dbId\": 1, \"stableId\": \"R-TST-1\", \"nodes\": [" + NODES + "], " +
                "\"edges\": [], \"compartments\": [" + COMPARTMENTS + "], \"notes\": [" + note + "]}");
        LayoutConverter.addLayout(model, diagram);
        // x 50-600, y 50-620
        assertEquals("325,620 1x1", box(layout(model).getCompartmentGlyph("layout_hidden_compartment_0").getBoundingBox()));
    }

    @Test
    void aDiagramThatCannotBeLaidOutFailsNamingIt() throws Exception {
        Model model = model(new long[]{1}, new long[]{}, new long[]{});
        String noBox = "{\"id\": 10, \"reactomeId\": 1, \"displayName\": \"A\", \"renderableClass\": \"Protein\"}";
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> LayoutConverter.addLayout(model, diagram(noBox, "", "")));
        assertEquals("The diagram R-TST-1 cannot be laid out", e.getMessage());
    }

    @Test
    void aReactionsGlyphIsAWidgetAtItsPosition() throws Exception {
        Model model = model(new long[]{1, 2}, new long[]{}, new long[]{3});
        LayoutConverter.addLayout(model, diagram(NODES, TWO_INPUTS, ""));
        ReactionGlyph rg = layout(model).getReactionGlyph("layout_30");
        assertEquals("224,104 12x12", box(rg.getBoundingBox()));
        assertEquals(List.of("200,110-230,110", "230,110-260,110"), points(rg.getCurve()));
    }

    @Test
    void substratesRunFromTheHubToTheirNode() throws Exception {
        Model model = model(new long[]{1, 2}, new long[]{}, new long[]{3});
        LayoutConverter.addLayout(model, diagram(NODES, TWO_INPUTS, ""));
        ReactionGlyph rg = layout(model).getReactionGlyph("layout_30");

        SpeciesReferenceGlyph first = rg.getListOfSpeciesReferenceGlyphs().get(0);
        assertEquals("layout_30_substrate_0", first.getId());
        assertEquals("layout_20", first.getSpeciesGlyph());
        assertEquals(List.of("200,110-190,90"), points(first.getCurve()));
        assertEquals("layout_10", rg.getListOfSpeciesReferenceGlyphs().get(1).getSpeciesGlyph());
        // No role, as the relational converter left it
        assertFalse(first.isSetSpeciesReferenceRole());
    }

    @Test
    void aPartNotDrawnAsASpeciesHasNoGlyph() throws Exception {
        // species_2 is not in the model, so node 20 has no species glyph
        Model model = model(new long[]{1}, new long[]{}, new long[]{3});
        LayoutConverter.addLayout(model, diagram(NODES, TWO_INPUTS, ""));
        ReactionGlyph rg = layout(model).getReactionGlyph("layout_30");
        assertEquals(List.of("layout_30_substrate_1"),
                rg.getListOfSpeciesReferenceGlyphs().stream().map(SpeciesReferenceGlyph::getId).collect(Collectors.toList()));
    }

    @Test
    void modifiersRunFromTheirNodeToTheReactionsPosition() throws Exception {
        Model model = model(new long[]{1, 2}, new long[]{}, new long[]{3});
        String catalysed = edge(30, 3, seg(200, 110, 230, 110) + "," + seg(230, 110, 260, 110), at(230, 110),
                "\"catalysts\": [{\"id\": 20, \"points\": [" + at(300, 50) + "," + at(240, 60) + "]}], " +
                "\"inhibitors\": [{\"id\": 10}]");
        LayoutConverter.addLayout(model, diagram(NODES, catalysed, ""));
        ReactionGlyph rg = layout(model).getReactionGlyph("layout_30");

        SpeciesReferenceGlyph catalyst = rg.getSpeciesReferenceGlyph("layout_30_modifier_0");
        assertEquals(List.of("300,50-240,60", "240,60-230,110"), points(catalyst.getCurve()));
        // A role without branch points: a point half a unit off the hub
        SpeciesReferenceGlyph inhibitor = rg.getSpeciesReferenceGlyph("layout_30_inhibitor_0");
        assertEquals(List.of("230.5,110.5-230,110"), points(inhibitor.getCurve()));
    }

    @Test
    void aTwoPointBackboneGetsItsWholeMidpoint() throws Exception {
        String twoPoints = edge(30, 3, seg(201, 110, 260, 111), at(230, 110), "");
        LayoutConverter.Points points = LayoutConverter.reactionPoints(diagram(NODES, twoPoints, "").getEdges().get(0));
        assertEquals(List.of("201,110", "230,110", "260,111"),
                points.backbone.stream().map(p -> fmt(p.getX()) + "," + fmt(p.getY())).collect(Collectors.toList()));
    }

    @Test
    void aTwoPointBackbonesHubIsItsMidpoint() throws Exception {
        // The relational reaction's validatePosition moved a position at an end to the inserted midpoint
        Model model = model(new long[]{1, 2}, new long[]{}, new long[]{3});
        String twoPoints = edge(30, 3, seg(200, 110, 260, 110), at(200, 110),
                "\"inputs\": [{\"id\": 10}, {\"id\": 20}], \"catalysts\": [{\"id\": 20}]");
        LayoutConverter.addLayout(model, diagram(NODES, twoPoints, ""));
        ReactionGlyph rg = layout(model).getReactionGlyph("layout_30");
        assertEquals("224,104 12x12", box(rg.getBoundingBox()));
        assertEquals(List.of("230.5,110.5-230,110"), points(rg.getSpeciesReferenceGlyph("layout_30_modifier_0").getCurve()));
    }

    @Test
    void aSingleInputOrOutputGetsABranchFromTheBackbonesEnd() throws Exception {
        Model model = model(new long[]{1, 2}, new long[]{}, new long[]{3});
        String single = edge(30, 3, seg(200, 110, 230, 110) + "," + seg(230, 110, 261, 110), at(230, 110),
                "\"inputs\": [{\"id\": 10}], \"outputs\": [{\"id\": 20}]");
        LayoutConverter.addLayout(model, diagram(NODES, single, ""));
        ReactionGlyph rg = layout(model).getReactionGlyph("layout_30");

        assertEquals(List.of("215,110-230,110", "230,110-245,110"), points(rg.getCurve()));
        assertEquals(List.of("215,110-200,110"), points(rg.getSpeciesReferenceGlyph("layout_30_substrate_0").getCurve()));
        assertEquals(List.of("245,110-261,110"), points(rg.getSpeciesReferenceGlyph("layout_30_product_0").getCurve()));
    }

    @Test
    void aReactionWithNoSegmentsIsDrawnAtItsHub() throws Exception {
        // The relational diagram stored its backbone as one repeated point, which the diagram step leaves out as
        // segments of no length: today's layout draws it at its hub (10 reactions of release 97's human pathways)
        Model model = model(new long[]{1, 2}, new long[]{}, new long[]{3});
        String noSegments = edge(30, 3, "", at(1176, 411),
                "\"inputs\": [{\"id\": 10}, {\"id\": 20}], \"outputs\": [{\"id\": 10}, {\"id\": 20}], \"catalysts\": [{\"id\": 20}]");
        LayoutConverter.addLayout(model, diagram(NODES, noSegments, ""));
        ReactionGlyph rg = layout(model).getReactionGlyph("layout_30");
        assertEquals("1170,405 12x12", box(rg.getBoundingBox()));
        assertEquals(List.of("1176,411-1176,411", "1176,411-1176,411"), points(rg.getCurve()));
        assertEquals(5, rg.getSpeciesReferenceGlyphCount());
    }

    @Test
    void theDocumentDeclaresTheLayoutPackageAsNotRequired() throws Exception {
        Model model = model(new long[]{1, 2}, new long[]{}, new long[]{3});
        LayoutConverter.addLayout(model, diagram(NODES, TWO_INPUTS, ""));
        String xml = new TidySBMLWriter().writeSBMLToString(model.getSBMLDocument());
        assertTrue(xml.contains("xmlns:layout=\"http://www.sbml.org/sbml/level3/version1/layout/version1\""), xml);
        assertTrue(xml.contains("layout:required=\"false\""), xml);
    }
}
