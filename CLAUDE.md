# Working in this repository

Day-to-day guidance for anyone, human or agent, changing sbml-exporter. The
principles are in [`.specify/memory/constitution.md`](.specify/memory/constitution.md);
where this file and the constitution disagree, the constitution wins.

## What it is

sbml-exporter writes Reactome's SBML (Level 3 Version 1, with the layout package)
from the Neo4j graph, through graph-core. It is Java, built with Maven, and
writes with JSBML. It holds two converters:

- **`SbmlConverterForRel`** (package `org.reactome.sbml.rel`) is used today. It
  needs the relational release database, for the reactions and the layout.
- **`SbmlConverter`** (packages `converter` and `data`) reads only the graph,
  through two Cypher queries in `DataFactory`. It is to become the one
  implementation, for both:
  - **the release's archives:** `all_species.3.1.sbml.tgz` and
    `homo_sapiens.3.1.sbml.tgz`, made by the command line;
  - **the website's per-event downloads:** made through the library.

The plan is in [issue #14](https://github.com/reactome/sbml-exporter/issues/14)
and in this repository's specs.

## Specs first: use Spec Kit

Feature-sized work goes through Spec Kit before any code, and the specs live in
`specs/`:

- **Feature-sized** means a new capability, a change to what is written, or
  anything that will take more than one pull request.
- **The sequence** is `/speckit-specify` → `/speckit-plan` → `/speckit-tasks`, then
  implementation task by task.
- **Keep `tasks.md` current.** The pull request that completes a task ticks it and
  adds its number, in the same pull request.
- **Work that also touches the website** (`reactome/WebsiteAngular`) keeps its
  website part in that repository's specs. Its spec 015 is the website's half of
  this work, and holds the measurements and the reference harness this work
  started from.

## Proving what is written

- **The model** is compared byte for byte, the generation note's timestamp set
  aside, with the graph-only converter's output once its five defects are fixed.
  Issue #14 lists the defects.
- **The layout** is compared by meaning with today's: the same glyphs, ids, roles
  and order, and the diagram's own coordinates.
- **Runs need a fixed environment** (UTC, `en_US`, UTF-8), because the generation
  note and the history dates depend on it.
- **Runs use a private copy of the release's graph**, never a shared database, and
  never Reactome's production services. The release's published archives, from
  its download directory, are the bulk reference. Fetch one at a time, and keep
  them out of the repository: they hold people's names.
- **libSBML's consistency check** must report zero errors. Warnings are allowed.

## Before pushing

- `mvn test` passes, and every fix has a test seen failing first.
- checkstyle passes, as CI's lint stage runs it.
- No personal names in fixtures, logs or test output.
