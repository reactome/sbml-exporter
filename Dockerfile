ARG REPO_DIR=/opt/sbml-exporter


# ===== stage 1 =====
FROM maven:3.9.6-eclipse-temurin-11-focal AS setup-env

ARG REPO_DIR

WORKDIR ${REPO_DIR}

COPY . .

SHELL ["/bin/bash", "-c"]

# run lint if container started
ENTRYPOINT []

# Maven's own status decides: a failure that names no .java file (an unresolvable
# parent, say) fails lint too. lint.log is what CI shows.
CMD mvn -B -q -nsu checkstyle:check > lint.log 2>&1; \
    status=$?; \
    cat lint.log; \
    exit $status


# ===== stage 2 =====
FROM setup-env AS build-jar

# -nsu: no snapshot updates mid-build; ci/snapshots.sh checks what resolved.
RUN mvn -B -nsu clean package

# Fail if a snapshot dependency changed under the build (spec 001, research R2).
RUN ci/snapshots.sh check


# ===== stage 3 =====
FROM eclipse-temurin:11-jre-focal

ARG REPO_DIR

ARG EXEC_JAR_FILE=target/sbml-exporter-exec.jar
ARG VERIFIER_JAR_FILE=target/sbml-exporter-verifier.jar

WORKDIR ${REPO_DIR}

# The command line's module builds the jars; the image keeps them where they always were.
COPY --from=build-jar ${REPO_DIR}/cli/${EXEC_JAR_FILE} ./target/
COPY --from=build-jar ${REPO_DIR}/cli/${VERIFIER_JAR_FILE} ./target/
