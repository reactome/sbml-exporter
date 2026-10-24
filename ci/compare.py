"""Compares two folders of SBML element by element (spec 001, FR-004, T016).

Usage: python ci/compare.py <reference-folder> <folder>

The reference is today's relational output of the same release. Files are
paired by name. Each file is reduced to a set of (path, value) pairs:

- the document's notes, the model, and every compartment, species and reaction
  by id: name, SBO term, attributes, notes, annotations (each qualifier's
  resources) and history;
- each reaction's reactants, products and modifiers by species: stoichiometry,
  SBO term and constant.

Set aside, as FR-004 says:

- the generation note's timestamp ("... on <date> using ...");
- element order, including the order of an annotation's resources: elements are
  matched by id and resources compared as sets;
- metaids, which follow element order;
- the order of a complex's parts in its species' note ("nested structure for
  this complex: (...)"): today's comes from a HashMap of database instances,
  which no run reproduces, so the parts are compared sorted;
- the layout, until it is ported (T021): the layout package, the root's
  `layout:required` and namespace, and the SBO term of every species the
  reference's layout draws, since the layout sets those.

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


def describe(sbase, path, out, skip_sbo=False):
    """Add an element's common parts: SBO term, notes, annotations, history."""
    if sbase.isSetSBOTerm() and not skip_sbo:
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


def drawn_species(model):
    """The ids of the species the model's layouts draw (their SBO term is the layout's)."""
    plugin = model.getPlugin("layout")
    drawn = set()
    if plugin is None:
        return drawn
    for i in range(plugin.getNumLayouts()):
        layout = plugin.getLayout(i)
        for j in range(layout.getNumSpeciesGlyphs()):
            drawn.add(layout.getSpeciesGlyph(j).getSpeciesId())
    return drawn


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


def reduce(path, skip_sbo_of=()):
    """Return ({path: value}, drawn species ids) for one file."""
    document = libsbml.readSBMLFromFile(str(path))
    # Only an unreadable file stops the comparison. Validity is ci/validate.py's
    # job, and today's layouts have errors of their own (a layout with no id).
    if document.getNumErrors(libsbml.LIBSBML_SEV_FATAL):
        raise ValueError(f"{path}: cannot be read: {document.getError(0).getMessage().strip()}")
    out = {}
    if document.isSetNotes():
        out["sbml/notes"] = GENERATED.sub(r"\1 on <date> \2", text(document.getNotesString()))
    model = document.getModel()
    if model is None:
        return out, set()
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
        describe(s, at, out, skip_sbo=s.getId() in skip_sbo_of)
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
    return out, drawn_species(model)


def shown(value):
    value = str(value)
    return value if len(value) <= 160 else value[:157] + "..."


def compare(reference, candidate):
    """Return the differences between two files, as printable lines."""
    _, drawn = reduce(reference)
    old, _ = reduce(reference, skip_sbo_of=drawn)
    new, _ = reduce(candidate, skip_sbo_of=drawn)
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
        lines = compare(reference / name, candidate / name)
        print(f"{name}: {len(lines)} differences")
        for line in lines:
            print(f"  {line}")
        differing += bool(lines)
    unpaired = len(names ^ other)
    print(f"{len(names & other)} files compared, {differing} differing, {unpaired} unpaired")
    return 1 if differing or unpaired else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
