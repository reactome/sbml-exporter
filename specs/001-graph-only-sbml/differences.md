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

Measured in #47, #51 and #52 (T021) over the sample's six events with a layout today
(R-HSA-112409, R-HSA-2894858, R-HSA-9839373, R-HSA-109606, R-MMU-69620, R-HSA-69620): today's
model of each was laid out again from release 97's diagram file, and `ci/compare.py`
compared the two. Each file's offset is taken off (today's coordinates are the diagram's
shifted by a fixed amount per diagram: +3, +24; +53, +27; +22, +71; +113, +38; +48, +29 for
both R-HSA-69620 and R-MMU-69620), and labels' boxes are set aside (FR-007). The generation
note differs in all six only because of how the files were laid out again for this
comparison; it is not a difference of the converter's. Glyph sets, ids, targets, order and
curves are otherwise the same as today's, R-HSA-2894858's entirely; every other difference is
one of these.

The whole run then went through the graph (#57): the command line over all 30 events from
the private copy, with `--diagrams` holding release 97's files for every sample pathway with
a diagram (18 of the 30; the other 12, reactions and pathways drawn on an
ancestor's diagram,
have none), each compared with today's file. It finished without error, all 30 files valid
(`ci/validate.py`), and its layout differences are those of the six files above but one, the
hidden compartments' numbering (below). Its model differences are those of the tables above
and below, no others: EC numbers (957 reactions), cross-references (2,695 entries, still to
decide), 3 last-modified dates, 1 `isHomologTo`, and the 4 SBO terms below.

| Difference | Count in the sample | Cause |
| --- | --- | --- |
| A node's box | 46 of 538 species glyphs, in five events | Today's are resized to their label's font (`tightNodes`); the files keep the drawn size (FR-007) |
| A branch's end, or a backbone's end, a few units off | 67 branch ends, all at a resized node; 24 backbone ends | Follow the resized nodes: today's re-attached to the resized box |
| The hidden compartments' anchor | R-HSA-112409 | It is the diagram's bottom centre, which a resized node moved |
| Which compartment each hidden compartment glyph (`layout_hidden_compartment_N`) stands for | 2 of 3 in R-HSA-109606 | They are numbered in the model's compartment order, which differs between the two converters; element order is set aside by design |
| Glyph order | R-MMU-69620 (its species, reaction and text glyphs) | The relational diagram's order was that of its making; an inferred diagram's is neither its ids' nor the file's. The sets, ids and targets are the same |
| SBO term of a drawn species: a protein's (252), not a set's (289) | 4 species in R-MMU-69620 | The relational converter took the stored drawing's class, a set copied from the human diagram; the diagram step corrects a class that disagrees with the graph (`fixBrokenRenderableClass`), and these are single proteins in the graph. The stored class is not in the file |
| A backbone the relational diagram stored with a repeated point: its curve, the reaction's box (its hub moved), and its product's branch | 1 reaction in R-HSA-9839373 | The file's backbone has the point between; the relational one cannot be recovered from it |
| Species drawn today that the diagram file does not hold | 210 species glyphs in 46 of release 97's 1,137 human pathways with a layout, all on diagrams flagged as disease (31 of them among the 453 drawn as disease diagrams; none in the sample); no reaction glyphs | For a disease diagram (the diagram's `disease` flag), the diagram step keeps only the objects of its normal, disease, crossed, overlaid and loss-of-function lists (`Diagram.addNode`), and drops the rest; the relational converter read every stored object. They cannot be drawn from the file |

Matched to today's (decided 25 Oct 2026: match today, and suggest improvements as issues):

- **The diagram step's copies in a disease diagram are left out.** For a disease diagram not drawn as a normal one, the diagram step (reactome/diagram-converter's `DiagramFetcher`, through reactome-base's `PathwayDiagramXMLGenerator` and `DiseasePathwayImageEditor`) copies the normal reaction's glyph for each disease reaction the stored diagram does not draw, and adds disease nodes over the normal ones it replaces. The relational converter read the stored diagram alone, so its layout has none: in R-HSA-2894858, 2 of the 21 reactions, where the files have 21 (19 reaction and 16 species glyphs copied). The files do not list the copies, but the code guarantees two things of each: its id is above every stored object's (the counter it takes its id from is moved past each stored id as the diagram is read), and it lies exactly over the stored object it copies, which has a lower id (a reaction at its position; a node at its centre, since a node whose label overflows is resized about it). `LayoutConverter.copies` takes the disease objects with both. Measured over every human pathway with a layout in release 97's published `homo_sapiens.3.1.sbml.tgz` (1,137 diagram files fetched; 453 are disease diagrams, 435 of them with copies, 1,452 copies): the objects it takes are exactly those today's layouts leave out, with nothing today draws taken and nothing today leaves out kept. Either condition alone is not enough: an id above every normal object's would also take disease objects the curators drew last (12 diagrams), and lying over an earlier object would take disease reactions the curators drew over their normal ones (2). Drawing the copies, as the website does, is suggested in #48; having the diagram step mark them, in #49.

Fixed, since they make the layout invalid:

- **The layout has an id and dimensions** (the diagram's extent): libSBML refuses a layout without them (6020305, 6020315), and every one of today's has neither.
- **A branch to a part not drawn as a species is left out**, so that no glyph points at nothing (6021011); the others keep their numbers. None of today's six files has one: the port's own output did, from a disease copy, before the copies were left out.

With the layout valid, libSBML goes on to warn that a set's SBO term, 289 (functional compartment), is not a material entity's (10713): the relational converter's choice for Minerva, kept.

## Open: to decide

| Difference | Count in the sample | What today does | What the graph converter does |
| --- | --- | --- | --- |
| Reactions' cross-references (`bqmodel:hasInstance`) | 2,695 reaction entries across the sample's files; 2,747 more links: 2,626 MONDO (`monarchinitiative.org`) and 121 Rhea | No MONDO, 44 Rhea | Every cross-reference the graph holds. The MONDO ones were curated (each has a creation record), so why the relational output lacks them is not yet known. Asked in #36 |

## Decided

| Difference | Decision |
| --- | --- |
| A single reaction's model was its first parent pathway (id, name, notes, references, history) | Matched to today's in #37: `no_parent_pathway`, "No parent pathway detected". A reaction can have several parents (733 human reactions in release 97, up to 10), so any one would be arbitrary. Five of the sample's nine reactions now match today's exactly; the other four differ only in EC numbers or cross-references |
