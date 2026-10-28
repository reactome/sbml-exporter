#!/bin/bash

PACKAGE_NAME="SBML_EXPORTER_VERSION"

# Check if VERSIONS_FILE_PATH is set
if [ -z "${VERSIONS_FILE_PATH}" ]; then
    echo "Error: Please define \${VERSIONS_FILE_PATH} in the GO-CD environment or in the server."
    exit 1
fi

# Check if VERSIONS_FILE_PATH exists
if [ ! -f "${VERSIONS_FILE_PATH}" ]; then
    echo "Error: File ${VERSIONS_FILE_PATH} not found."
    exit 1
fi

# Load versions to be used
# The versions file is named at run time.
# shellcheck source=/dev/null
source "${VERSIONS_FILE_PATH}"

#add parents version and current version
xmlstarlet ed -L \
  -u "//_:project/_:parent/_:version" -v "${REACTOME_PARENT_VERSION}" \
  -u "//_:project/_:version" -v "${!PACKAGE_NAME}" \
   "pom.xml"

# the modules' parent is the root pom (spec 001, T022)
for module in lib cli; do
  xmlstarlet ed -L -u "//_:project/_:parent/_:version" -v "${!PACKAGE_NAME}" "${module}/pom.xml"
done

# the library check (ci/library-check) stands alone, at the library's version
xmlstarlet ed -L \
  -u "//_:project/_:parent/_:version" -v "${REACTOME_PARENT_VERSION}" \
  -u "//_:project/_:version" -v "${!PACKAGE_NAME}" \
  "ci/library-check/pom.xml"

echo "Maven ${PACKAGE_NAME} updated to ${!PACKAGE_NAME}"
echo "Maven parent updated to ${REACTOME_PARENT_VERSION}"


# The single pom's custom org.reactome versions were deleted here. Since the module split (spec 001, T022)
# the root pom has no dependencies of that group, and the modules' versions are deliberate: diagram-reader's
# and verifier-lib's pins, and the library's own.
