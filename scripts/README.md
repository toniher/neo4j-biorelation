Scripts for importing NCBI Taxonomy and GeneOntology data to Neo4j

* neo4j2-go.py
    * http://www.geneontology.org/GO.downloads.database.shtml (mysql dump files)
	* Argument files: term.txt, term_definition.txt, term2term.txt
* neo4j2-ncbi.py
	* ftp://ftp.ncbi.nih.gov/pub/taxonomy/
	* Argument files: nodes.dmp, names.dmp

* uniprot.sh (queries to speed up relationships addition for UniProt entries, to be used with: https://github.com/jexp/neo4j-shell-tools)

