Files for showing `ci/compare.py` works, each folder holding one `event.sbml`:

- `reference`: a small model, with a placeholder creator.
- `same`: the reference with only what the comparison sets aside changed: the
  generation note's timestamp, the metaids, the order of the species and of an
  annotation's resources. `python ci/compare.py ci/test/compare/reference ci/test/compare/same`
  reports 0 differences.
- `changed`: the reference with a product removed, a species' SBO term changed
  and a literature reference removed. The comparison reports each and fails.
