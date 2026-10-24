# Feature Specification: SBML from the graph alone, for the release and the website

**Feature Branch**: `graph-only` (an integration branch; see "How the work lands")
**Created**: 2026-10-24
**Status**: Draft
**Input**: Issue [#14](https://github.com/reactome/sbml-exporter/issues/14). Make the graph-only `SbmlConverter` the one SBML implementation, used by the release's bulk archives and by the website's per-event downloads, with no relational database. Fix its five defects, draw the layout from the diagram files, give it a graph-only command line, and split it into a library and a command line. Do the work on a branch until it is proven, then merge it into `main`.

## Why

Today both of Reactome's SBML outputs come from `SbmlConverterForRel`, which reads the relational release database for its reactions and for the layout:

- **the release's archives**, `all_species.3.1.sbml.tgz` and `homo_sapiens.3.1.sbml.tgz`, made by this repository's command line;
- **the website's per-event download**, `/ContentService/exporter/event/{id}.sbml`, made by the same converter inside ContentService.

The graph-only `SbmlConverter` has been in this repository since 2021, but nothing calls it. Run against release 97's graph (reactome/WebsiteAngular spec 015, research R1 and R9), it had five data defects that nothing caught, because nothing compared its output with anything (constitution, Principle I).

The website's own half of this work (its routes, its links and the service that calls the library) is in [WebsiteAngular spec 015](https://github.com/reactome/WebsiteAngular/tree/main/specs/015-graph-exports). That spec also holds the measurements and the 30-event sample this spec relies on.

## How the work lands

- **On the `graph-only` branch**, through one pull request per task, each reviewed. CI runs on every pull request, whatever its base.
- **`main` is merged into `graph-only` by pull request** whenever `main` changes, so CI runs on the result and the final merge holds no surprises.
- **`graph-only` is protected** as `main` is: changes by pull request only, no force-pushes.
- **The work on `graph-only` is provisional** until this repository's maintainers answer issue #14 (spec 015, FR-010). Nothing reaches `main` without their agreement.
- **`main` keeps what the release uses today** until the whole of this spec is proven (FR-011). Then `graph-only` merges into `main` in one pull request. Because CI publishes the image as `latest` on every push to `main`, and the release runs `latest`, that merge changes the next release run; it is timed with the release's maintainers.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - The build and its checks work (Priority: P1)

A developer changes the converter, and the build, the tests, the lint and the SBML validity check each pass or fail on that change.

Today none of them can (constitution, Principle II):

- **The build fails at compile.** The `verifier-lib:1.0-SNAPSHOT` published to the Central snapshot repository on 19 Oct 2026 lacks `DefaultVerifier` and `Verifier`, which `SBMLExportVerifier` imports, and the previous snapshot, which had them, now answers 404. Fixing the published library is its maintainer's; this branch must not wait for it.
- **There are no tests.**
- **The lint stage** runs `mvn checkstyle:check` with no configuration, so it uses Checkstyle's default rules and reports more than a thousand existing violations. The repository's own `checkstyle.xml` holds one rule (lines up to 350 characters), which the code already meets.
- **Nothing checks that the output is valid SBML.**

**Why this priority**: every later story's proof runs through these checks.

**Independent Test**: on a pull request into `graph-only`, CI builds the image, runs the tests, runs lint and runs the validity check, and each is seen failing once on a deliberately broken change.

**Acceptance Scenarios**:

1. **Given** the published `verifier-lib` snapshot is still broken, **When** CI builds the branch, **Then** the build compiles, with `verifier-lib` built from `release-verifier-lib` commit `ac71fb3` instead of the published snapshot, and the verifier jar starts and prints its usage with no missing-class error.
2. **Given** a change that breaks a tested behaviour, **When** CI runs, **Then** the tests fail.
3. **Given** a line over the repository's `checkstyle.xml` limit, **When** CI runs, **Then** lint fails on it.
4. **Given** output with an SBML consistency error, **When** CI runs the validity check, **Then** it fails.

---

### User Story 2 - The model is correct (Priority: P1)

A researcher downloads SBML for a pathway or reaction and gets every reactant, product, catalyst and regulator the graph holds, in the right roles.

**Why this priority**: the five defects make the graph-only output wrong; nothing can switch to it until they are fixed.

**Independent Test**: the fixed converter's models are compared element by element with today's relational output of the same release over WebsiteAngular spec 015's 30-event sample, with three things set aside: the generation note's timestamp; the order of elements, which are matched by id; and, until US3, the layout and what adding it changes in the model (the drawn species' SBO terms, `layout:required` and the layout namespace).

**Acceptance Scenarios**:

1. **Given** R-HSA-69604, **When** its SBML is written, **Then** it has two reactants, two products and one catalyst, as the relational converter's file has; unfixed, it has one of each and the positive regulators appear as catalysts.
2. **Given** each of the five defects, **When** its fix is undone, **Then** a unit test fails.
3. **Given** any event in the sample, **When** its model is written, **Then** it matches today's relational file of the same release apart from what is set aside above, or the difference is recorded with its reason.

---

### User Story 3 - The layout comes from the diagram files (Priority: P1)

A researcher opens a pathway's SBML in a tool that reads the layout package and sees the same picture as the website's diagram.

**Why this priority**: once the model is fixed, the layout is the only thing the graph-only converter would still need the relational database for (`LayoutConverter` reads its `PathwayDiagram`).

**Independent Test**: for every event in the sample whose own diagram draws a reaction, the new layout is compared by meaning with today's.

**Acceptance Scenarios**:

1. **Given** a pathway whose own diagram draws a reaction, **When** its SBML is written, **Then** the layout has the same glyphs, ids, sets, roles, targets and order as today's, at the diagram's own coordinates. (For R-HSA-69620, today's are the diagram's shifted by a fixed +48, +29; the offset is checked per event.)
2. **Given** a pathway with no diagram of its own, or a reaction, **When** its SBML is written, **Then** it has no layout, as today.
3. **Given** a node `tightNodes` resizes today (10 of 153 species boxes in R-HSA-69620), **When** the layout is written, **Then** it has the diagram's stored size, and this is a recorded difference.

---

### User Story 4 - A library the website can call (Priority: P2)

The website's Java library service (WebsiteAngular spec 010) depends on an artefact of this repository and asks it for one event's SBML.

**Why this priority**: it is what makes the website and the release run the same code (constitution, Principle III), but the website cannot switch until stories 2 and 3 are proven.

**Independent Test**: a small program that depends only on the library writes an event's SBML from a graph and a folder of diagram files, with no relational database and no `verifier-lib` on its classpath.

**Acceptance Scenarios**:

1. **Given** the library artefact, **When** its dependency tree is listed, **Then** it has neither the relational database's reader nor `verifier-lib`.
2. **Given** an event and its diagram files, **When** the library is called, **Then** the result is the same file the command line writes for that event.
3. **Given** two calls at once, **When** they run, **Then** neither changes the other's output: settings such as identifier URLs are per call, not static flags.

---

### User Story 5 - The bulk archives from the graph (Priority: P2)

The release's SBML step makes both archives from the graph and the diagram files, with no relational database.

**Why this priority**: it retires the relational database from the release's SBML; it comes after the model and the layout are proven.

**Independent Test**: a dry run for one species, then for all, is compared with the same release's published archives.

**Acceptance Scenarios**:

1. **Given** a release's graph and diagram files, **When** the command line runs for a species, **Then** it writes one `{stId}.sbml` per pathway, as today, with no relational options.
2. **Given** the dry run, **When** it is compared with the release's published archive, **Then** the file names are the same, every file passes the validity check, and a sample of files matches by the comparisons of stories 2 and 3.
3. **Given** the release's step, **When** it runs the new command line, **Then** it produces the same archives in the same output folder, and the verifier runs on them as today; its inputs change only as FR-008 says.

---

### Edge Cases

- **A reaction's own export names no parent pathway**, as today's relational output does: a reaction can have several parents, and any one would be arbitrary (decided 24 Oct 2026, #37).
- **Black-box events, disease and inferred events, and set-heavy events** are in the sample and must pass like the rest.
- **A species with no pathways** writes no files and does not fail the run.
- **An event drawn only on a parent's diagram** gets no layout, as today.
- **Diagram files of another release.** The caller states the diagram folder's release, and the layout refuses it when it differs from the graph's (spec 015, R3).
- **People's names.** Model history names its creators. Fixtures replace them, and comparisons hash them (constitution, Principle V).

## Requirements *(mandatory)*

### Functional Requirements

**Build and checks**

- **FR-001**: The branch MUST build without the published `verifier-lib` snapshot: the Docker build and CI build `verifier-lib` from `release-verifier-lib` commit `ac71fb3` and install it, then build with snapshot updates off (`-nsu`). This ends when `verifier-lib` is published as a non-snapshot version; the branch then depends on that version.
- **FR-002**: CI on the branch MUST run:
  - the unit tests;
  - Checkstyle with the repository's `checkstyle.xml` over the whole repository. Adding rules to it is a separate decision, each with its existing violations fixed or recorded;
  - libSBML's consistency check (python-libsbml, as in spec 015) over the converter's output for a fixture graph: a small Neo4j database started in CI, with graph-core's lazy loading working, and placeholder names in it.

  Each is seen failing once.

**The model**

- **FR-003**: The five defects (issue #14) MUST be fixed, each with a unit test that fails without its fix.
- **FR-004**: The model MUST match today's relational output of the same release for the 30-event sample, element by element (compartments, species, reactions, reactants, products and modifiers with role and stoichiometry, names, SBO terms, notes, annotations and identifier URLs), apart from the generation note's timestamp, element order (including the order of a complex's parts in its species' note, which today's files take from a HashMap) and the layout (US2), or each difference recorded with its reason. A change of graph-core version that alters the output is a recorded difference.
- **FR-005**: Identifiers MUST be written as the URLs today's relational files have (identifiers.org), in both the library and the command line, without a static flag.
- **FR-006**: Output MUST NOT depend on the machine's time zone or locale: the library fixes UTC and `en_US` for the generation note and for history dates itself.

**The layout**

- **FR-007**: The layout MUST be ported from `LayoutConverter`'s algorithm to read the release's diagram files (`diagram/{stId}.json`), from a folder the caller gives, never fetched over the network. It keeps today's ids, order, glyph sets, roles, backbone rules, hidden compartments, SBO override and root attributes, and still writes a layout only when the event's own diagram draws a reaction. Accepted differences, each recorded:
  - coordinates are the diagram's own;
  - node sizes are the diagram's stored sizes, with no font-based resizing;
  - a label's size is its node's box.

**The release's step**

- **FR-008**: The command line MUST run from the graph and a diagram folder alone, for listed ids, a species and `all`, keeping today's file names and the two archives. The release's step changes only in this: the relational options and credentials go, and the diagram folder (the diagram step's output) is passed in. The image keeps its jar paths (`target/sbml-exporter-exec.jar`, `target/sbml-exporter-verifier.jar`) whatever the module layout.

**The library**

- **FR-009**: The repository MUST be split into a library module (the graph converter and its layout) and a command-line module for the release. The library MUST NOT depend on the relational database's reader or on `verifier-lib`, and MUST be safe to call concurrently.
- **FR-010**: The library MUST be available to the website at a fixed version. Until it can be published, the website builds it from a pinned commit (spec 015, R10). Publishing it needs, and this spec records once settled with the maintainers: a non-snapshot parent pom (it is `reactome-parent:1.0.5-SNAPSHOT` today); the repository it goes to; versions that are never overwritten (today's `release_github.sh` recreates an existing tag); and a licence, which the repository does not have.

**Before `main`**

- **FR-011**: `graph-only` MUST NOT merge into `main` until: the comparisons of FR-004 and FR-007 pass; a full bulk dry run is compared with the same release's published archives, including the release verifier's size check (SC-004, SC-007); the maintainers agree; and the timing is agreed with the release's maintainers. Until the website's service switches too (spec 010, WebsiteAngular spec 015), the release's SBML and the website's would come from different converters; the merge either waits for that switch in the same release, or that release's difference is accepted and recorded (constitution, Principle III).
- **FR-012**: No request is made to Reactome's production services. The release's published archives are the one exception, fetched one at a time and kept out of the repository (constitution, Principle IV).

### Key Entities

- **Reference**: today's relational output of the same release: per event, the relational converter's file from a ContentService of that release that is not production; for the bulk run, the published archives. Kept outside both repositories.
- **Sample**: WebsiteAngular spec 015's 30 events (top-level pathways including Signal Transduction, sub-pathways with and without diagrams, reactions, a black box, disease, inferred and set-heavy events).
- **Diagram files**: the release's `diagram/{stId}.json`, which the website draws, given as a folder.
- **Recorded difference**: a difference from the reference or today's output that is accepted, with its reason, in this spec's `differences.md`, the one list for SBML (WebsiteAngular spec 015 links to it).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: On the branch, the build, tests, lint and validity check run on every pull request, and each has been seen failing once.
- **SC-002**: 100% of the sample's models match today's relational files, apart from what US2 sets aside, or carry a recorded difference.
- **SC-003**: Every layout in the sample matches today's by meaning, apart from recorded differences.
- **SC-004**: Every file of a full bulk dry run passes libSBML's consistency check with zero errors, and the file names match the published archives'.
- **SC-005**: Neither the library nor the command line needs the relational database.
- **SC-006**: The command-line module and the website's service (spec 010's pin) name the same library version or commit.
- **SC-007**: The full bulk run, from the graph, finishes within the release's time for this step; its time is recorded against today's.

## Assumptions

- **The maintainers' answers on issue #14** (one pull request or five for the fixes; the module split; the parent pom and graph-core version) are still open. This spec assumes five small pull requests and the split; it changes if they say otherwise.
- **graph-core's version** is the one WebsiteAngular spec 010 pins for its library service, agreed with BioPAX3Exporter's.
- **A private copy of the release's graph** is the source for runs, never a shared database.
- **`verifier-lib`'s fix is its maintainer's.** FR-001 only keeps this branch independent of it.
