# Biorelation Neo4j unmanaged extension

[![DOI](https://zenodo.org/badge/27435268.svg)](https://zenodo.org/badge/latestdoi/27435268)

This is an unmanaged extension. Details at: http://neo4j.com/docs/stable/server-unmanaged-extensions.html 

## REQUIREMENTS

You would need to import NCBI taxonomy, Gene Ontology and UniProt into your Neo4j Database. The scripts and instructions can be found at scripts directory in this repository.

* Java 17 (or 21)
* Maven >= 3.6
* Compatible with Neo4j 5.x (developed and built against 5.26 LTS)

> For the legacy Neo4j 3.5.x build, see the `master` branch.

## INSTALL

1. Build it: 

    mvn clean package

2. Copy target/neo4j-biorelation-0.3.x.jar to the plugins/ directory of your Neo4j server.

   The jar is shaded (it bundles minimal-json and commons-lang3), so no extra jars need to be copied into the Neo4j lib/ directory.

3. Configure Neo4j by adding a line to conf/neo4j.conf:

    server.unmanaged_extension_classes=cat.cau.neo4j.biorelation.rest=/biodb

   (In Neo4j 5 this setting was renamed from `dbms.unmanaged_extension_classes`.)

In this case, queries will need biodb prepended.

4. Start Neo4j server.

5. Query it over HTTP:

	* curl http://localhost:7474/biodb/helloworld -> Dummy
	* curl http://localhost:7474/biodb/distance/tax/9606/10114 -> Distance (hops) between human and guinea pig
	* curl http://localhost:7474/biodb/path/tax/9606/10114 -> Path between human and guinea pig
	* curl http://localhost:7474/biodb/common/tax/9606-10114 -> LCA between human and guinea pig. Many values allowed
	* curl http://localhost:7474/biodb/distance/go/GO:0004180/GO:0004866 -> Distance (hops) between carboxypeptidase and endopeptidase GOs
	* curl http://localhost:7474/biodb/path/go/GO:0004180/GO:0004866 -> Path between carboxypeptidase and endopeptidase GOs
	* curl http://localhost:7474/biodb/common/go/GO:0004180-GO:0004866 LCA between carboxypeptidase and endopeptidase GOs
	* curl http://localhost:7474/biodb/rels/go/Q96IY4 -> GO information about Q96IY4
	* curl http://localhost:7474/biodb/rels/tax/Q96IY4 -> Taxon information about Q96IY4

## Docker

* [CentOS version](https://hub.docker.com/r/toniher/neo4j-biorelation-centos/)


## Data population scripts

We provide data population scripts at ```population``` directory for sake of convenience. They are compatible with provided Docker versions.


## TODO

* Simplify functions

* Add Molecular interaction controlled vocabulary https://www.ebi.ac.uk/ols/ontologies/mi

* Restrict a bit more path finding
	* Allow different GO options relations (is_a, part_of, etc.)

    MATCH (n:TAXID { id:9604 })<-[r*]-(m:TAXID)
    where not(()-->m)
    return distinct m;


* Make tests work.
* root working in all cases
* Dealing with exceptions

    (m:MOL)-[has_annotation]-(a:annotation)-[has_go]-(g:GO_TERM)
            (a:annotation)-[has_evidence]-(e:ECO_TERM)
            (a:annotation)-[has_goref]-(r:GOREF_TERM)

    * ECO: https://www.ebi.ac.uk/ols/ontologies/eco
    * GO_REF: http://www.geneontology.org/cgi-bin/references.cgi

