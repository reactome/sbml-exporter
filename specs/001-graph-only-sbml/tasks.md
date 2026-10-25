# Tasks: SBML from the graph alone

**Input**: [spec.md](spec.md), [plan.md](plan.md), [research.md](research.md)

Each code or document task is one pull request into `graph-only`, reviewed before it merges, and ticks itself here with its number. Tasks that are a setting, a conversation or a merge (T010, T025, T029–T031) record their outcome here by pull request. "Seen failing" means the check is run once on a deliberately broken change and the failure is quoted in the pull request. `[P]` marks tasks that can run alongside others in their phase.

References kept outside the repository (the private graph copy, today's relational files, the published archives, the diagram files) are named by where to get them, never committed.

**Standing task:** whenever `main` changes, merge it into `graph-only` by pull request, so its checks run on the result.

## Phase 1: The checks (US1)

**Goal**: the build, tests, lint and validity check pass or fail on a change.
**Independent test**: a pull request into `graph-only` runs them, and each has been seen failing.

- [x] T001 [US1] (#18) Build without the broken snapshot (R1): in `Dockerfile`, a first stage clones `release-verifier-lib`, checks out `ARG VERIFIER_LIB_COMMIT=ac71fb3` and installs it (`-DskipTests -Dmaven.javadoc.skip=true -Dgpg.skip=true`); the build stage runs `mvn -nsu clean package`. The image's jar paths are unchanged. Show the verifier jar starting (its usage, no missing class) in the pull request.
- [x] T002 [US1] (#19) CI triggers in `.github/workflows/ci.yml`: pull requests `reopened` as well as `opened` and `synchronize`, and pushes to `graph-only` as well as `main`. The `docker-push` job stays on `main` only, so nothing from the branch reaches the release's `latest`.
- [x] T003 [P] [US1] (#27) Pin the snapshot dependencies by content (R2): a committed list of the sha1 of each resolved snapshot jar (`graph-core`, `reactome-base`) and of the parent's pom (`reactome-parent`), and a build step that fails when one differs. Seen failing on a changed entry.
- [x] T004 [US1] (#21) Unit tests run (R4): `src/test/java`, JUnit 5's engine and Mockito in `pom.xml`; the Docker build runs the tests (no `-DskipTests`). One placeholder test seen failing, then removed.
- [x] T005 [P] [US1] (#20) Lint on the repository's rules (R3): pin `maven-checkstyle-plugin` (3.6.0); configure it with `checkstyle.xml` and `includeTestSourceDirectory`; make the lint stage fail on Maven's status (`set -o pipefail` in the `Dockerfile`'s `CMD`). Seen failing on a 351-character line and on a Maven error.
- [x] T006 [US1] (#28) A minimal graph-only runner (R5), so CI can run the graph converter before T023: a `--graph-only` mode of `Main` (or a small runner class) that connects to the graph alone, reads `DBInfo`'s version, and writes `{stId}.sbml` for listed ids. T023 replaces it. (Replaced in #30: `Main` reads the graph alone, with no switch.)
- [x] T007 [US1] (#38) The fixture graph (R5): `ci/fixture/fixture.cypher`, a few events from the private copy of the reference's release (a pathway with a diagram, a reaction, a black box) with the relationships the converter walks lazily, full label sets, `schemaClass`, and a `DBInfo` node; the fixture pathway's diagram file beside it; how it was extracted in `ci/fixture/README.md`. Every person's name (`Person`, including literature authors), every `InstanceEdit` display name and every note is a placeholder; `ci/fixture/check-placeholders.sh` fails if one is not, and is seen failing.
- [x] T008 [US1] (#29) The validity check (R5): `ci/validate.py` runs python-libsbml 5.21.2's `checkConsistency`, units and modelling-practice categories off, over a folder, and fails on any error, allowing warnings. Seen failing on a file with a deliberate error.
- [x] T009 [US1] (#39) CI job `validity`: start Neo4j 4.4 Community with its database named `graph.db`, load the fixture, run T006's runner over its events, run `ci/validate.py` over the output, and assert the expected content (the placeholder creators, the notes, the compartments), so a lazy-loading failure fails it. Seen failing on T008's file and on a missing creator.
- [x] T010 [US1] (#40) Make lint, `docker-build` (which runs the tests) and `validity` the required checks on `graph-only`, once each has passed there. In the same step, update `CLAUDE.md` ("Checks that do not work yet", "Before pushing") and the constitution (Principle II, Quality Gates, Development Workflow) to say what now runs.

**Checkpoint**: every later task's pull request is judged by real checks.

## Phase 2: The model (US2)

**Goal**: the graph-only model is correct and matches today's relational output.
**Independent test**: the comparison over the 30-event sample (T016).

- [x] T011 [P] [US2] (#22) Fix defect 4, the reused `Participant`, in `DataFactory.getParticipantResults`, with a test (mocked `DatabaseObjectService`) that fails without the fix.
- [x] T012 [P] [US2] (#23) Fix defect 5, `ReactionBaseResult.build` not reading `catalysts`, with a test over an `InternalRecord` built from the driver's `Values` that fails without the fix.
- [x] T013 [US2] (#24) Fix defects 1–3 in `DataFactory.getReactionList` (catalysts, positive and negative regulators each from their own column), with a test for each (mocked `AdvancedDatabaseObjectService`; the static service set through `DataFactoryHelper` and reset before each test) that fails without its fix. Add tests for the two queries' row mapping.
- [x] T014 [P] [US2] (#25) Identifier URLs always on (R6, FR-005): replace the static `Helper.setUseIdentifierURL` flag; a test that two converters in parallel write identifiers.org URLs.
- [x] T015 [P] [US2] (#26) Time zone and locale (R6, FR-006): parse history dates in `UTC` and format the generation note with `UTC` and `Locale.US`, keeping the `hh` parse quirk; a test run under another default zone and locale writes the same output.
- [ ] T016 [US2] (`ci/compare.py` in #32; reaction literature from summations matched in #33; complexes' parts and identifiers in #34; `differences.md` in #35; a reaction's model without a parent pathway in #37; the cross-references asked in #36; the rest of the sample's differences to come) `ci/compare.py` (python-libsbml, as `ci/validate.py`): compares two folders of SBML element by element as US2 says (FR-004), the set-aside list written in the script, names hashed in its report; seen failing on a changed file. Run it over the 30-event sample: the fixed converter's files from the private copy of release 97, against the relational converter's files for release 97 from a ContentService that is not production, fetched one event at a time. Each difference in `differences.md` (created here; spec 015's `contracts/differences.md` links to it). Result in the pull request.
- [ ] T017 [US2] Pin graph-core to the version agreed in spec 015's T029 (with spec 010's service and BioPAX3Exporter), update T003's list, and rerun T016. A change in output is a recorded difference, not a reason to regenerate the reference.

**Checkpoint**: the model is proven.

## Phase 3: The layout (US3)

**Goal**: the layout from the diagram files, matching today's by meaning.
**Independent test**: the comparison over the sample's events with diagrams (T021).

- [X] T018 [US3] Fetch the diagram files a comparison needs from the release's download directory (`download.reactome.org/{release}/diagram/{stId}.json`; R8): one at a time, in sequence, kept outside the repository. A script `ci/fetch-diagrams.sh` takes a list of ids and a release, and skips files it already has.
  - Done in #43: `ci/fetch-diagrams.sh <release> <folder> <stId>...` (or ids on standard input), refusing a folder inside the repository. The download store answers 403 for a missing file (its XML `AccessDenied` error), as well as 404; both mean no file, and `DiagramFiles` refuses a pathway with a diagram whose file is missing. The review's fixes in #44: the folder records its release (`RELEASE`) and refuses another; any other 403 stops; timeouts; no ids, or a malformed one, is refused before anything is fetched; an interrupted fetch leaves no partial file.
- [X] T019 [US3] Read the diagram files (R8): decide between `reactome/diagram-reader`'s model and a small Jackson model (record it in research.md R8); read `diagram/{stId}.json` from a folder the caller gives, refusing it when the stated release differs from the graph's `DBInfo` version. Tests over the fixture's diagram file.
  - Done in #41: `layout.DiagramFiles` over `diagram-reader` 1.2.5 (R8); the folder's release is a constructor argument, checked against the graph's. The review's fixes in #42: a missing file for a pathway the graph says has a diagram is an error, and a file that is not the event's diagram is refused.
- [X] T020 [US3] Port `LayoutConverter`'s algorithm (`org.reactome.sbml.rel.LayoutConverter`, on `main`; removed from this branch in #30) to a new `layout` package over the diagram model (FR-007): ids, order, glyph sets, roles, backbone rules, RNA inset, hidden compartments, the SBO override and the root attributes; a layout only when the event's own diagram draws a reaction. Unit tests for each rule, each seen failing.
  - Done in #45: `layout.LayoutConverter`, and `SbmlConverter` takes the diagram files (null: no layout). Each kind of glyph is written in diagram-id order: the relational diagram's was the order components were added, which matched id order in R-HSA-112409; T021 checks it on every event. The relational protein, chemical and RNA drug classes extend the plain kinds, so those drugs keep their kind's SBO term; only a set holding a drug (`EntitySetDrug`) gets the drug's. The command line's diagram folder is T023's.
  - The review's fixes in #46: a "Cell" node takes the complex's SBO term (the relational class extends the complex's); a two-point backbone's hub is its new midpoint, as the relational `validatePosition` made it; notes count in the diagram's bounds; only an edge of class `Reaction` is a reaction; a diagram that cannot be laid out fails the conversion, naming it; a pathway whose `hasDiagram` is unset is an error; ids are lower-cased in the root locale.
- [ ] T021 [US3] Compare the layouts with today's files over the sample's events with diagrams, with `ci/compare.py` extended to the layout: glyph sets, ids, roles, order and targets exactly; coordinates against the diagram's; the resized nodes and label sizes as recorded differences. Re-run T016's comparison with the layout no longer set aside, since the layout changes SBO terms and the root. Result in the pull request.
  - Layout part done in #47: `ci/compare.py` compares the layout; today's six models laid out again from their diagram files differ only as differences.md records, and the layout is now valid (an id and dimensions; no branch to an undrawn part). The twelve sample pathways with a file but no layout today draw no reaction; three have no file. Left: the whole files from the graph, with the layout, once the command line takes the diagram folder (T023), on a private graph copy when the disk allows.
  - #50: the diagram step's disease copies left out, so R-HSA-2894858's layout is today's (decided 25 Oct 2026: match today; drawing them suggested in #48, listing them in the diagram files in #49); `ci/compare.py`'s test files prove each layout key; exact number formatting.

**Checkpoint**: the graph-only converter needs no relational database.

## Phase 4: The library and the command line (US4, US5)

**Goal**: a library for the website, and a graph-only command line for the release.

- [ ] T022 [US4] Split into `lib` (artifact `sbml-exporter-lib`) and `cli` (artifact `sbml-exporter`) modules, both at `3.0.0-SNAPSHOT` (R7):
  - `lib` holds the converter, data, layout, utilities and `url2identifier.txt`; no static service (the caller passes graph-core's services), a safe one-time load of the identifier map, a writer per call, no `System.exit`; no MySQL connector, no `reactome-base`, no `verifier-lib` (a test of its dependency list).
  - `cli` holds `Main`, the verifier, `ProgressBar`, `GraphNeo4jConfig` and `logback.xml`, and alone is repackaged; the `Dockerfile` copies its jars to the same image paths.
  - In the `validity` job: two conversions in parallel give the same files as in sequence (US4, acceptance 3), and the library's file for an event is the command line's (acceptance 2).
  - `lib` is also built and tested on JDK 17.
- [ ] T023 [US5] The graph-only command line (FR-008): drop the relational options; add the diagram folder and its release; listed ids, a species and `all` use the graph converter with its layout, keeping today's file names; a species with no pathways writes nothing and does not fail. Remove the relational converter from the build (ContentService pins the released 2.2.7, so it is unaffected).
  - Done ahead of the layout in #30, by decision (24 Oct 2026: nothing on this branch reads the relational database, and there is no switch): the relational options, the `rel` package, the MySQL connector, `reactome-base` and JDOM are gone, and listed ids, a species and `all` use the graph converter. Until T020, its output has no layout. Left for this task: the diagram folder and its release, and a species with no pathways.
  - The Jenkinsfile's relational credentials and options went in the same pull request, so the branch's step matches its command line (T024 still checks it by a local run).
- [ ] T024 [US5] The release step's files: `Jenkinsfile` without the relational credentials and options, passing the diagram step's output folder through a Jenkins variable; the archive names, the output folder and the verifier run as today. Checked by a local run of the image over the fixture graph.
- [ ] T025 [US4] Make the library available to the website at a fixed version (FR-010): until it can be published, record here the commit spec 010's service builds it from; list what publishing needs (a non-snapshot parent, the repository, versions never overwritten, a licence) on issue #14 for the maintainers.

## Phase 5: The bulk run (US5)

- [ ] T026 [US5] A dry run for one species (human) from the private copy, compared with the same release's published `homo_sapiens.3.1.sbml.tgz` (fetched once, as in spec 015's T033, kept outside the repository): the same file names, every file valid, a sample of files compared as in T016 and T021, and the release verifier's size check (no file smaller by 5% or more) run against the published archive's entries. Result in the pull request.
- [ ] T027 [US5] The full dry run (`all`), compared with `all_species.3.1.sbml.tgz` in the same way, including the size check; record its time relative to today's step (SC-007).
- [ ] T028 [P] Record the results against the success criteria (SC-001 to SC-007) in this file, SC-006 by naming the library version or commit in both the command line and spec 010's service.

## Phase 6: Into `main`

- [ ] T029 When `verifier-lib` is published as a non-snapshot version, depend on it and drop T001's build stage (FR-001).
- [ ] T030 Ask the maintainers on issue #14 to review the branch and agree the merge. Agree with the release's maintainers its timing, a one-off size baseline for the verifier if T026 or T027 found smaller files, and whether the website's switch (spec 010 and WebsiteAngular spec 015) lands in the same release or a release where the two differ is accepted and recorded (FR-011).
- [ ] T031 Merge `graph-only` into `main` by pull request, after `main` has been merged into `graph-only` and its checks pass.

## Dependencies

- Phase 1 comes first; T001 before every other task, since nothing builds without it. T006 and T007 before T009.
- In Phase 2, T011–T015 can run in any order; T016 needs spec 015's T030 and the private copy; T017 needs spec 015's T029.
- Phase 3 needs T016, since the layout changes the model; T018 before T021.
- Phase 4 needs Phases 2 and 3. T022 before T023; T023 before T024.
- Phase 5 needs Phase 4, and T018's script for the species' diagrams. Phase 6 needs everything (T029 whenever `verifier-lib` is released).
