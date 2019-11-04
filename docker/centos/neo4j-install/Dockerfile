FROM biocorecrg/centos-perlbrew-pyenv-java:centos7

ARG NEO4J_VERSION=3.5.12
ARG NEO4J_BIORELATION_VERSION=0.2.3
ARG NEO4J_APOC_VERSION=3.5.0.5
ARG MINIMAL_JSON_VERSION=0.9.5
ENV NEO4J_SHA256=3b82b72a211d1b628b64f18f4af9f10b12ed6e2fd0ad94910e11b7fef5d0d86c 
ENV NEO4J_PATH /var/lib/neo4j

# Install Neo4j
# From official Neo4j

ENV NEO4J_TARBALL=neo4j-community-${NEO4J_VERSION}-unix.tar.gz
ARG NEO4J_URI=http://dist.neo4j.org/neo4j-community-${NEO4J_VERSION}-unix.tar.gz

RUN curl --fail --silent --show-error --location --remote-name ${NEO4J_URI} \
    && echo "${NEO4J_SHA256}  ${NEO4J_TARBALL}" | sha256sum --status -cw - \
    && tar --extract --file ${NEO4J_TARBALL} --directory /var/lib \
    && mv /var/lib/neo4j-* /var/lib/neo4j \
    && rm ${NEO4J_TARBALL}

WORKDIR $NEO4J_PATH

RUN mv data /data \
    && ln -s /data

VOLUME /data

# Extra processing
ENV NEO4J_SHELL_BASE /tmp

WORKDIR $NEO4J_SHELL_BASE

# Install Neo4j Biorelation
RUN git clone https://github.com/toniher/neo4j-biorelation
RUN cd neo4j-biorelation; mvn clean package
RUN cp neo4j-biorelation/target/neo4j-biorelation-$NEO4J_BIORELATION_VERSION.jar $NEO4J_PATH/plugins

# APOC
RUN cd $NEO4J_PATH/plugins; curl --silent --show-error --location --remote-name https://github.com/neo4j-contrib/neo4j-apoc-procedures/releases/download/${NEO4J_APOC_VERSION}/apoc-${NEO4J_APOC_VERSION}-all.jar

# Install JSON minimal
RUN cd $NEO4J_PATH/lib; curl --silent --show-error --location --remote-name http://central.maven.org/maven2/com/eclipsesource/minimal-json/minimal-json/${MINIMAL_JSON_VERSION}/minimal-json-${MINIMAL_JSON_VERSION}.jar 

WORKDIR $NEO4J_PATH

# Volume from where to import files
VOLUME /scratch

# Ensure always work LOAD CSV
RUN cd import; ln -s /scratch .

COPY docker-entrypoint.sh /docker-entrypoint.sh

# Installing Python libraries
RUN pip install neobolt py2neo

EXPOSE 7474 7473 7687 1337

# For allowing bulk import we don't run it
#ENTRYPOINT ["/docker-entrypoint.sh"]
#CMD ["neo4j"]
