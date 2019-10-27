set -ueo pipefail

# CONFIG parameters
set -a

. ./vars.sh

set +a

mkdir -p $TAXDIR
cd $TAXDIR

curl --fail --silent --show-error --location --remote-name $TAXURL
curl --fail --silent --show-error --location --remote-name $TAXURL.md5

md5sum -c taxdump.tar.gz.md5

tar zxf taxdump.tar.gz
rm taxdump.tar.gz

cd $SCRIPTPATH

mkdir -p $MOMENTDIR

#
#echo "CREATE CONSTRAINT ON (n:TAXID) ASSERT n.id IS UNIQUE"
#$NEO4JSHELL "CREATE CONSTRAINT ON (n:TAXID) ASSERT n.id IS UNIQUE"


# python neo4j2-import-ncbi.py $TAXDIR/nodes.dmp $TAXDIR/names.dmp

# Prepare nodes and relationships

TAXNODES=$MOMENTDIR/taxnodes.csv
TAXRELS=$MOMENTDIR/taxrels.csv

python prepareTaxNodesAndRels.py $TAXDIR/nodes.dmp $TAXDIR/names.dmp $MOMENTDIR

$NEO4JADMIN import --array-delimiter=$ --delimiter=TAB --id-type=STRING --nodes:TAXID=$TAXNODES --relationships=$TAXRELS

#echo "CREATE INDEX ON :TAXID(rank)"
#$NEO4JSHELL "CREATE INDEX ON :TAXID(rank)"
#echo "CREATE INDEX ON :TAXID(scientific_name)"
#$NEO4JSHELL "CREATE INDEX ON :TAXID(scientific_name)"
#echo "CREATE INDEX ON :TAXID(name)"
#$NEO4JSHELL "CREATE INDEX ON :TAXID(name)"
