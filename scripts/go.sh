set -ueo pipefail

# CONFIG parameters

GOURL=http://archive.geneontology.org/latest-lite/go_weekly-seqdb-tables.tar.gz
GODIR=/data/db/go/
MOMENTDIR=/data/toniher
SCRIPTPATH=`pwd`

TMPDIR=/data/tmp


cd $GODIR

wget -c -t0 $GOURL

tar zxf go_weekly-seqdb-tables.tar.gz
rm go_weekly-seqdb-tables.tar.gz

cd $SCRIPTPATH

python neo4j2-import-go.py $GODIR/go_weekly-seqdb-tables/term.txt  $GODIR/go_weekly-seqdb-tables/term_definition.txt $GODIR/go_weekly-seqdb-tables/term2term.txt



