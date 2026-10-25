# Differences from today's SBML

The one list for SBML (FR-004; WebsiteAngular spec 015's `contracts/differences.md`
links here). Each way the graph-only converter's output differs from today's
relational output of the same release, with its cause. `ci/compare.py` finds them;
what it sets aside by design (the timestamp, element order, metaids, and labels' boxes)
is listed in the script, not here.

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

## The layout

Measured in #47 (T021) over the sample's six events with a layout today (R-HSA-112409,
R-HSA-2894858, R-HSA-9839373, R-HSA-109606, R-MMU-69620, R-HSA-69620): today's model of
each was laid out again from release 97's diagram file, and `ci/compare.py` compared the
two. Each file's offset (today's coordinates are the diagram's shifted by a fixed amount,
from +3, +24 to +113, +38) is taken off, and labels' boxes are set aside (FR-007). The
re-laid files are all valid (`ci/validate.py`), where today's are not (below). Glyph ids,
targets, kinds and curves are otherwise the same; every difference is one of these.

| Difference | Count in the sample | Cause |
| --- | --- | --- |
| A node's box | 46 of 538 species glyphs, in five events | Today's are resized to their label's font (`tightNodes`); the files keep the drawn size (FR-007) |
| A branch's end, or a backbone's end, a few units off | 67 branch ends, all at a resized node; 24 backbone ends | Follow the resized nodes: today's re-attached to the resized box |
| The hidden compartments' anchor | R-HSA-112409 | It is the diagram's bottom centre, which a resized node moved |
| Disease reactions drawn over their normal counterparts: 19 reaction glyphs, 16 species glyphs and their labels added, and the 15 species' SBO terms set by the layout | R-HSA-2894858, a disease diagram | The diagram step draws each disease reaction over its faded normal one, as the website shows it; the relational diagram drew 2 of the pathway's 21 reactions. Kept: the layout shows what the website's diagram shows (US3) |
| Glyph order | R-MMU-69620 (each kind's glyphs) | The relational diagram's order was that of its making; an inferred diagram's is neither its ids' nor the file's. The sets, ids and targets are the same |
| SBO term of a drawn species: protein (252), not set (289) | 4 species in R-MMU-69620 | Single proteins in the graph and the file, drawn as sets in the relational diagram copied from the human one |
| A backbone the relational diagram stored with a repeated point, and its product's branch | 1 reaction in R-HSA-9839373 | The file's backbone has the point between; the relational one cannot be recovered from it |

Fixed against today's, since today's layouts are invalid:

- **The layout has an id and dimensions** (the diagram's extent): libSBML refuses a layout without them (6020305, 6020315), and every one of today's has neither.
- **A branch to a part not drawn as a species is left out**: a failed reaction's overlay names its normal counterpart's product, which is not in the model, and a glyph pointing at no species glyph is invalid (6021011). The others keep their numbers.

With the layout valid, libSBML goes on to warn that a set's SBO term, 289 (functional compartment), is not a material entity's (10713): the relational converter's choice for Minerva, kept.

## Open: to decide

| Difference | Count in the sample | What today does | What the graph converter does |
| --- | --- | --- | --- |
| Reactions' cross-references (`bqmodel:hasInstance`) | 2,695 reaction entries across the sample's files; 2,747 more links: 2,626 MONDO (`monarchinitiative.org`) and 121 Rhea | No MONDO, 44 Rhea | Every cross-reference the graph holds. The MONDO ones were curated (each has a creation record), so why the relational output lacks them is not yet known. Asked in #36 |

## Decided

| Difference | Decision |
| --- | --- |
| A single reaction's model was its first parent pathway (id, name, notes, references, history) | Matched to today's in #37: `no_parent_pathway`, "No parent pathway detected". A reaction can have several parents (733 human reactions in release 97, up to 10), so any one would be arbitrary. Five of the sample's nine reactions now match today's exactly; the other four differ only in EC numbers or cross-references |
