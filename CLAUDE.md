# Working in this repository

Day-to-day guidance for anyone, human or agent, changing sbml-exporter. The
principles are in [`.specify/memory/constitution.md`](.specify/memory/constitution.md);
where this file and the constitution disagree, the constitution wins.

## What it is

sbml-exporter writes Reactome's SBML (Level 3 Version 1, with the layout
package). It is Java, built with Maven, writes with JSBML, and reads through
graph-core. On this branch (`graph-only`) it has one converter; `main` still has
the relational `SbmlConverterForRel`, which ContentService uses today, until the
branch merges (T031).

- **`SbmlConverter`** (packages `converter` and `data`, in the `lib` module) reads
  only the graph:
  - two Cypher queries in `DataFactory`;
  - graph-core's domain objects, walked lazily (summations, creators, literature,
    compartments). That needs graph-core's lazy-fetch aspect, which
    `GraphNeo4jConfig` enables. Without it the getters return nothing.

  It is to become the one implementation, for both:
  - **the release's archives:** `all_species.3.1.sbml.tgz` and
    `homo_sapiens.3.1.sbml.tgz`, made by the command line;
  - **the website's per-event downloads:** made through the `lib` module, which
    WebsiteAngular's java-library service runs.

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

## The checks

On `graph-only`, CI runs and requires five checks, each seen failing once
(constitution, Principle II; `format` in run 40744132003 and each of `scripts`' three tools in run
40758551040 (Ruff's format step in run 40760026528), on throwaway branches):

- **`docker-build`**: the build and the unit tests. `verifier-lib` is the released
  1.0.0 from Maven Central (spec 001, T029); before it, the `Dockerfile` built a
  fixed commit, because the 1.0-SNAPSHOT of 19 Oct 2026 lacked the classes
  `SBMLExportVerifier` uses. The build also fails if a snapshot dependency changes
  under it (`ci/snapshots.sh`).
- **`lint`**: Checkstyle with the repository's `checkstyle.xml`.
- **`validity`**: the converter over the fixture graph (`ci/fixture`), its output
  valid by libSBML (`ci/validate.py`) and equal to `ci/fixture/expected`
  (`ci/compare.py`).

The fourth, **`format`**, checks Markdown, YAML and JSON as Prettier
3.9.10 writes them (`.prettierrc`, `.prettierignore`; WebsiteAngular's settings). Fix with
`npx prettier@3.9.10 --write .`, which a Spec Kit run needs afterwards: Spec Kit writes
`[X]` and `*emphasis*`, which Prettier writes `[x]` and `_emphasis_`.

The fifth, **`scripts`**, checks the scripts and the workflow, each tool pinned: ShellCheck on
the shell scripts (Spec Kit's own aside), Ruff's lint and format on `ci/`'s Python
(`ruff.toml`; fix with `pipx run ruff==0.16.7 format ci`), and actionlint on the workflow.
Each step runs even when one before it fails, so one push shows every problem.

Dependabot opens weekly updates to `graph-only` for Maven, the workflow's actions and the
`Dockerfile`'s images (`.github/dependabot.yml`, read from `main`; its `target-branch` comes
out when `graph-only` merges, T031). reactome-parent, and the graph-core and reactome-bom
versions it brings, are snapshots pinned by checksum (`ci/snapshots.sh`) and move only by
hand, as do the pinned tools above, the build image (`maven`) and the Java version of the
runtime image.

`main` is unchanged until `graph-only` merges: its build is broken, it has no
tests, and its lint uses Checkstyle's defaults.

## Proving what is written

- **The model** is compared element by element with today's relational output
  of the same release (constitution, Principle I): the timestamp, element order
  and the layout (compared on its own, below) set aside; every other difference
  recorded in `specs/001-graph-only-sbml/differences.md` with its reason.
  `ci/compare.py` does it (T016).
- **The layout** is compared by meaning with today's: the same glyphs, ids, roles
  and order, and the diagram's own coordinates. Spec 002 replaces that: the layout
  is to be compared with the diagram files as the website draws them (constitution,
  Principle I).
- **Runs keep a fixed locale and write UTF-8.** The generation note and history
  dates are made in UTC by the converter itself (FR-006); the note's short
  format is `en_US`'s, which JDK 20 and later spell with a narrow space before
  AM/PM (JDK 11 and 17 do not), and the comparison sets the timestamp aside.
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

- The build compiles and the tests run and pass (`mvn -nsu test`, see "The
  checks"). Every fix has a test seen failing first.
- A change to what is written: run `ci/compare.py` against today's files, and put
  the result in the pull request. If the fixture's output changes, regenerate
  `ci/fixture/expected` and say why.
- New code adds no Checkstyle violations.
- No personal names in fixtures, logs or test output.
