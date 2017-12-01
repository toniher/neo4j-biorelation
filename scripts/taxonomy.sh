set -ueo pipefail

# CONFIG parameters

TAXURL=ftp://ftp.ncbi.nlm.nih.gov/pub/taxonomy/taxdump.tar.gz
TAXDIR=/data/db/go/taxonomy
MOMENTDIR=/data/toniher
SCRIPTPATH=`pwd`

TMPDIR=/data/tmp

mkdir -p $TAXDIR
cd $TAXDIR

wget -c -t0 $TAXURL
wget -c -t0 $TAXURL.md5

md5sum -c taxdump.tar.gz.md5

tar zxf taxdump.tar.gz
rm taxdump.tar.gz

cd $SCRIPTPATH

python neo4j2-import-ncbi.py $TAXDIR/nodes.dmp $TAXDIR/names.dmp




