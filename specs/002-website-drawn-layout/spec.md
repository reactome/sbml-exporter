# Feature Specification: SBML layout as the website draws the diagram

**Feature Branch**: `spec002/website-layout` (into `graph-only`)

**Created**: 2026-10-25

**Status**: Draft

**Input**: User description: "SBML layout as the website draws the diagram. Reactome is moving away from the old diagrams and how they were drawn; the SBML layout should no longer copy the relational converter's, and 'match today' no longer applies to the layout (it still applies to the model). Make the best layout we can, from the diagram files the website draws from."

## Why

Spec 001 makes SBML from the graph alone. Its layout is a rule-for-rule port of the
relational converter's: it reads the release's diagram files, but writes only what
the old converter wrote, the way it wrote it. Measured over release 97's 1,384
diagram files, that leaves out much of what the website shows:

- subpathway boxes: 1,380 process nodes and 1,811 encapsulated nodes;
- links: 2,701 between sets and their members, 841 between sets, 2,997 interactions
  and 80 flow lines;
- the 978 shadows that mark subpathways;
- modifications on 2,342 nodes;
- the disease copies that the diagram step adds to a disease diagram (issue #48);
- every style. Nothing marks the 60,452 faded nodes, the 937 crossed-out ones, the
  18,868 faded reactions or the 3,389 faded links. There are no shapes or colours:
  a species' SBO term tells tools that know SBO what kind of thing it is, and every
  other tool draws a plain box.

Reactions and subpathways drawn on an ancestor's diagram get no layout at all, and
neither does a diagram with no reactions (246 of the 1,384 files).

It also cannot match today's layout exactly. Today's box sizes come from the old
drawing code measuring labels in whatever font the exporting machine has. Reactome is
moving away from that code (decided 25 Oct 2026), so matching it is no longer the
goal. The goal is the diagram as the website draws it.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - The layout holds the whole diagram the website shows (Priority: P1)

A researcher downloads a pathway's SBML from the website, or a release archive, and
opens it in a tool that reads the SBML layout. They see every object the website shows
for that diagram, in the same place and at the same size: compartments, entities,
reactions and their branches, subpathway boxes, subpathway shadows, links,
modifications and labels. On a disease diagram they also see the disease objects drawn
over the normal ones, as the website shows them.

**Why this priority**: the layout is the part of the file a reader sees. Today it shows a
subset of the diagram, and nothing in it says what is missing.

**Independent Test**: convert the sample's events and, for each layout, list its glyphs
against the objects the website draws from the same diagram file. The two lists match one
for one, and every glyph's position and size equal the file's.

**Acceptance Scenarios**:

1. **Given** a pathway whose diagram has subpathway boxes, links and modifications, **When**
   it is converted, **Then** each has a glyph at its drawn position and size, and the glyph
   names the diagram object it comes from.
2. **Given** a disease diagram not drawn as a normal one, **When** it is converted,
   **Then** the layout has the normal and disease objects that the website draws from
   it (the diagram step's copies included), plus its normal pathway's shadows, faded, as
   the website adds them; and nothing that the website does not draw.
3. **Given** a node whose label would overflow its box, **When** it is converted, **Then**
   the box keeps the size stored in the diagram file.
4. **Given** a diagram object that is not an element of the SBML model (a subpathway box, a
   shadow, a link), **When** it is converted, **Then** it is drawn as a glyph that refers to
   no model element, so the model stays exactly as spec 001 writes it.
5. **Given** a diagram note, which the website does not draw, **When** it is converted,
   **Then** it has no glyph.
6. **Given** a diagram with no reactions, **When** it is converted, **Then** it has a
   layout, as the website draws it.

---

### User Story 2 - It looks like the website in tools that read render information (Priority: P2)

The same researcher opens the file in a tool that reads SBML render information. The
diagram looks like the website's:
- each kind of object has its shape (a protein, a complex, a set, a chemical, a gene, an
  RNA, a compartment, a subpathway);
- colours follow the website's diagram style;
- faded, crossed-out, loss-of-function and disease objects are marked as the website marks
  them. A crossed-out object is drawn unfaded, as the website draws it.

The website's zoom-dependent behaviour (hiding trivial molecules when zoomed out) is not
carried: a file is one static picture, and trivial molecules are drawn like the others.

**Why this priority**: with positions alone, every object is a plain box. The look is what
makes the diagram readable, but the positions come first.

**Independent Test**: for each kind of object and each mark in the sample, check that its
glyph gets the style the website uses for it. Render a few files in an SBML tool that reads
render information, and compare them by eye with the website's diagram.

**Acceptance Scenarios**:

1. **Given** a converted diagram, **When** it is read by a tool that supports render
   information, **Then** each glyph's shape and colours are those the website draws for its
   kind.
2. **Given** an object the website draws faded, crossed out or as loss-of-function, **When**
   it is converted, **Then** its glyph has the corresponding style.
3. **Given** a tool that ignores render information, **When** it reads the file, **Then** the
   layout still reads as in User Story 1.

---

### User Story 3 - Events drawn on an ancestor's diagram get a layout (Priority: P3)

A researcher downloads the SBML of a reaction, or of a subpathway with no diagram of its
own. Today the file has no layout. With this story it has the diagram the website opens
for that event: the ancestor's diagram. The event's own objects are drawn normally and the
rest dimmed, as the website dims the rest of a diagram when an event is pointed at in the
pathway hierarchy. Where several ancestors draw the event, the diagram is the one the
website opens for the event's id alone; the plan records how the website chooses it.

**Why this priority**: 12 of the sample's 30 events have no diagram of their own, but the
website still shows each of them on a diagram. It depends on Stories 1 and 2, since the
fading is a style.

**Independent Test**: convert the sample's events with no diagram of their own. Each has
the layout of the diagram the website opens for it. The event's own objects are drawn
normally and the rest faded.

**Acceptance Scenarios**:

1. **Given** a reaction drawn on its pathway's diagram, **When** it is converted, **Then** its
   layout is that diagram's, with the reaction and its participants in normal style and the
   rest dimmed.
2. **Given** a reaction that several pathways' diagrams draw, **When** it is converted,
   **Then** its layout is the diagram the website opens for it, by the recorded rule, every
   time.
3. **Given** an event that no diagram draws, **When** it is converted, **Then** it has no
   layout, as today.

---

### Edge Cases

- **A diagram file is missing for an event that should have one:** the conversion fails
  and names the file, as in spec 001. It does not write a file without a layout.
- **A disease diagram's normal pathway file is missing:** the conversion fails in the same
  way. The website draws the disease diagram without the normal pathway's shadows when that
  file does not load; a release export with a file missing is a broken release, and says
  so.
- **A glyph's target is not drawn as a species** (a branch to an object outside the model):
  it is drawn as a general glyph, not a species glyph, so that nothing points at a missing
  model element.
- **Two diagram objects share an id** (the normal pathway's shadows come from a second
  file, with its own ids): every glyph id stays unique and still says which object, in
  which file, it came from.
- **A set linked to itself** (21 set links in release 97): the website does not draw it, so
  it has no glyph.
- **A diagram with no reactions:** the layout is written anyway, as the website draws it.
  Today such a diagram gets no layout.
- **Very large diagrams** (Signal Transduction's): the file grows with the new glyphs and
  styles, and its size and conversion time are measured.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The layout MUST be made from the release's diagram files, read as the website
  reads them, and from the graph only where the website also asks it: a disease diagram's
  normal pathway, and the diagram that draws an event without its own (User Story 3).
- **FR-002**: Every object the website draws from a diagram MUST have exactly one glyph,
  and the layout MUST have no glyph for anything the website does not draw.
- **FR-003**: Every glyph MUST be at the position and size stored in the diagram file. No
  object may be resized or moved to fit its label. Where the website draws an object
  differently from the file, the file wins, and the plan lists each case and how the style
  stands in for it:
  - the website shortens RNA and cell boxes (their shapes are drawn in the file's box);
  - it draws a compartment's inner boundary (`insets`) as a second shape (one glyph, the
    inner line drawn by its style);
  - it draws a subpathway's shadow as a band along the subpathway's reactions, with a label
    placed between them (the glyph is the file's shadow shape and its label).
- **FR-004**: Every glyph MUST say which diagram object it was drawn from. That object is
  identified by its diagram id; for a shadow the website takes from the normal pathway's
  diagram, also by that diagram.
- **FR-005**: Objects that are SBML model elements (compartments, species, reactions and
  their participants) MUST have glyphs that refer to those elements. Every other object
  MUST have a glyph that refers to no model element.
- **FR-006**: The file MUST carry render information giving each glyph the website's shape,
  colours and marks (faded, crossed out, loss-of-function, disease), taken from the
  website's diagram style at a recorded version.
- **FR-007**: An event without a diagram of its own, but drawn on an ancestor's, MUST get
  that ancestor's layout, with its own objects in normal style and the rest faded (User
  Story 3).
- **FR-008**: The model part of the file (everything outside the layout and the render
  information) MUST be identical to what spec 001 writes for the same event. That includes
  the species' SBO terms: spec 001 takes a drawn species' SBO term from its node's class in
  the event's own diagram, and only there. A glyph this spec adds (a disease copy, an
  ancestor's diagram) never changes the model.
- **FR-013**: A diagram with no reactions MUST get a layout, since the website draws it.
  This changes spec 001's rule that only a diagram drawing a reaction gets one.
- **FR-009**: The output MUST pass libSBML's consistency check with zero errors, layout
  and render included, for the fixture and the sample.
- **FR-010**: The part that reads the diagram files MUST be the only part that knows their
  format, so that a new diagram source changes only that part.
- **FR-011**: The release's archives and the website's per-event downloads MUST get the
  same layout, from the same library.
- **FR-012**: The constitution's rule that the layout is compared with today's relational
  layout MUST be replaced by this spec's checks, in the same pull request as the spec
  (governance: an amendment states what changed and what it cost to learn).

### Key Entities

- **Diagram file**: a release's drawing of one pathway: its nodes, reactions (edges),
  compartments, links, shadows, notes and attachments, each with an id, a position and a
  size, plus flags (disease, faded, crossed out, loss-of-function) and, for a disease
  diagram, whether it is drawn as a normal one. Notes are in the file but the website does
  not draw them.
- **Drawn object**: one thing the website draws from a diagram file. It is the unit that
  layout glyphs are matched against.
- **Glyph**: an object's place in the SBML layout. It is linked to a model element when
  the object is one.
- **Style**: the shape, colours and marks the website gives a kind of object, carried as
  render information.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: For every event in the 30-event sample with a diagram, the layout's glyphs
  match the website's drawn objects one for one: none missing, none extra.
- **SC-002**: 100% of glyphs have the diagram file's position and size.
- **SC-003**: Every file in the sample and the fixture passes libSBML's consistency check
  with zero errors.
- **SC-004**: The model part is identical to spec 001's output for every event in the
  sample.
- **SC-005**: The 12 sample events drawn only on an ancestor's diagram each get a layout.
- **SC-006**: Opened in an SBML tool that reads render information, three sample diagrams
  (a large pathway, a disease one, a reaction) look like the website's at a glance, as
  judged by a curator.
- **SC-007**: For Signal Transduction (R-HSA-162582), the sample's largest pathway, the
  conversion time and the file size are each at most twice spec 001's.

## Assumptions

- Spec 001 finishes first: it removes MySQL. This spec builds on its library module, its
  diagram-file reader and its checks, on the `graph-only` branch.
- "What the website draws" is the Pathway Browser's diagram code in
  reactome/WebsiteAngular, at a commit recorded in the plan (748aca6f at the time of
  writing). The plan's research lists, from that code, exactly which objects and flags it
  draws and how it merges a disease diagram with its normal one.
- The diagram files stay the source: they are what the website draws from. If the
  website's diagrams move to another source, only the reader changes (FR-010).
- The interactors the website can show from other databases are not part of a release's
  diagram file, and are out of scope.
- Shapes and colours follow the website's diagram style. Matching them exactly, pixel for
  pixel, is not the goal: the measure is a recognisable likeness (SC-006).
- Issue #48 (drawing the disease copies) is settled by this spec: the layout draws what
  the website draws.
- Spec 001's own layout rule (compared with today's by meaning, the SBO override, a layout
  only for diagrams that draw a reaction) stands until this spec's work replaces it; the
  SBO override stays, as part of the model (FR-008).
- The code stays in Java for now. The team intends to move to Kotlin eventually, for null
  safety, but not in the middle of this work.
- Public repository: no internal hosts, paths or people's names in the spec, its fixtures
  or its output checks.
