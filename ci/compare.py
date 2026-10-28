"""Compares two folders of SBML element by element (spec 001, FR-004, T016).

Usage: python ci/compare.py [--exact] <reference-folder> <folder>

--exact is for comparing the graph converter with its own expected output (CI):
no offset is taken off the coordinates, and the layout's id and dimensions are
compared. Without it, a regression that moved every coordinate by the same
amount, or a wrong extent, passed.

The reference is today's relational output of the same release. Files are
paired by name. Each file is reduced to a set of (path, value) pairs:

- the document's notes, the model, and every compartment, species and reaction
  by id: name, SBO term, attributes, notes, annotations (each qualifier's
  resources) and history;
- each reaction's reactants, products and modifiers by species: stoichiometry,
  SBO term and constant;
- the layout (FR-007, T021): whether the package is required; each kind of
  glyph's ids, in order; each glyph's target and box; each reaction glyph's
  curve and its species reference glyphs, in order, with their species glyph,
  role and curve; each text glyph's text and object. Coordinates are compared
  after taking off the offset between the two files' layouts, the commonest
  difference between the same species glyph's positions: today's coordinates
  are the diagram's shifted by a fixed amount per diagram.

Set aside, as FR-004 says:

- the generation note's timestamp ("... on <date> using ...");
- element order, including the order of an annotation's resources: elements are
  matched by id and resources compared as sets;
- metaids, which follow element order;
- the order of a complex's parts in its species' note ("nested structure for
  this complex: (...)"): today's comes from a HashMap of database instances,
  which no run reproduces, so the parts are compared sorted;
- a text glyph's box: the diagram files have no label size, so a label's box
  is its glyph's (FR-007).

Not compared, as today's files have none of them: the layout's own id and
dimensions (today's layouts have neither, which libSBML refuses; the graph
converter's have both; --exact compares them), general glyphs, and species
reference glyphs' boxes.

History creators are compared as hashes, so the report never prints a name.
Every other difference is printed and fails the comparison.
"""
import hashlib
import pathlib
import re
import sys

import libsbml

GENERATED = re.compile(r"( generated from Reactome version \S+) on .*? (using )")
STRUCTURE = re.compile(r"(nested structure for this complex: \()([^)]*)(\))")


def text(value):
    return " ".join(value.split())


def creator(c):
    who = "|".join([c.getFamilyName(), c.getGivenName(), c.getEmail(), c.getOrganisation()])
    return "creator:" + hashlib.sha256(who.encode()).hexdigest()[:12]


def qualifier(term):
    if term.getQualifierType() == libsbml.MODEL_QUALIFIER:
        return "bqmodel:" + libsbml.ModelQualifierType_toString(term.getModelQualifierType())
    return "bqbiol:" + libsbml.BiolQualifierType_toString(term.getBiologicalQualifierType())


def describe(sbase, path, out):
    """Add an element's common parts: SBO term, notes, annotations, history."""
    if sbase.isSetSBOTerm():
        out[f"{path}/sboTerm"] = sbase.getSBOTermID()
    if sbase.isSetNotes():
        notes = text(sbase.getNotesString())
        out[f"{path}/notes"] = STRUCTURE.sub(lambda m: m.group(1) + ", ".join(sorted(m.group(2).split(", "))) + m.group(3), notes)
    resources = {}
    for i in range(sbase.getNumCVTerms()):
        term = sbase.getCVTerm(i)
        found = resources.setdefault(qualifier(term), set())
        for j in range(term.getNumResources()):
            found.add(term.getResourceURI(j))
    for name, found in resources.items():
        out[f"{path}/{name}"] = sorted(found)
    if sbase.isSetModelHistory():
        history = sbase.getModelHistory()
        out[f"{path}/creators"] = sorted(creator(history.getCreator(i)) for i in range(history.getNumCreators()))
        if history.isSetCreatedDate():
            out[f"{path}/created"] = history.getCreatedDate().getDateAsString()
        out[f"{path}/modified"] = sorted(history.getModifiedDate(i).getDateAsString() for i in range(history.getNumModifiedDates()))


def number(value):
    """A coordinate, exact to a thousandth at any size."""
    value = round(value, 3)
    return str(int(value)) if value == int(value) else repr(value)


def box(glyph, offset):
    b = glyph.getBoundingBox()
    return (f"{number(b.getX() - offset[0])},{number(b.getY() - offset[1])} "
            f"{number(b.getWidth())}x{number(b.getHeight())}")


