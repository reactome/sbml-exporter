Files for showing `ci/compare.py` works, each folder holding one `event.sbml`:

- `reference`: a small model, with a placeholder creator.
- `same`: the reference with only what the comparison sets aside changed: the
  generation note's timestamp, the metaids, the order of the species, of an
  annotation's resources and of a complex's parts in its note, the layout's
  coordinates by a fixed offset (the reference's are shifted by +10, +20, as
  today's are shifted from the diagram's) and the text glyphs' boxes. `python ci/compare.py ci/test/compare/reference ci/test/compare/same`
  reports 0 differences.
- `changed`: the reference with a product removed, a species' SBO term changed,
  a literature reference removed and a part's count changed in a complex's note;
  and in the layout, two species glyphs swapped in order, a backbone point
  moved, a substrate's glyph pointing at the other species and a label's text
  changed. The comparison reports each and fails.
