ARG REPO_DIR=/opt/sbml-exporter


# ===== stage 0 =====
# verifier-lib built from a fixed commit of release-verifier-lib. The
# 1.0-SNAPSHOT published on 19 Oct 2026 lacks DefaultVerifier and Verifier, and
# the snapshot before it is no longer served (spec 001, research R1). Drop this
# stage once verifier-lib is released (spec 001, T029).
FROM maven:3.9.6-eclipse-temurin-11-focal AS verifier-lib

ARG VERIFIER_LIB_COMMIT=ac71fb3

WORKDIR /opt/release-verifier-lib

RUN git clone --quiet https://github.com/reactome/release-verifier-lib.git . && \
    git checkout --quiet ${VERIFIER_LIB_COMMIT} && \
    mvn -B -q install -DskipTests -Dmaven.javadoc.skip=true -Dgpg.skip=true


# ===== stage 1 =====
FROM maven:3.9.6-eclipse-temurin-11-focal AS setup-env

ARG REPO_DIR

COPY --from=verifier-lib /root/.m2/repository/org/reactome/release/verifier \
     /root/.m2/repository/org/reactome/release/verifier

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

# -nsu: keep the verifier-lib installed above, not the published snapshot.
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
