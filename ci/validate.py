"""libSBML's consistency check over every .sbml file in a folder (spec 001, research R5).

Usage: python ci/validate.py <folder>

Errors fail the check; warnings are reported and allowed. The units and
modelling-practice categories are off, as in WebsiteAngular spec 015's R5, so the
results compare with that baseline. libSBML sets no exit status, so this does.
"""
import pathlib
import sys

import libsbml

SEVERE = (libsbml.LIBSBML_SEV_ERROR, libsbml.LIBSBML_SEV_FATAL)


def check(path):
    """Return (errors, warnings), each a list of 'id: message' strings."""
    document = libsbml.readSBMLFromFile(str(path))
    # Reading problems (malformed XML, say) are in the log before the check runs.
    document.setConsistencyChecks(libsbml.LIBSBML_CAT_UNITS_CONSISTENCY, False)
    document.setConsistencyChecks(libsbml.LIBSBML_CAT_MODELING_PRACTICE, False)
    document.checkConsistency()
    errors, warnings = [], []
    log = document.getErrorLog()
    for i in range(log.getNumErrors()):
        error = log.getError(i)
        line = f"{error.getErrorId()}: {error.getShortMessage()} (line {error.getLine()})"
        (errors if error.getSeverity() in SEVERE else warnings).append(line)
    return errors, warnings


def main(argv):
    if len(argv) != 2:
        print(__doc__.strip(), file=sys.stderr)
        return 2
    files = sorted(pathlib.Path(argv[1]).glob("*.sbml"))
    if not files:
        # No files is a failure: a converter that wrote nothing must not pass.
        print(f"no .sbml files in {argv[1]}", file=sys.stderr)
        return 1
    failed = 0
    for path in files:
        errors, warnings = check(path)
        print(f"{path.name}: {len(errors)} errors, {len(warnings)} warnings")
        for line in errors:
            print(f"  error {line}")
        failed += bool(errors)
    print(f"{len(files)} files, {failed} with errors")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
