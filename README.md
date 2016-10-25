# Biorelation Neo4j unmanaged extension

[![DOI](https://zenodo.org/badge/27435268.svg)](https://zenodo.org/badge/latestdoi/27435268)

This is an unmanaged extension. Details at: http://neo4j.com/docs/stable/server-unmanaged-extensions.html 

## REQUIREMENTS

You would need to import NCBI taxonomy, Gene Ontology and UniProt into your Neo4j Database. The scripts and instructions can be found at scripts directory in this repository.

* Java 1.8
* Maven >= 3.1
* Compatible with Neo4j 3.0.6

## INSTALL

1. Build it: 

    mvn clean package

2. Copy target/neo4j-biorelation-0.2.x.jar to the plugins/ directory of your Neo4j server.

3. Configure Neo4j by adding a line to conf/neo4j.conf:

    dbms.unmanaged_extension_classes=cat.cau.neo4j.biorelation.rest=/biodb

In this case, queries will need biodb prepended.

You likely need to add further packages. For instance, copying [minimal-json](http://mvnrepository.com/artifact/com.eclipsesource.minimal-json) to system/lib Neo4j directory.

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

## TODO

* Simplify functions

* Restrict a bit more path finding
	* Allow different GO options relations (is_a, part_of, etc.)

    MATCH (n:TAXID { id:9604 })<-[r*]-(m:TAXID)
    where not(()-->m)
    return distinct m;


* Make tests work.

* root working in all cases
* Dealing with exceptions



