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


$NEO4JADMIN import --delimiter=TAB --id-type=STRING --nodes:GO=$GONODES --relationships=$GORELS