def curve(glyph, offset):
    if not glyph.isSetCurve():
        return ""
    c = glyph.getCurve()
    return "; ".join(f"{number(s.getStart().getXOffset() - offset[0])},{number(s.getStart().getYOffset() - offset[1])}-"
                     f"{number(s.getEnd().getXOffset() - offset[0])},{number(s.getEnd().getYOffset() - offset[1])}"
                     for s in (c.getCurveSegment(i) for i in range(c.getNumCurveSegments())))


def layouts(model):
    plugin = model.getPlugin("layout")
    return [] if plugin is None else [plugin.getLayout(i) for i in range(plugin.getNumLayouts())]


def positions(model):
    """Each species glyph's position, by id."""
    return {g.getId(): (g.getBoundingBox().getX(), g.getBoundingBox().getY())
            for layout in layouts(model)
            for g in (layout.getSpeciesGlyph(i) for i in range(layout.getNumSpeciesGlyphs()))}


def offset(reference, candidate):
    """The commonest difference between the same species glyphs' positions: what to take off the reference's."""
    old, new = positions(reference), positions(candidate)
    shifts = [(old[k][0] - new[k][0], old[k][1] - new[k][1]) for k in old.keys() & new.keys()]
    # The commonest; a tie goes to the smallest, so that every run takes the same.
    return max(sorted(set(shifts)), key=shifts.count) if shifts else (0, 0)


def layout(document, at_offset, out, exact=False):
    model = document.getModel()
    if document.isPackageEnabled("layout"):
        out["layout/required"] = document.getPackageRequired("layout")
    for n, lay in enumerate(layouts(model)):
        at = f"layout[{n}]"
        if exact:
            # getDimensions() is never None: a layout without the element reads as 0x0,
            # so whether it was written is asked separately.
            dimensions = lay.getDimensions()
            out[f"{at}/id"] = lay.getId()
            out[f"{at}/dimensions"] = (
                (dimensions.getWidth(), dimensions.getHeight(), dimensions.getDepth())
                if lay.getDimensionsExplicitlySet()
                else None
            )
        kinds = (("compartmentGlyph", lay.getNumCompartmentGlyphs, lay.getCompartmentGlyph, "getCompartmentId"),
                 ("speciesGlyph", lay.getNumSpeciesGlyphs, lay.getSpeciesGlyph, "getSpeciesId"),
                 ("reactionGlyph", lay.getNumReactionGlyphs, lay.getReactionGlyph, "getReactionId"),
                 ("textGlyph", lay.getNumTextGlyphs, lay.getTextGlyph, "getGraphicalObjectId"))
        for kind, count, get, target in kinds:
            glyphs = [get(i) for i in range(count())]
            out[f"{at}/{kind}s"] = " ".join(g.getId() for g in glyphs)
            for g in glyphs:
                gat = f"{at}/{kind}[{g.getId()}]"
                out[f"{gat}/target"] = getattr(g, target)()
                if kind == "textGlyph":
                    out[f"{gat}/text"] = g.getText()
                    continue
                out[f"{gat}/box"] = box(g, at_offset)
                if kind == "reactionGlyph":
                    out[f"{gat}/curve"] = curve(g, at_offset)
                    refs = [g.getSpeciesReferenceGlyph(i) for i in range(g.getNumSpeciesReferenceGlyphs())]
                    out[f"{gat}/speciesReferenceGlyphs"] = " ".join(r.getId() for r in refs)
                    for r in refs:
                        rat = f"{gat}/speciesReferenceGlyph[{r.getId()}]"
                        out[f"{rat}/speciesGlyph"] = r.getSpeciesGlyphId()
                        out[f"{rat}/role"] = r.getRoleString() if r.isSetRole() else ""
                        out[f"{rat}/curve"] = curve(r, at_offset)


def participants(references, path, out):
    for i in range(references.size()):
        ref = references.get(i)
        at = f"{path}[{ref.getSpecies()}]"
        while f"{at}/sboTerm" in out or at in out:
            at += "+"  # the same species twice
        out[at] = "present"
        describe(ref, at, out)
        if isinstance(ref, libsbml.SpeciesReference):
            if ref.isSetStoichiometry():
                out[f"{at}/stoichiometry"] = ref.getStoichiometry()
            if ref.isSetConstant():
                out[f"{at}/constant"] = ref.getConstant()


def read(path):
    document = libsbml.readSBMLFromFile(str(path))
    # Only an unreadable file stops the comparison. Validity is ci/validate.py's
    # job, and today's layouts have errors of their own (a layout with no id).
    if document.getNumErrors(libsbml.LIBSBML_SEV_FATAL):
        raise ValueError(f"{path}: cannot be read: {document.getError(0).getMessage().strip()}")
    return document


