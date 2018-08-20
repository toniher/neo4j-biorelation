Scripts for importing NCBI Taxonomy and GeneOntology data to Neo4j

# Directly using LOAD CSV

* go.sh
* taxonomy.sh
* goa.sh


# Using py2neo

It may be faster in some cases

* neo4j2-import-go.py
    * http://www.geneontology.org/GO.downloads.database.shtml (mysql dump files)
	* Argument files: term.txt, term_definition.txt, term2term.txt
* neo4j2-import-ncbi.py
	* ftp://ftp.ncbi.nih.gov/pub/taxonomy/
	* Argument files: nodes.dmp, names.dmp


