FROM biocorecrg/centos-perlbrew-pyenv-java:centos7

ARG NEO4J_BIORELATION_VERSION=0.2.3
ENV NEO4J_PATH /var/lib/neo4j
ENV NEO4J_SHELL_BIN $NEO4J_PATH/bin/neo4j-shell

# Install Neo4j
# From official Neo4j

ENV NEO4J_SHA256=af53823776645e11d04436a513368e7e417b515572d6228da6b2977c8490ffbb \
    NEO4J_TARBALL=neo4j-community-3.4.5-unix.tar.gz
ARG NEO4J_URI=http://dist.neo4j.org/neo4j-community-3.4.5-unix.tar.gz

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

# Install JSON minimal
RUN cd $NEO4J_PATH/lib; curl --fail --silent --show-error --location --remote-name http://central.maven.org/maven2/com/eclipsesource/minimal-json/minimal-json/0.9.5/minimal-json-0.9.5.jar 

WORKDIR $NEO4J_PATH

VOLUME /scratch

COPY docker-entrypoint.sh /docker-entrypoint.sh

EXPOSE 7474 7473 7687 1337

ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["neo4j"]
