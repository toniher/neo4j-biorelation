= Biorelation Neo4j unmanaged extension =

This is an unmanaged extension. Details at: http://neo4j.com/docs/stable/server-unmanaged-extensions.html 

1. Build it: 

        mvn clean package

2. Copy target/unmanaged-extension-template-1.0.jar to the plugins/ directory of your Neo4j server.

3. Configure Neo4j by adding a line to conf/neo4j-server.properties:

	org.neo4j.server.thirdparty_jaxrs_classes=cat.cau.neo4j.biorelation.rest=/biodb
	In this case, queries will need biodb prepended.

	You likely need to add further packages. For instance, copying minimal-json http://mvnrepository.com/artifact/com.eclipsesource.minimal-json to system/lib Neo4j directory.

4. Start Neo4j server.

5. Query it over HTTP:

	curl http://localhost:7474/biodb/parent/helloworld -> Dummy
	curl http://localhost:7474/biodb/parent/distance/tax/9606/10114 -> Distance (hops) between human and guinea ping
	curl http://localhost:7474/biodb/parent/common/tax/9606-10114 -> LCA between human and guinea pig. Many values allowed
	curl http://localhost:7474/biodb/parent/distance/go/GO:0004180/GO:0004866 -> Distance (hops) between carboxypeptidase and endopeptidase GOs
	curl http://localhost:7474/biodb/parent/common/go/GO:0004180-GO:0004866 LCA between carboxypeptidase and endopeptidase GOs

== TODO ==

* Make tests work.
* Code cleaning
* Add further functionality out-of-the-box