def reduce(document, at_offset=(0, 0), exact=False):
    """Return {path: value} for one file."""
    out = {}
    if document.isSetNotes():
        out["sbml/notes"] = GENERATED.sub(r"\1 on <date> \2", text(document.getNotesString()))
    model = document.getModel()
    if model is None:
        return out
    out["model/id"] = model.getId()
    out["model/name"] = model.getName()
    describe(model, "model", out)
    for i in range(model.getNumCompartments()):
        c = model.getCompartment(i)
        at = f"compartment[{c.getId()}]"
        out[f"{at}/name"] = c.getName()
        describe(c, at, out)
    for i in range(model.getNumSpecies()):
        s = model.getSpecies(i)
        at = f"species[{s.getId()}]"
        out[f"{at}/name"] = s.getName()
        out[f"{at}/compartment"] = s.getCompartment()
        for attribute in ("BoundaryCondition", "Constant", "HasOnlySubstanceUnits"):
            out[f"{at}/{attribute[0].lower() + attribute[1:]}"] = getattr(s, "get" + attribute)()
        describe(s, at, out)
    for i in range(model.getNumReactions()):
        r = model.getReaction(i)
        at = f"reaction[{r.getId()}]"
        out[f"{at}/name"] = r.getName()
        out[f"{at}/compartment"] = r.getCompartment()
        out[f"{at}/reversible"] = r.getReversible()
        out[f"{at}/fast"] = r.getFast()
        describe(r, at, out)
        participants(r.getListOfReactants(), f"{at}/reactant", out)
        participants(r.getListOfProducts(), f"{at}/product", out)
        participants(r.getListOfModifiers(), f"{at}/modifier", out)
    layout(document, at_offset, out, exact)
    return out


def shown(value):
    value = str(value)
    return value if len(value) <= 160 else value[:157] + "..."


def compare(reference, candidate, exact=False):
    """Return the differences between two files, as printable lines."""
    old_document, new_document = read(reference), read(candidate)
    shift = (0, 0)
    if not exact and old_document.getModel() is not None and new_document.getModel() is not None:
        shift = offset(old_document.getModel(), new_document.getModel())
    old, new = reduce(old_document, shift, exact), reduce(new_document, (0, 0), exact)
    lines = []
    for key in sorted(old.keys() | new.keys()):
        if key not in new:
            lines.append(f"- {key}: only in the reference: {shown(old[key])}")
        elif key not in old:
            lines.append(f"+ {key}: only in the new file: {shown(new[key])}")
        elif old[key] != new[key]:
            if isinstance(old[key], list) and isinstance(new[key], list):
                gone, added = sorted(set(old[key]) - set(new[key])), sorted(set(new[key]) - set(old[key]))
                lines.append(f"~ {key}: reference only {shown(gone)}; new only {shown(added)}")
            elif isinstance(old[key], str) and isinstance(new[key], str) and len(old[key]) + len(new[key]) > 160:
                # Long text: show where the two first part.
                at = next((i for i, (a, b) in enumerate(zip(old[key], new[key])) if a != b), min(len(old[key]), len(new[key])))
                start = max(0, at - 40)
                lines.append(f"~ {key}: from character {at}: reference ...{shown(old[key][start:at + 80])}; new ...{shown(new[key][start:at + 80])}")
            else:
                lines.append(f"~ {key}: reference {shown(old[key])}; new {shown(new[key])}")
    return lines


def main(argv):
    exact = len(argv) > 1 and argv[1] == "--exact"
    if exact:
        argv = argv[:1] + argv[2:]
    if len(argv) != 3:
        print(__doc__.strip(), file=sys.stderr)
        return 2
    reference, candidate = pathlib.Path(argv[1]), pathlib.Path(argv[2])
    names = {p.name for p in reference.glob("*.sbml")}
    other = {p.name for p in candidate.glob("*.sbml")}
    if not names:
        # No reference files is a failure: an empty comparison must not pass.
        print(f"no .sbml files in {reference}", file=sys.stderr)
        return 1
    differing = 0
    for name in sorted(names - other):
        print(f"{name}: missing from {candidate}")
    for name in sorted(other - names):
        print(f"{name}: not in the reference")
    for name in sorted(names & other):
        lines = compare(reference / name, candidate / name, exact)
        print(f"{name}: {len(lines)} differences")
        for line in lines:
            print(f"  {line}")
        differing += bool(lines)
    unpaired = len(names ^ other)
    print(f"{len(names & other)} files compared, {differing} differing, {unpaired} unpaired")
    return 1 if differing or unpaired else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
