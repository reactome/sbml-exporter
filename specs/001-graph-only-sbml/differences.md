# Differences from today's SBML

The one list for SBML (FR-004; WebsiteAngular spec 015's `contracts/differences.md`
links here). Each way the graph-only converter's output differs from today's
relational output of the same release, with its cause. `ci/compare.py` finds them;
what it sets aside by design (the timestamp, element order, metaids and, until the
layout is ported, the layout) is listed in the script, not here.

## How this was measured

- **Sample:** WebsiteAngular spec 015's 30 events, release 97.
- **Today's files:** the relational converter's, from a ContentService of release 97
  that is not production. They match the release's published `homo_sapiens.3.1.sbml.tgz`
  exactly for the 19 sample pathways it holds (0 differences), so they are the
  release's own output.
- **The graph converter's files:** from a private copy of release 97's graph, after
  #33 and #34.
- **Result:** 30 files, all valid (`ci/validate.py`); 3,727 differences after #34, every
  one of them below or fixed since.

## In the data, not the converter

The converter writes what the graph holds; these differ because the graph holds
something different from the relational database of the same release.

| Difference | Count in the sample | Cause |
| --- | --- | --- |
| Fewer EC numbers on reactions: a subset of today's, never a different one (254 distinct against 360) | 957 reactions | The graph keeps one `ecNumber` per GO molecular function; the relational database keeps every one. Fixing it is the graph's import, not this converter |
| A pathway's history: an earlier last-modified date | 3 pathways | The relational database has later edits (12 June 2026) than the graph (18 May 2026) for these instances |
| `isHomologTo` missing for one species | 1 species | Its inferred rat and yeast entities are not in the graph |

## Open: to decide

| Difference | Count in the sample | What today does | What the graph converter does |
| --- | --- | --- | --- |
| Reactions' cross-references (`bqmodel:hasInstance`) | 2,695 reaction entries across the sample's files; 2,747 more links: 2,626 MONDO (`monarchinitiative.org`) and 121 Rhea | No MONDO, 44 Rhea | Every cross-reference the graph holds. The MONDO ones were curated (each has a creation record), so why the relational output lacks them is not yet known. Asked in #36 |

## Decided

| Difference | Decision |
| --- | --- |
| A single reaction's model was its first parent pathway (id, name, notes, references, history) | Matched to today's in #37: `no_parent_pathway`, "No parent pathway detected". A reaction can have several parents (733 human reactions in release 97, up to 10), so any one would be arbitrary. Five of the sample's nine reactions now match today's exactly; the other four differ only in EC numbers or cross-references |
