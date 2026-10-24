# sbml-exporter Constitution

These principles are not aspirations. Each one is here because breaking it has
already cost Reactome's SBML something specific. The cost is named so the rule
can be argued with rather than merely obeyed.

## Core Principles

### I. Proven against a reference, and the reference itself checked

The graph-only `SbmlConverter` was written in 2021, then left uncalled: the
command line and the website both still use the relational converter. When its
code was read and then run against release 97's graph (23–24 Oct 2026,
reactome/WebsiteAngular spec 015, research R1 and R9), it had five data defects:

- catalysts were written from the positive regulators;
- negative regulators were written from the positive regulators;
- positive regulators were never written;
- one `Participant` was reused, so every reaction kept a single reactant and a
  single product;
- catalysts were never read from the query.

Nothing in this repository compared its output with anything, so none of them
showed.

**So:** a change to what is written is compared with a reference, and the
reference is checked before it is trusted.

- **The model** is compared byte for byte, the generation note's timestamp set
  aside. The reference is the graph-only converter with those defects fixed, run
  by the reference harness in WebsiteAngular spec 015 (`reference-harness.md`):
  - with `Helper.setUseIdentifierURL(true)`, as ContentService sets it;
  - with the outputs kept outside both repositories, because they hold people's
    names.

  Once the fixes land here, the reference outputs stay as they were generated.
  They are not regenerated from the code under test.
- **The layout** is compared by meaning with today's relational layout: the same
  glyphs, ids, roles and order (spec 015 R3).
- **The release's published archives** come from the relational converter, so
  they can only be compared by meaning: species and reaction order and some
  annotations differ (R1). They are the reference for the bulk run's file set,
  not for bytes.

### II. Verify the instrument

None of this repository's checks can pass or fail on a change today:

- **The build is broken on `main`.** It depends on `verifier-lib:1.0-SNAPSHOT`,
  which now resolves to a version without the classes `SBMLExportVerifier` uses
  (the snapshot published on 19 Oct 2026; the earlier one now answers 404).
  `mvn test` and the Docker build fail at compile, before any test.
- **There are no tests.** The defects above survived for years.
- **The lint stage runs `mvn checkstyle:check` with no configuration**, so it
  uses Checkstyle's default rules. The repository's own `checkstyle.xml` is not
  used. It reports over a thousand existing violations and has failed on earlier
  pull requests. It is not a required check.

**So:** name what a check actually reads, and see it fail once before relying on
it. A gate that cannot fail, or cannot pass, is not a gate.

### III. One implementation, for the release and the website

The release's archives (`all_species.3.1.sbml.tgz` and
`homo_sapiens.3.1.sbml.tgz`) and the website's per-event SBML share the relational
converter. That converter needs the relational database for its reactions and
for the layout. The graph-only converter, which would remove that need, was
never wired in.

**So:** this repository is to have two parts:

- a library module: graph in, SBML out, with the layout drawn from the diagram
  files the website shows;
- a thin command line for the release, built on that library.

The website is to depend on the published library, not on a copy. Neither is to
need the relational database. Today there is one module, and the command line
requires the relational options.

### IV. Production services are not test fixtures

Reactome's production services (the website, its APIs and its databases) are
not used for development, comparison or verification: a burst of requests can
load the live site.

**The reference for the bulk output is the release's own published archives**,
static files in the release's download directory
(`download.reactome.org/{release}/`). Fetching them is allowed, one file at a
time, only when a comparison needs them, and they are kept out of the
repository. **A private copy of the release's graph database** is the source for
runs.

**So:** no request to the production services. The published files, fetched as
above, are the only exception.

### V. People's names stay out of the data this repository holds

This repository is public. SBML's model history names its creators, and a
fixture or test log copied from real output would publish them.

**So:** fixtures replace those values with placeholders. Comparisons hash them,
and never print them. This is about exported data, not about contributor credits
such as the pom's developer list.

## Quality Gates

Every change is to pass the following checks. The first spec's first tasks make
each one real (Principle II):

- **The build compiles**, with `verifier-lib` pinned or the verifier fixed. This
  comes first.
- **The unit tests run and pass.** Each fix comes with a test seen failing first.
- **Checkstyle with the repository's own `checkstyle.xml`.** New code adds no
  violations.
- **libSBML's consistency check, with zero errors**, over the output for a
  fixture graph. Warnings are allowed. The fixture has to support graph-core's
  lazy loading, not only the two queries (see `CLAUDE.md`).
- **For a change to what is written:** the comparison with the reference
  (Principle I), with its result in the pull request.

## Development Workflow

- **Specs first.** Feature-sized work goes through Spec Kit before any code:
  `/speckit-specify` → `/speckit-plan` → `/speckit-tasks`. The specs live in
  `specs/`. The pull request that completes a task ticks it, with its number. A
  single-PR fix names the spec it belongs to, if any.
- **Every change is a pull request.** `main` requires a pull request but no
  approving review, and no CI check is required yet. Each pull request is still
  reviewed before it merges. Until the checks above are required, run them before
  asking for a merge.
- **Work that changes what the release produces goes on an integration branch**
  until it is proven, then merges into `main` in one pull request. `main` keeps
  what the release uses meanwhile. Spec 001's work is on `graph-only`. Why: the
  release runs the image CI builds from `main`, and the graph-only converter sat
  unproven for years with five defects; landing its changes on `main` one by one
  would change the release before the whole was compared.
- **Work that also touches the website** keeps its website part in the website's
  own specs, and links across.

## Governance

Amendments are made by pull request, and must state which principle changed and
what it cost to learn. A principle with no cost behind it does not belong here.

`CLAUDE.md` carries day-to-day guidance; where the two disagree, this file wins.

**Version**: 1.1.0 | **Ratified**: 2026-10-24 | **Last Amended**: 2026-10-24
