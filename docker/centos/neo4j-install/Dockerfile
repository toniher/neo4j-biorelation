# Neo4j 5 requires Java 17 (or 21). The base image must provide it.
FROM eclipse-temurin:21-jdk

ARG NEO4J_VERSION=5.26.21
ARG NEO4J_BIORELATION_VERSION=0.3.0
ARG NEO4J_APOC_VERSION=5.26.11
ENV NEO4J_SHA256=
ENV NEO4J_PATH=/var/lib/neo4j

# Install Neo4j
# From official Neo4j

ENV NEO4J_TARBALL=neo4j-community-${NEO4J_VERSION}-unix.tar.gz
ARG NEO4J_URI=https://dist.neo4j.org/neo4j-community-${NEO4J_VERSION}-unix.tar.gz

RUN curl --fail --silent --show-error --location --remote-name ${NEO4J_URI} \
    && tar --extract --file ${NEO4J_TARBALL} --directory /var/lib \
    && mv /var/lib/neo4j-* /var/lib/neo4j \
    && rm ${NEO4J_TARBALL}

WORKDIR $NEO4J_PATH

RUN mv data /data \
    && ln -s /data

VOLUME /data

# Extra processing
ENV NEO4J_SHELL_BASE=/tmp

# Build tools (Maven) to compile the extension
RUN apt-get update && apt-get install -y --no-install-recommends maven git python3 python3-pip \
    && rm -rf /var/lib/apt/lists/*

WORKDIR $NEO4J_SHELL_BASE

# Install Neo4j Biorelation.
# The jar is shaded (bundles minimal-json + commons-lang3), so nothing extra
# needs to be copied into lib/.
RUN git clone https://github.com/toniher/neo4j-biorelation
RUN cd neo4j-biorelation; git checkout neo4j5; mvn clean package
RUN cp neo4j-biorelation/target/neo4j-biorelation-$NEO4J_BIORELATION_VERSION.jar $NEO4J_PATH/plugins

# APOC (core) for Neo4j 5
RUN cd $NEO4J_PATH/plugins; curl --silent --show-error --location --remote-name https://github.com/neo4j/apoc/releases/download/${NEO4J_APOC_VERSION}/apoc-${NEO4J_APOC_VERSION}-core.jar

WORKDIR $NEO4J_PATH

# Volume from where to import files
VOLUME /scratch

# Ensure LOAD CSV always works
RUN cd import; ln -s /scratch .

COPY docker-entrypoint.sh /docker-entrypoint.sh

# Installing Python libraries
COPY requirements.txt /
RUN pip3 install --break-system-packages -r /requirements.txt

EXPOSE 7474 7473 7687

# For allowing bulk import we don't run it
#ENTRYPOINT ["/docker-entrypoint.sh"]
#CMD ["neo4j"]
