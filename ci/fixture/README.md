# The fixture graph (spec 001, T007)

A small graph for CI: three events of release 97 and everything the converter reads
about them, with every person's name, and all free text that could cite one, replaced.

- **`fixture.cypher`**: 609 nodes and 874 relationships, for
  - R-HSA-9613354, Lipophagy: a pathway with a diagram and five reactions;
  - R-HSA-8932284: a reaction with a catalyst and a regulator;
  - R-HSA-5667111: a black-box event;
  - and release 97's `DBInfo` node, whose version the converter writes.
- **`expected/`**: the command line's output for the three events from this graph and this folder of diagram files (`--diagrams ci/fixture --diagram-release 97`), with Lipophagy's layout; CI's `validity` job compares its output with them (`ci/compare.py`).
- **`R-HSA-9613354.json`**: Lipophagy's diagram file, from release 97's download
  directory (`download.reactome.org/97/diagram/R-HSA-9613354.json`, sha256
  `cad296e4…a425d1`), for the layout (T019 onwards).
- **`extract.py`**: wrote `fixture.cypher`.
- **`check-placeholders.sh`**: fails if the fixture leaves its allowlists (below); CI runs it.
- **`audit.py`**: searches the fixture for its people's real names, against the private
  copy, at extraction; it prints counts only.

## How it was made

From a private copy of release 97's graph (Neo4j 4.4, database `graph.db`, never a
shared or production graph):

```
python ci/fixture/extract.py http://<private-copy>:7474 R-HSA-9613354 R-HSA-8932284 R-HSA-5667111 > ci/fixture/fixture.cypher
ci/fixture/check-placeholders.sh
python3 -I ci/fixture/audit.py http://<private-copy>:7474
```

`extract.py` follows, in their direction, the relationships the converter reads, and
keeps every node's full label set and properties, which Spring Data Neo4j maps by.
Species, compartments, people, publications, reference databases and orthologous
entities are taken without their own relationships.

What could name a person is replaced:
- every `Person` keeps only its dbId and class and becomes `Placeholder` `PersonN`;
- every `InstanceEdit`'s display name and note;
- every summation's text, which cites authors ("X et al": all eight did), and its
  display name, the text's opening;
- every publication's display name, which begins with its first author's;
- every comment.

The expected output is written from this graph, so it carries the placeholders too
(a summation is written into the notes). A dbId is kept, as the converter writes it;
the content service resolves it to the object, a person's name included, so the
placeholders hide a name from this repository, not from the service.

## How it was checked

- **Complete:** the three events converted from the fixture (loaded into a fresh
  `neo4j:4.4.48-community`, database `graph.db`, with `cypher-shell -f`) and from the
  full graph compare with `ci/compare.py` with no differences but the placeholders: the
  creators, and the eight summations in the notes. All three files pass `ci/validate.py`.
- **No names, by allowlist (CI):** `check-placeholders.sh` (BioPAX3Exporter's, spec 001
  T006) passes on the fixture. It holds people, edits, summations, publications' display
  names and comments to placeholders; every line to the four shapes `extract.py`
  writes; every node's labels to the graph classes the fixture holds, its `schemaClass`
  among them; and refuses, on any node, a key from its list of person-like
  names (in any capitalisation, underscores and hyphens aside; a longer key containing
  one, such as `authorName`, is not matched) and any value like an email address or an ORCID. It
  fails on the fixture before this change, whose summations cited authors.
- **No names, by search (at extraction):** `audit.py` finds none of the fixture's six
  people's real names in it, nor an email or an ORCID. It cannot see a cited author who
  is not among those people: the fixture before this change passed it too, though its
  summations cited authors. The placeholders, held by the check, are what keep those
  out.

To change the events, rerun both commands against a private copy and repeat both
checks.
