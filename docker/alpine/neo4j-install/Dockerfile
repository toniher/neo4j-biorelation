FROM openjdk:13-jdk-alpine

ARG NEO4J_VERSION=3.5.7
ARG NEO4J_BIORELATION_VERSION=0.2.3
ARG NEO4J_APOC_VERSION=3.5.0.4
ARG MINIMAL_JSON_VERSION=0.9.5

ARG MAVEN_VERSION=3.5.4
ARG MAVEN_SHA=ce50b1c91364cb77efe3776f756a6d92b76d9038b0a0782f7d53acf1e997a14d
ARG MAVEN_BASE_URL=https://apache.osuosl.org/maven/maven-3/${MAVEN_VERSION}/binaries

ENV NEO4J_PATH=/var/lib/neo4j

# RUN addgroup -S neo4j && adduser -S -H -h /var/lib/neo4j -G neo4j neo4j

# Install Neo4j
# From official Neo4j

ENV NEO4J_TARBALL=neo4j-community-${NEO4J_VERSION}-unix.tar.gz
ARG NEO4J_URI=http://dist.neo4j.org/neo4j-community-${NEO4J_VERSION}-unix.tar.gz
ENV NEO4J_SHA256=31c8f398df9342928502a7321f045eb8f2b96ec4ded26f9f057dc6f6fa10c60c

RUN apk add --no-cache curl

RUN curl --fail --silent --show-error --location --remote-name ${NEO4J_URI} \
    && echo "${NEO4J_SHA256}  ${NEO4J_TARBALL}" | sha256sum -csw - \
    && tar --extract --file ${NEO4J_TARBALL} --directory /var/lib \
    && mv /var/lib/neo4j-* /var/lib/neo4j \
    && rm ${NEO4J_TARBALL}

ENV PATH $NEO4J_PATH/bin:$PATH

WORKDIR $NEO4J_PATH

RUN mv data /data \
    && ln -s /data

VOLUME /data

# Extra processing
ENV NEO4J_SHELL_BASE /tmp

WORKDIR $NEO4J_SHELL_BASE

RUN mkdir -p /usr/share/maven /usr/share/maven/ref \
  && curl -fsSL -o /tmp/apache-maven.tar.gz ${MAVEN_BASE_URL}/apache-maven-${MAVEN_VERSION}-bin.tar.gz \
  && echo "${MAVEN_SHA}  /tmp/apache-maven.tar.gz" | sha256sum -c - \
  && tar -xzf /tmp/apache-maven.tar.gz -C /usr/share/maven --strip-components=1 \
  && rm -f /tmp/apache-maven.tar.gz \
&& ln -s /usr/share/maven/bin/mvn /usr/bin/mvn

RUN apk add --no-cache git

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

EXPOSE 7474 7473 7687 1337

RUN apk add --no-cache tini su-exec bash python-dev py-pip build-base
RUN apk add --no-cache libffi-dev openssl-dev

RUN pip install neobolt py2neo

RUN apk add --no-cache perl

COPY docker-entrypoint.sh /docker-entrypoint.sh

ENV NEO4J_EDITION=community

ENTRYPOINT ["/sbin/tini", "-g", "--", "/docker-entrypoint.sh"]

CMD ["neo4j"]
