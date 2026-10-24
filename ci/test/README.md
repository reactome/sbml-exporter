Files for showing `ci/validate.py` works: `valid.sbml` passes, and `invalid.sbml.txt`
(a species in a compartment that does not exist) fails once renamed to `.sbml`. Its
suffix keeps it out of any folder the check runs over by accident.
