# Research: SBML from the graph alone

Measured on 24 Oct 2026 against `graph-only` at `997113ac` (the same code as `main` at `10cc0756`), unless a section says otherwise. The measurements behind the model and the layout are WebsiteAngular spec 015's (research R1, R3 and R9); this file records only what this repository's plan adds.

## R1. Building without the broken `verifier-lib` snapshot

**Measured.**

- The Central snapshot repository's `verifier-lib:1.0-SNAPSHOT` metadata lists only `1.0-20261019.201057-1`. Its jar holds `FileUtils`, `TooSmallFile`, `CountUtils`, `Results` and `DBParams`: no `DefaultVerifier`, no `Verifier`. The December build, `1.0-20251229.214134-8`, answers 404.
- The EBI snapshot repository's metadata names `1.0-20250128.044026-12`, whose jar cannot be downloaded. That is the "could not be resolved" failure other release repositories show.
- `release-verifier-lib` commit `ac71fb3` (its `main`, 29 Dec 2025) has both classes. Built and installed into a scratch Maven repository (`mvn install -DskipTests -Dmaven.javadoc.skip=true -Dgpg.skip=true`; the javadoc and signing plugins need a full JDK and a key), it lets `mvn -nsu clean package` build all three jars. The verifier jar starts and asks for `--output` and `--releaseNumber`.
- The build image (`maven:3.9.6-eclipse-temurin-11-focal`) has git 2.25.1, so the Docker build can clone it.

**Decision.** The Docker build's first stage clones `release-verifier-lib`, checks out `ac71fb3`, installs it, and the build runs with `-nsu`. The commit is an `ARG`, so it changes in one place.

**Alternatives.** Pinning a timestamped snapshot: none that works is still served. Vendoring the classes: copies its maintainer's code into this repository. Dropping the verifier from the build: the release step runs it.

## R2. The other snapshot dependencies

**Measured** (`mvn dependency:tree`): `graph-core 3.0.0-SNAPSHOT` (resolved `3.0.0-20250925.120535-19`), `reactome-base 2.2.4-SNAPSHOT` (`2.2.4-20260303.104120-2`), and the parent `reactome-parent 1.0.5-SNAPSHOT` (`1.0.5-20260309.163435-4`). Each can change under the build exactly as `verifier-lib` did. Spring Data Neo4j is 6.3.6, through graph-core; the Neo4j driver's 4.4.9 is set by the parent's dependency management. `mvn dependency:list` prints `3.0.0-SNAPSHOT`, not the timestamp, and omits the parent; `-nsu` on a fresh CI runner pins nothing, since it only stops updates to what is already in the local repository.

