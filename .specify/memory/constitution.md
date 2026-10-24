# sbml-exporter Constitution

These principles are not aspirations. Each one is here because breaking it has
already cost Reactome's SBML something specific. The cost is named so the rule
can be argued with rather than merely obeyed.

## Core Principles

### I. Proven against a reference, and the reference itself checked

The graph-only `SbmlConverter` was written, then left uncalled. The command line
and the website both still use the relational converter. When it was finally run
against release 97's graph (24 Oct 2026), five data defects showed at once:

- catalysts were written from the positive regulators;
- negative regulators were written from the positive regulators;
- positive regulators were never written;
- one `Participant` was reused, so every reaction kept a single reactant and a
  single product;
- catalysts were never read from the query.

Nothing had ever compared its output with anything.

**So:** a change to what is written is compared with a reference.

- **The model** is compared byte for byte, the generation note's timestamp set
  aside, against the graph-only converter with those defects fixed.
- **The layout** is compared by meaning against today's: the same glyphs, ids,
  roles and order.

A reference is itself checked before it is trusted. That is how the five defects
were found.

### II. Prove a test fails before believing it passes

The repository has no tests, and the defects above survived for years because
nothing failed.

**So:** every fix comes with a test that fails without it, and that has been seen
to fail. Where that is impractical, say so in the pull request rather than
implying coverage.

### III. One implementation, for the release and the website

The release's archives (`all_species.3.1.sbml.tgz` and
`homo_sapiens.3.1.sbml.tgz`) and the website's per-event SBML share the
converter's code. They also share its dependence on the relational database,
which the converter uses for the layout.

**So:** this repository has a library module (graph in, SBML out, the layout
from the diagram files the website draws) and a thin command line for the
release. The website depends on the published library, not on a copy. Neither
needs the relational database.

### IV. Production is not a test fixture

Reactome's production services are not used for development, comparison or
verification. The release's published archives in its download directory are
the reference for the bulk output. A private copy of the release's graph
database is the source for runs.

**So:** no request to the production services. The published files are fetched
one at a time when a comparison needs them, and kept out of the repository.

### V. People's names stay out of the repository

SBML's model history names its creators.

**So:** fixtures replace personal values with placeholders. Comparisons hash
them, and never print them.

## Quality Gates

Every change passes:

- `mvn test`: unit tests, each fix with a test seen failing first;
- checkstyle, as CI's lint stage runs it;
- libSBML's consistency check over the output for a fixture graph, with zero
  errors;
- for a change to what is written, the comparison with the reference
  (Principle I), with its result in the pull request.

## Development Workflow

- **Specs first.** Feature-sized work goes through Spec Kit before any code:
  `/speckit-specify` → `/speckit-plan` → `/speckit-tasks`. The specs live in
  `specs/`. The pull request that completes a task ticks it, with its number.
- **`main` is protected.** Every change is a pull request.
- **Work that also touches the website** keeps its website part in the website's
  own specs, and links across.

## Governance

Amendments are made by pull request, and must state which principle changed and
what it cost to learn. A principle with no cost behind it does not belong here.

`CLAUDE.md` carries day-to-day guidance; where the two disagree, this file wins.

**Version**: 1.0.0 | **Ratified**: 2026-10-24 | **Last Amended**: 2026-10-24
