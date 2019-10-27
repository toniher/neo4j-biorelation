#!/bin/bash

set -ueo pipefail

# CONFIG parameters

set -a 
. ./vars.sh
set +a

mkdir -p $GODIR
cd $GODIR

if [ "$DOWNIFEXISTS" -eq "1" ]; then

	if [ -d $GODIR/go_weekly-assocdb-tables ]; then
		rm -rf $GODIR/go_weekly-assocdb-tables
	fi
	
	curl --fail --silent --show-error --location --remote-name $GOURL

fi

tar zxf go_weekly-assocdb-tables.tar.gz

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

if [ "$DOWNIFEXISTS" -eq "1" ]; then

	if [ -f $TAXDIR/nodes.dmp ]; then
		rm -rf $TAXDIR/*
	fi

	curl --fail --silent --show-error --location --remote-name $TAXURL
	curl --fail --silent --show-error --location --remote-name $TAXURL.md5

fi

md5sum -c taxdump.tar.gz.md5

tar zxf taxdump.tar.gz

cd $SCRIPTPATH

mkdir -p $MOMENTDIR

# Prepare nodes and relationships

TAXNODES=$MOMENTDIR/taxnodes.csv
TAXRELS=$MOMENTDIR/taxrels.csv

python prepareTaxNodesAndRels.py $TAXDIR/nodes.dmp $TAXDIR/names.dmp $MOMENTDIR

mkdir -p $GOADIR

# Let's uncompress all files
cd $GOADIR

if [ "$DOWNIFEXISTS" -eq "1" ]; then

	if [ -f $GOAFILE ]; then
		rm $GOAFILE
	fi

	if [ -f $INFOFILE ]; then
		rm $INFOFILE
	fi

	curl --fail --silent --show-error --location --remote-name $GOAURL
	curl --fail --silent --show-error --location --remote-name $INFOURL
	gunzip *gz
	
fi

# Base entries
# cut -f 2,4,6 $INFOFILE | perl -F'\t' -lane ' if ($F[0]!~/^\!/ ) { $F[1]=~s/\"/\\"/g; print join( "\t", @F[0..2] ); } ' > $INFOFILE.protein
cut -f 2,4,6 $INFOFILE | perl -F'\t' -lane ' if ($F[0]!~/^\!/ ) { print join( "\t", @F[0..2] ); } ' > $INFOFILE.protein

echo -e "id:ID\tname\ttype" |cat - $$INFOFILE.protein > $MOMENTDIR/tempfile && mv $MOMENTDIR/tempfile $INFOFILE.protein

# Adding relationships to Taxon
cut -f 2,7 $INFOFILE | perl -F'\t' -lane ' if ($F[0]!~/^\!/ && $F[1]=~/^taxon/ ) { my $id=$F[0]; my $tax=$F[1]; $tax=~s/taxon\://g; print $id, "\t", "TAXID:".$tax; } ' > $INFOFILE.reduced

echo -e "MOL:START_ID\tTAXID:END_ID" |cat - $INFOFILE.reduced > $MOMENTDIR/tempfile && mv $MOMENTDIR/tempfile $INFOFILE.reduced

# Adding relationships to GOA
cut -f 2,3,4,5,6 $GOAFILE  | perl -F'\t' -lane ' if ($F[0]!~/^(\!|gpa-)/ ) { print join( "\t", @F[0..4] ); } ' > $GOAFILE.pre

python mapGOidsInGOA.py $GOAFILE.pre $GONODES > $GOAFILE.reduced


echo -e "MOL:START_ID\tqualifier\tGO:END_ID\tref\tevidence" |cat - $GOAFILE.reduced > $MOMENTDIR/tempfile && mv $MOMENTDIR/tempfile $GOAFILE.reduced


$NEO4JADMIN import --array-delimiter=$ --delimiter=TAB --id-type=STRING --nodes:GO=$GONODES --nodes:TAXID=$TAXNODES --nodes:MOL=$INFOFILE.protein \
									 --relationships=$GORELS --relationships=$TAXRELS --relationships=$INFOFILE.reduced --relationships=$GOAFILE.reduced


# rm go_weekly-assocdb-tables.tar.gz
# rm taxdump.tar.gz
rm $GOAFILE.pre