**Decision.** Record the resolved build of each at the start, by the sha1 of the jar (and of the parent's pom) that Maven resolved, and fail the build when one differs (T003). graph-core is pinned to the release version WebsiteAngular spec 010 pins for its library service, agreed with BioPAX3Exporter (spec 015 T029); reactome-base and the parent follow the maintainers' answer on issue #14. Until then a build that resolves a different snapshot from the recorded one is treated as a broken build, not as a code change. The recorded list is `ci/snapshots.sha1` (24 Oct 2026: graph-core, reactome-base, and the `reactome-parent` and `reactome-bom` poms), checked by `ci/snapshots.sh check` in the Docker build.

## R3. Lint

**Measured.** The lint stage runs `mvn -B -q checkstyle:check` with no `configLocation`, so the plugin uses its default (Sun) rules; on 24 Oct it failed on #15 with violations throughout `rel/`. The plugin's version is not pinned and resolves to the latest (3.6.0, with Checkstyle 9.3). The repository's `checkstyle.xml` has one module, `LineLength` with `max` 350, directly under `Checker`, which needs Checkstyle 8.24 or later; with it, the code has no violations. The stage's exit status is `grep`'s, not Maven's, so a Maven failure that prints no `.java` line (an unresolvable parent, say) passes. Test sources are skipped by default.

**Decision.** The plugin is pinned (3.6.0) and configured with `checkstyle.xml` and `includeTestSourceDirectory`; the stage fails on Maven's own status (`pipefail`). It is shown failing on a 351-character line and on a Maven error. Checkstyle has no diff mode, and the module split moves every file, so "changed code only" is not workable; rules are added to `checkstyle.xml` one at a time, each with its existing violations fixed in the same pull request.

## R4. Unit tests

**Measured.** There is no `src/test`. `junit-jupiter-api` is declared, with no engine and no Surefire configuration. The defects sit in code that can be tested without a database:

- `DataFactory.getParticipantResults(DatabaseObjectService, List<ParticipantResult>)`: the reused `Participant`, with a mocked service.
- `DataFactory.getReactionList(eventStId, ads)`: the three regulator columns, with a mocked `AdvancedDatabaseObjectService` and the static service set through the constructor.
- `ReactionBaseResult.build(Record)`: the unread `catalysts`, with records built from the Neo4j driver's `Values`.

Surefire 2.22.2 (from the parent) already runs JUnit 5; only the engine is missing. `new DataFactoryHelper(mockService)` sets `DataFactory`'s static service, so tests reset it before each test. Records for `ReactionBaseResult.build` are built as `new InternalRecord(keys, values)` from the driver's `Values`.

**Decision.** JUnit 5's engine and Mockito (4.5.1, from the parent). Each defect gets a test that fails before its fix.

## R5. The validity check in CI

**Decision.**

- **The fixture graph** is a Neo4j 4.4 Community container in CI (the driver and Spring Data Neo4j above are 4.4-era), loaded from a Cypher script of a few events taken from the private copy of the reference's release: one pathway with a diagram, one reaction, one black box.
  - **Its database is named `graph.db`**: graph-core's `ReactomeGraphCore.initialise(host, user, password, config)` hard-codes that name. It holds a `DBInfo` node, whose version the converter writes.
  - **It keeps every node's full label set and its `schemaClass` property**, which Spring Data Neo4j maps by.
  - **People's names are placeholders** wherever they appear: `Person` nodes (creators, and the authors of `LiteratureReference`s), `InstanceEdit` display names (which carry a surname) and notes. A check fails if a name from the source graph survives.
  - **How it is extracted** is recorded with it (APOC's export if the private copy has APOC, otherwise a small driver script).
- **graph-core's lazy loading** must work against it, since the converter walks summations, creators, literature and compartments lazily. Valid SBML does not prove that (with lazy loading off the getters return nothing and the file is still valid), so the job also asserts the expected content: the placeholder creators, the notes and the compartments.
- **libSBML**: python-libsbml 5.21.2 runs `checkConsistency` with the units and modelling-practice categories off, as spec 015's R5 does, failing on any error; warnings are allowed.
- It is shown failing once on a file with a deliberate error.
- **Something must run the graph converter** before the command line is made graph-only (T023): today `Main` always builds a `MySQLAdaptor` and requires the relational options. A minimal graph-only runner comes first (T006).

## R6. Identifier URLs, time zone and locale

**Measured.** `Helper.setUseIdentifierURL` is a static flag, false by default; `SbmlConverterForRel` sets it to true, `SbmlConverter` never does. The generation note uses the default date format and history dates are parsed in the default zone, so the output depends on the machine (spec 015's harness runs with `-Duser.timezone=UTC` and `en_US`).

**Decision.** Identifier URLs are always on, with the flag replaced by a constant or a per-converter setting defaulting to on. History dates are parsed in an explicit `UTC` zone and the generation note is formatted with `UTC` and `Locale.US`, so the library's output does not depend on where it runs. JSBML already writes the dates in GMT. The `hh` (12-hour) parse quirk spec 015 R1 lists is kept, since the reference has it.

## R7. Library and command line

**Measured.** Shared and static state that a library called concurrently must not have: `DataFactory`'s static service (set by the Spring component `DataFactoryHelper`); `Helper`'s lazily loaded `url2identifier` map (an unsynchronised first load) and its identifier-URL flag; one static `TidySBMLWriter` in `Utils`. `Utils.outputCheck` calls `System.exit`, and `ProgressBar` is a command-line concern. `GraphNeo4jConfig` scans all of `org.reactome.server`, which would pick up the host application's beans.

**Decision.** A Maven multi-module build:

- **`sbml-exporter-lib`**: `converter`, `data`, `util` and the new `layout`; graph-core and JSBML; no MySQL connector, no `reactome-base` (the relational reader), no `verifier-lib`. Only `Main` imports anything relational today.
- **`sbml-exporter`** (the command line): `Main`, the verifier class, `ProgressBar`, `GraphNeo4jConfig`, `logback.xml` and, until the command line no longer needs it, the relational converter. It depends on the library and alone is repackaged by Spring Boot.
- The library keeps `url2identifier.txt`, loads it once safely, holds no static service (the caller passes graph-core's services), creates its writer per call, and never exits the process.
- **Versions:** the command line's coordinate `org.reactome.server.tools:sbml-exporter` is what ContentService pins at the released 2.2.7, so the branch moves both modules to `3.0.0-SNAPSHOT`; the website consumes `sbml-exporter-lib`.
- The library is also built and tested on JDK 17, the website's service's runtime.

The image keeps `target/sbml-exporter-exec.jar` and `target/sbml-exporter-verifier.jar` by copying from the command-line module's `target/`. ContentService pins the released 2.2.7, so the split does not affect the website until spec 010's service uses the library.

## R8. Reading the diagram files

**Measured.** The diagram files are made by the release's diagram step (`reactome/diagram-converter`), which reads the relational database as well as the graph, so they cannot be regenerated from a graph copy. The JSON has no release field. Beta does not serve the diagram files locally (spec 015 R3), The release's download directory publishes them per pathway (`download.reactome.org/{release}/diagram/{stId}.json`, with no listing).

**Decision.**

- The layout reads `{stId}.json` from a folder the caller gives, with `reactome/diagram-reader`'s model (decided in T019, #41), not a model of our own. It is Reactome's own reading of these files, used by the diagram exporter; version 1.2.5 is a release, from the same repository as graph-core, and it brings only Jackson (databind, core and the mrbean module; below). It has everything the algorithm reads: each node's id, Reactome id, render class, box (`prop`) and label position; each reaction's id, Reactome id, position, backbone (`segments`) and, per input, output, catalyst, activator and inhibitor, the branch's points; each compartment's box and label position.
- **What the files lack:** a label's size. The relational layout's text glyph boxes are the node's text bounds; the files give the label's position only. T020 decides the box and T021 records the difference.
- An event with no file of its own has no layout (a reaction, or a pathway drawn only on a parent's diagram). A pathway the graph says has a diagram (`hasDiagram`) but whose file is missing is an error, since a missed fetch would otherwise pass as a pathway without one. A file that is not that event's diagram (another stable id, or no nodes or edges) is refused too: diagram-reader ignores unknown properties, so any JSON object reads.
- **For T020:** the files give a reaction's backbone as from→to segments, which T020 chains into points. The glyph ids number each role's branches in order (`_substrate_0`, ...), so the order of the files' lists must match the relational one; T021 checks it. That an event has a file exactly when the relational database had a diagram representing it is inferred, and T021 checks it too.
- **Jackson:** core 2.13.1 and databind 2.13.3 (pinned by reactome-parent) and mrbean 2.13.4 (from Spring Boot's BOM), one minor version. A service that takes the library brings its own Jackson, and its mrbean must match its databind's minor version.
- **The release check:** the caller states the folder's release, and the library refuses it if it differs from the graph's `DBInfo` version. The command line takes it as an option.
- **The source for comparisons and dry runs** is the release's download directory (decided 24 Oct 2026): only the files a run needs, fetched one at a time and in sequence, kept outside the repository. A species or full dry run needs one per pathway with a diagram.
- **CI** uses the fixture pathway's own diagram file, committed beside the fixture.
