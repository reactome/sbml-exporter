# The fixture graph (spec 001, T007)

A small graph for CI: three events of release 97 and everything the converter reads
about them, with every person's name replaced.

- **`fixture.cypher`**: 608 nodes and 800 relationships, for
  - R-HSA-9613354, Lipophagy: a pathway with a diagram and five reactions;
  - R-HSA-8932284: a reaction with a catalyst and a regulator;
  - R-HSA-5667111: a black-box event;
  - and release 97's `DBInfo` node, whose version the converter writes.
- **`expected/`**: the command line's output for the three events from this graph and this folder of diagram files (`--diagrams ci/fixture --diagram-release 97`), with Lipophagy's layout; CI's `validity` job compares its output with them (`ci/compare.py`).
- **`R-HSA-9613354.json`**: Lipophagy's diagram file, from release 97's download
  directory (`download.reactome.org/97/diagram/R-HSA-9613354.json`, sha256
  `cad296e4…a425d1`), for the layout (T019 onwards).
- **`extract.py`**: wrote `fixture.cypher`.
- **`check-placeholders.sh`**: fails if a person's name could have survived.

## How it was made

From a private copy of release 97's graph (Neo4j 4.4, database `graph.db`, never a
shared or production graph):

```
python ci/fixture/extract.py http://<private-copy>:7474 R-HSA-9613354 R-HSA-8932284 R-HSA-5667111 > ci/fixture/fixture.cypher
ci/fixture/check-placeholders.sh
```

`extract.py` follows, in their direction, the relationships the converter reads, and
keeps every node's full label set and properties, which Spring Data Neo4j maps by.
Species, compartments, people, publications, reference databases and orthologous
entities are taken without their own relationships. Every `Person` becomes
`Placeholder` `PersonN`, without ORCID or project, and every `InstanceEdit`'s display
name and note a placeholder.

## How it was checked

- **Complete:** the three events converted from the fixture (loaded into a fresh
  `neo4j:4.4.48-community`, database `graph.db`, with `cypher-shell -f`) and from the
  full graph compare with `ci/compare.py` with no differences but the creators, which
  are the placeholders. All three files pass `ci/validate.py`.
- **No names:** `check-placeholders.sh` passes on the fixture, and fails on a copy
  with a person's surname restored, one with an `InstanceEdit`'s display name
  restored, and one with an ORCID added.

To change the events, rerun both commands against a private copy and repeat both
checks.
