#!/bin/bash

PACKAGE_NAME=SBML_EXPORTER_VERSION

# Check if VERSIONS_FILE_PATH is not set or empty
if [ -z "${VERSIONS_FILE_PATH}" ]; then
    echo "Error: Please define \${VERSIONS_FILE_PATH} in the GO-CD environment or in the server."
    exit 1
fi

# Get the current version from Maven
current_version=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
echo "Current version: $current_version"


# Extract major, minor, and patch versions
major=${current_version%%.*}
rest=${current_version#*.}
minor=${rest%%.*}
patch=${rest#*.}


# Increment the minor version
new_minor=$((minor + 1))
new_version="${major}.${new_minor}.0"
# Output the new version
echo "New version: $new_version"
# Set the new version in Maven
mvn versions:set -DnewVersion=$new_version



if grep -q "^${PACKAGE_NAME}=" /var/go/versions.properties; then
    sed -i "s/^${PACKAGE_NAME}=.*/${PACKAGE_NAME}=${new_version}/" "${VERSIONS_FILE_PATH}"
else
    echo "${PACKAGE_NAME}=${new_version}" >> "${VERSIONS_FILE_PATH}"
fi
