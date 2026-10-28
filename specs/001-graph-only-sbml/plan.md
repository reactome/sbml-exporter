# Implementation Plan: SBML from the graph alone

**Branch**: `graph-only` | **Date**: 2026-10-24 | **Spec**: [spec.md](spec.md)

## Summary

Make the graph-only `SbmlConverter` the one SBML implementation. First make the build and its checks real on the branch (R1–R5); then fix the five defects with tests and prove the model against today's relational output of the same release; then port the layout to the diagram files and prove it against today's; then split the library from the command line and make the command line graph-only; then a bulk dry run against the published archives. `graph-only` merges into `main` only after all of that and the maintainers' agreement (FR-011).

## Technical Context

**Language/Version**: Java 11 (the build image and the release's runtime); the library must also run on spec 010's JDK 17 service.
**Primary Dependencies**: graph-core (Spring Data Neo4j 6.3.6; Neo4j driver 4.4.9 from the parent), JSBML 1.5, Jackson for the diagram files; JUnit 5 and Mockito for tests. `verifier-lib` only in the command line.
**Storage**: the release's Neo4j graph (a private copy for runs; a small fixture container in CI); the release's diagram files, as a folder: the release's own diagram output, obtained as one archive (R8, open).
**Testing**: unit tests (each defect, the row mapping, the layout's rules); the validity check (python-libsbml 5.21.2) over a fixture graph's output in CI; the comparisons with today's relational output (model and layout, `ci/compare.py`), run outside CI against the private copy.
**Target Platform**: the release's SBML step (Docker image, as today); spec 010's Java library service (the library).
**Project Type**: a library and a command line, in one Maven multi-module repository.
**Performance Goals**: the full bulk run within the release's time for this step (SC-007); one event as fast as today's or faster.
**Constraints**: the model matching today's relational output element by element; layout parity by meaning; no relational database; no production requests (the published archives excepted); no people's names in the repository.
**Scale/Scope**: five fixes; `LayoutConverter` (about 500 lines) ported; a module split; a graph-only command line; CI for tests, lint and validity.

## Constitution Check

- **I. Proven against a reference.** The model against today's relational output of the same release (never made by the code under test); the layout against today's by meaning; the bulk run against the published archives by meaning. **Pass.**
- **II. Verify the instrument.** Every check (build, tests, lint, validity) is seen failing once before it is relied on (T001–T009). The snapshot dependencies are pinned by content, so a changed snapshot fails the build (R2). The validity job asserts content as well as validity, since valid SBML does not show that lazy loading worked (R5). **Pass.**
- **III. One implementation.** The library module serves the release and the website (R7). **Pass.**
- **IV. No production requests.** The private graph copy; the published archives fetched one at a time and kept out of the repository. **Pass.**
- **V. No people's names.** The fixture graph's names are placeholders and checked (R5); comparisons hash names. **Pass.**

**Re-check after design: pass.**

## Project Structure

### Documentation

```text
specs/001-graph-only-sbml/
├── spec.md, plan.md, research.md (R1–R8), tasks.md
├── differences.md          # recorded differences, created by the first comparison
└── checklists/requirements.md
```

### Source code (after the split, T020)

```text
pom.xml                               # parent: modules lib, cli
lib/   (artifact sbml-exporter-lib)
  src/main/java/org/reactome/server/tools/sbml/{converter,data,util,layout}/
  src/test/java/...                   # defect tests, row mapping, layout rules
cli/   (artifact sbml-exporter)
  src/main/java/org/reactome/server/tools/sbml/Main.java
  src/main/java/org/reactome/verifier/SBMLExportVerifier.java
ci/fixture/                           # the fixture graph's Cypher, placeholder names
ci/validate.py                        # libSBML consistency over a folder
Dockerfile, Jenkinsfile, .github/workflows/ci.yml
```

Before the split, tests sit in `src/test/java` and move with their code. `ci/fixture/` also holds the fixture pathway's diagram file and a placeholder check.

## Order of work

1. **The checks** (T001–T010): build without the snapshot, pin the other snapshots by content, tests running, lint on `checkstyle.xml`, a minimal graph-only runner, the validity check over a fixture graph, each seen failing; then make them required.
2. **The model** (T011–T017): the five fixes, identifier URLs and the time zone, the comparison over the sample, and graph-core's pin.
3. **The layout** (T018–T021): the release's diagram files, the reader, the port, the comparison.
4. **The library and the command line** (T022–T025): the split, a graph-only command line, the release step's changes, the library's version for the website.
5. **The bulk run** (T026–T028): one species, then all, against the published archives, with the verifier's size check; the time.
6. **Into `main`** (T029–T031): `verifier-lib`'s release, the maintainers' agreement and timing, the merge.

## Complexity Tracking

| Choice                                | Why                                                                      | Simpler alternative rejected because                                                |
| ------------------------------------- | ------------------------------------------------------------------------ | ----------------------------------------------------------------------------------- |
| Building `verifier-lib` from a commit | `main`'s build is broken by a published snapshot                         | waiting blocks all of this work on another repository                               |
| A fixture Neo4j in CI                 | the converter walks graph-core's lazy relationships, which a mock cannot | mocking only the two queries would not exercise the lazy walk the output depends on |
