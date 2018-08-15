set -ueo pipefail

# CONFIG parameters
NEO4JSHELL=/data/soft/neo4j-community-3.4.5/bin/cypher-shell
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


#echo "CREATE CONSTRAINT ON (n:TAXID) ASSERT n.id IS UNIQUE"
#$NEO4JSHELL "CREATE CONSTRAINT ON (n:TAXID) ASSERT n.id IS UNIQUE"



python neo4j2-import-ncbi.py $TAXDIR/nodes.dmp $TAXDIR/names.dmp

# Prepare nodes and relationships

TAXNODES=$MOMENTDIR/taxnodes.csv
TAXRELS=$MOMENTDIR/taxrels.csv

python prepareTaxNodesAndRels.py $TAXDIR/nodes.dmp $TAXDIR/names.dmp $MOMENTDIR

#echo "CALL apoc.periodic.iterate(\"CALL apoc.load.csv('${TAXNODES}', { sep:'\t', header:true, mapping:{id:{type:'int'}, name:{array:true,arraySep:'?'} } } ) yield map as row return row\",\"CREATE (p:TAXID) SET p = row\",{batchSize:25000, iterateList:true, parallel:true});"
#$NEO4JSHELL "CALL apoc.periodic.iterate(\"CALL apoc.load.csv('${TAXNODES}', { sep:'\t', header:true, mapping:{id:{type:'int'}, name:{array:true,arraySep:'?'} } } ) yield map as row return row\",\"CREATE (p:TAXID) SET p = row\",{batchSize:25000, iterateList:true, parallel:true});" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err


echo "LOAD CSV WITH HEADERS FROM \"file://${TAXRELS}\" AS row FIELDTERMINATOR \"\t\" MATCH (c:TAXID { id:toInt( row.start )} ), (p:TAXID { id:toInt( row.end ) } ) call apoc.merge.relationship(c,row.rel,{},{},p) yield rel return count(*) ;"
$NEO4JSHELL "LOAD CSV WITH HEADERS FROM \"file://${TAXRELS}\" AS row FIELDTERMINATOR \"\t\" MATCH (c:TAXID { id:toInt( row.start )} ), (p:TAXID { id:toInt( row.end ) } ) call apoc.merge.relationship(c,row.rel,{},{},p) yield rel return count(*) ;" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err


echo "CREATE INDEX ON :TAXID(rank)"
$NEO4JSHELL "CREATE INDEX ON :TAXID(rank)"
echo "CREATE INDEX ON :TAXID(scientific_name)"
$NEO4JSHELL "CREATE INDEX ON :TAXID(scientific_name)"
echo "CREATE INDEX ON :TAXID(name)"
$NEO4JSHELL "CREATE INDEX ON :TAXID(name)"
