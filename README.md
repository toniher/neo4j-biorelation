# Biorelation Neo4j unmanaged extension

This is an unmanaged extension. Details at: http://neo4j.com/docs/stable/server-unmanaged-extensions.html 

## REQUIREMENTS

You would need to import NCBI taxonomy and Gene Ontology into your Neo4j Database. The following Py2Neo scripts are used: https://github.com/toniher/biomirror/tree/master/neo4j


## INSTALL

1. Build it: 

    mvn clean package

2. Copy target/neo4j-biorelation-0.1.jar to the plugins/ directory of your Neo4j server.

3. Configure Neo4j by adding a line to conf/neo4j-server.properties:

    org.neo4j.server.thirdparty_jaxrs_classes=cat.cau.neo4j.biorelation.rest=/biodb

In this case, queries will need biodb prepended.

You likely need to add further packages. For instance, copying [minimal-json](http://mvnrepository.com/artifact/com.eclipsesource.minimal-json) to system/lib Neo4j directory.

4. Start Neo4j server.

5. Query it over HTTP:

    * curl http://localhost:7474/biodb/parent/helloworld -> Dummy
    * curl http://localhost:7474/biodb/parent/distance/tax/9606/10114 -> Distance (hops) between human and guinea pig
    * curl http://localhost:7474/biodb/parent/path/tax/9606/10114 -> Path between human and guinea pig
    * curl http://localhost:7474/biodb/parent/common/tax/9606-10114 -> LCA between human and guinea pig. Many values allowed
    * curl http://localhost:7474/biodb/parent/distance/go/GO:0004180/GO:0004866 -> Distance (hops) between carboxypeptidase and endopeptidase GOs
    * curl http://localhost:7474/biodb/parent/path/go/GO:0004180/GO:0004866 -> Path between carboxypeptidase and endopeptidase GOs
    * curl http://localhost:7474/biodb/parent/common/go/GO:0004180-GO:0004866 LCA between carboxypeptidase and endopeptidase GOs


## TODO

* Simplify functions

* Restrict a bit more path finding
	* Allow different GO options relations (is_a, part_of, etc.)

    MATCH (n:TAXID { id:9604 })<-[r*]-(m:TAXID)
    where not(()-->m)
    return distinct m;

* curl http://localhost:7474/biodb/parent/rels/go/MOLID -> GO information about this MOLID
* curl http://localhost:7474/biodb/parent/rels/tax/MOLID -> Taxon information about this MOLID


* Make tests work.

* root working in all cases
* Dealing with exceptions



