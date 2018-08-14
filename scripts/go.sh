#!/bin/bash

set -ueo pipefail

# CONFIG parameters

NEO4JSHELL=/data/soft/neo4j-community-3.4.5/bin/cypher-shell
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

echo "CREATE CONSTRAINT ON (n:GO_TERM) ASSERT n.acc IS UNIQUE"
$NEO4JSHELL "CREATE CONSTRAINT ON (n:GO_TERM) ASSERT n.acc IS UNIQUE"
echo "CREATE CONSTRAINT ON (n:GO_TERM) ASSERT n.id IS UNIQUE"
$NEO4JSHELL "CREATE CONSTRAINT ON (n:GO_TERM) ASSERT n.id IS UNIQUE"


# merge term.txt with term_definition in below
GONODES=$MOMENTDIR/gonodes.csv
GORELS=$MOMENTDIR/gorels.csv

# Prepare nodes
python generateGOnodes.py $GODIR/go_weekly-seqdb-tables/term.txt $GODIR/go_weekly-seqdb-tables/term_definition.txt > $GONODES

# echo "LOAD CSV WITH HEADERS FROM \"file://${GONODES}\" AS row FIELDTERMINATOR \"\t\" CREATE (n:GO_TERM) SET n = row ;"
# $NEO4JSHELL "LOAD CSV WITH HEADERS FROM \"file://${GONODES}\" AS row FIELDTERMINATOR \"\t\" CREATE (n:GO_TERM) SET n = row ;" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err

# Using APOC

echo "CALL apoc.periodic.iterate(\"CALL apoc.load.csv('${GONODES}', { sep:'\t', header:true, mapping:{id:{type:'int'} } } ) yield map as row return row\",\"CREATE (p:GO_TERM) SET p = row\",{batchSize:10000, iterateList:true, parallel:true});"
$NEO4JSHELL "CALL apoc.periodic.iterate(\"CALL apoc.load.csv('${GONODES}', { sep:'\t', header:true, mapping:{id:{type:'int'} } } ) yield map as row return row\",\"CREATE (p:GO_TERM) SET p = row\",{batchSize:10000, iterateList:true, parallel:true});"


# replace term2term.txt to a version with 3 column and with rel replaced with its name version
python generateGOrels.py $GODIR/go_weekly-seqdb-tables/term.txt $GODIR/go_weekly-seqdb-tables/term2term.txt > $GORELS

#python importGOrels.py $GODIR/go_weekly-seqdb-tables/term.txt $GODIR/go_weekly-seqdb-tables/term2term.txt

echo "LOAD CSV WITH HEADERS FROM \"file://${GORELS}\" AS row FIELDTERMINATOR \"\t\" MATCH (c:GO_TERM { id:toInt( row.target )} ), (p:GO_TERM { id:toInt( row.source ) } ) CREATE (c)-[:row.rel]->(p) call apoc.merge.relationship(c,row.rel,{},{},p) yield rel return count(*) ;"

$NEO4JSHELL "LOAD CSV WITH HEADERS FROM \"file://${GORELS}\" AS row FIELDTERMINATOR \"\t\" MATCH (c:GO_TERM { id:toInt( row.target )} ), (p:GO_TERM { id:toInt( row.source ) } ) call apoc.merge.relationship(c,row.rel,{},{},p) yield rel return count(*) ;" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err





