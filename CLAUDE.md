# Working in this repository

Day-to-day guidance for anyone, human or agent, changing sbml-exporter. The
principles are in [`.specify/memory/constitution.md`](.specify/memory/constitution.md);
where this file and the constitution disagree, the constitution wins.

## What it is

sbml-exporter writes Reactome's SBML (Level 3 Version 1, with the layout
package). It is Java, built with Maven, writes with JSBML, and reads through
graph-core. It holds two converters:

- **`SbmlConverterForRel`** (package `org.reactome.sbml.rel`) is the one used
  today, by the command line and inside ContentService. It reads its reactions
  and the layout from the relational release database. It writes a layout only
  when the event's own diagram draws a reaction.
- **`SbmlConverter`** (packages `converter` and `data`) reads only the graph:
  - two Cypher queries in `DataFactory`;
  - graph-core's domain objects, walked lazily (summations, creators, literature,
    compartments). That needs graph-core's lazy-fetch aspect, which
    `GraphNeo4jConfig` enables. Without it the getters return nothing.

  It is to become the one implementation, for both:
  - **the release's archives:** `all_species.3.1.sbml.tgz` and
    `homo_sapiens.3.1.sbml.tgz`, made by the command line;
  - **the website's per-event downloads:** made through a library module that
    does not exist yet.

The plan is in [issue #14](https://github.com/reactome/sbml-exporter/issues/14)
and in [spec 001](specs/001-graph-only-sbml/spec.md). Its work is on the
`graph-only` branch, which merges into `main` once it is proven; until then
`main` is what the release uses.

## Specs first: use Spec Kit

Feature-sized work goes through Spec Kit before any code, and the specs live in
`specs/`:

- **Feature-sized** means a new capability, a change to what is written, or
  anything that will take more than one pull request.
- **The sequence** is `/speckit-specify` → `/speckit-plan` → `/speckit-tasks`, then
  implementation task by task.
- **Keep `tasks.md` current.** The pull request that completes a task ticks it and
  adds its number, in the same pull request.
- **A single-PR fix** needs no new spec, but its description names the spec it
  belongs to, if any.
- **Work that also touches the website** (`reactome/WebsiteAngular`) keeps its
  website part in that repository's specs. Its spec 015 is the website's half of
  this work, and holds the measurements and the reference harness this work
  started from.

## Checks that do not work yet

Don't trust these until the first spec has fixed them (constitution, Principle II):

- **The build is broken on `main`.** `verifier-lib:1.0-SNAPSHOT` now resolves to a
  version without the classes `SBMLExportVerifier` uses (the snapshot published
  on 19 Oct 2026). Fixing the published library is its maintainer's. Spec 001's
  branch builds `verifier-lib` from a fixed commit of `release-verifier-lib`
  instead; locally, install that commit first
  (`mvn install -DskipTests -Dmaven.javadoc.skip=true -Dgpg.skip=true`), then
  build with `mvn -nsu` so Maven does not fetch the broken snapshot.
- **There are no tests**, so nothing tests the code yet.
- **The lint stage** runs Checkstyle's default rules, not the repository's
  `checkstyle.xml`, and reports over a thousand existing violations. New code
  should add none.

## Proving what is written

- **The model** is compared element by element with today's relational output
  of the same release (constitution, Principle I): the timestamp, element order
  and, until the layout is ported, the layout set aside; every other difference
  recorded in `specs/001-graph-only-sbml/differences.md` with its reason.
  `ci/compare.py` does it (T016).
- **The layout** is compared by meaning with today's: the same glyphs, ids, roles
  and order, and the diagram's own coordinates.
- **Runs need a fixed time zone and locale** (UTC, `en_US`). The generation note
  uses the default date format, and history dates are parsed in the default
  zone. Write the output as UTF-8.
- **Runs use a private copy of the release's graph**, never a shared database.
- **The release's published archives** come from the release's download directory
  (`download.reactome.org/{release}/`). Fetch one at a time, only when a
  comparison needs it, and keep them out of the repository, since they hold
  people's names. They come from the relational converter, so compare them by
  meaning. No other Reactome production service is used.
- **libSBML's consistency check** must report zero errors. Warnings are allowed.

## This repository is public

Nothing internal goes in it, in code, comments, commit messages or pull request
descriptions:

- no host names or server paths;
- no credentials;
- no internal release-process detail;
- no people's names from exported data.

## Before pushing

- The build compiles and the tests run and pass: see "Checks that do not work
  yet". Every fix has a test seen failing first.
- New code adds no Checkstyle violations.
- No personal names in fixtures, logs or test output.
