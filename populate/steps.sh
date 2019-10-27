#!/bin/bash

set -ueo pipefail

# CONFIG parameters

set -a 

. ./vars.sh

set +a

mkdir -p $GODIR
cd $GODIR

curl --fail --silent --show-error --location --remote-name $GOURL

tar zxf go_weekly-assocdb-tables.tar.gz
rm go_weekly-assocdb-tables.tar.gz

cd $SCRIPTPATH

mkdir -p $MOMENTDIR

# merge term.txt with term_definition in below
GONODES=$MOMENTDIR/gonodes.csv
GORELS=$MOMENTDIR/gorels.csv

# Prepare nodes
python generateGOnodes.py $GODIR/go_weekly-assocdb-tables/term.txt $GODIR/go_weekly-assocdb-tables/term_definition.txt > $GONODES

# replace term2term.txt to a version with 3 column and with rel replaced with its name version
python generateGOrels.py $GODIR/go_weekly-assocdb-tables/term.txt $GODIR/go_weekly-assocdb-tables/term2term.txt > $GORELS


mkdir -p $TAXDIR
cd $TAXDIR

curl --fail --silent --show-error --location --remote-name $TAXURL
curl --fail --silent --show-error --location --remote-name $TAXURL.md5

md5sum -c taxdump.tar.gz.md5

tar zxf taxdump.tar.gz
rm taxdump.tar.gz

cd $SCRIPTPATH

mkdir -p $MOMENTDIR

# Prepare nodes and relationships

TAXNODES=$MOMENTDIR/taxnodes.csv
TAXRELS=$MOMENTDIR/taxrels.csv

python prepareTaxNodesAndRels.py $TAXDIR/nodes.dmp $TAXDIR/names.dmp $MOMENTDIR

$NEO4JADMIN import --array-delimiter=$ --delimiter=TAB --id-type=STRING --nodes:GO=$GONODES --nodes:TAXID=$TAXNODES --relationships="$NODERELS,$TAXRELS"


