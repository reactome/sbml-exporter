Files for showing `ci/compare.py` works, each folder holding one `event.sbml`:

- `reference`: a small model, with a placeholder creator.
- `same`: the reference with only what the comparison sets aside changed: the
  generation note's timestamp, the metaids, the order of the species, of an
  annotation's resources and of a complex's parts in its note. `python ci/compare.py ci/test/compare/reference ci/test/compare/same`
  reports 0 differences.
- `changed`: the reference with a product removed, a species' SBO term changed,
  a literature reference removed and a part's count changed in a complex's note. The comparison reports each and fails.
