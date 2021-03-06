# Neo4j
NEO4JSHELL=/var/lib/neo4j/bin/cypher-shell
NEO4JADMIN=/var/lib/neo4j/bin/neo4j-admin

# GO URLs
GOURL=http://archive.geneontology.org/latest-lite/go_weekly-assocdb-tables.tar.gz
GODIR=/scratch/go/

# Taxonomy URLs
TAXURL=ftp://ftp.ncbi.nlm.nih.gov/pub/taxonomy/taxdump.tar.gz
TAXDIR=/scratch/taxonomy

# GOA URLs
GOAURL=ftp://ftp.ebi.ac.uk/pub/databases/GO/goa/UNIPROT/goa_uniprot_all.gpa.gz
GOADIR=/scratch/goa
INFOURL=ftp://ftp.ebi.ac.uk/pub/databases/GO/goa/UNIPROT/goa_uniprot_all.gpi.gz
INFOFILE=goa_uniprot_all.gpi
GOAFILE=goa_uniprot_all.gpa

# ISOFORM
ISOURL=ftp://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/complete/uniprot_sprot_varsplic.fasta.gz
ISOFILE=uniprot_sprot_varsplic.fasta
ISODIR=/scratch/iso

# IntAct
INTACTURL=ftp://ftp.ebi.ac.uk/pub/databases/intact/current/psimitab/intact.txt
INTACTDIR=/scratch/intact

# IDmapping URL
# IDMAPURL=ftp://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/idmapping/idmapping.dat.gz
# IDMAPDIR=/scratch/idmapping
# IDMAPFILEPATH=/scratch/idmappingall.csv

# Temp dirs
MOMENTDIR=/scratch/moment
SCRIPTPATH=`pwd`
TMPDIR=/scratch/tmp

# Docker populate vars
SCRATCHDIR=/scratch/neo4j
DOCKERNAME=neo4jbio

# If download if exists
DOWNIFEXISTS=0
