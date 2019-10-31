#!/bin/bash

set -ueo pipefail

# CONFIG parameters

set -a 
. ./vars.sh
set +a

mkdir -p $TMPDIR

echo "GO processing"

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

echo "TAXONOMY processing"

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

echo "GOA processing"

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
cut -f 2,4,6 $INFOFILE | perl -F'\t' -lane ' if ($F[0]!~/^\!/ ) { $F[1]=~s/\"/\\"/g; print join( "\t", @F[0..2] ); } ' > $INFOFILE.protein
cut -f 2,4,6 $INFOFILE | perl -F'\t' -lane ' if ($F[0]!~/^\!/ ) { print join( "\t", @F[0..2] ); } ' > $INFOFILE.protein

echo -e "id:ID\tname\ttype" |cat - $INFOFILE.protein > $MOMENTDIR/tempfile && mv $MOMENTDIR/tempfile $INFOFILE.protein

# Adding relationships to Taxon
cut -f 2,7 $INFOFILE | perl -F'\t' -lane ' if ($F[0]!~/^\!/ && $F[1]=~/^taxon/ ) { my $id=$F[0]; my $tax=$F[1]; $tax=~s/taxon\://g; print $id, "\t", "TAXID:".$tax."\thas_taxon"; } ' > $INFOFILE.reduced

echo -e "MOL:START_ID\tTAXID:END_ID\t:TYPE" |cat - $INFOFILE.reduced > $MOMENTDIR/tempfile && mv $MOMENTDIR/tempfile $INFOFILE.reduced

# Adding relationships to GOA
cut -f 2,3,4,5,6 $GOAFILE  | perl -F'\t' -lane ' if ($F[0]!~/^(\!|gpa-)/ ) { print join( "\t", @F[0..4] )."\thas_go"; } ' > $GOAFILE.pre

python $SCRIPTPATH/mapGOidsInGOA.py $GONODES $GOAFILE.pre > $GOAFILE.reduced

echo -e "MOL:START_ID\tqualifier\tGO:END_ID\tref\tevidence\t:TYPE" |cat - $GOAFILE.reduced > $MOMENTDIR/tempfile && mv $MOMENTDIR/tempfile $GOAFILE.reduced


echo "ISOFORMS processing"

mkdir -p $ISODIR

cd $ISODIR

if [ "$DOWNIFEXISTS" -eq "1" ]; then
	
	if [ -f $ISOFILE ]; then
		rm $ISOFILE
	fi

	curl --fail --silent --show-error --location --remote-name $ISOURL
	gunzip $ISOFILE.gz

fi

perl -lane 'if ( $_=~/^\>\w+\|(\S+)\|/ ) { print $1; } '  $ISOFILE |sort -u > isoforms.pre.txt
python $SCRIPTPATH/processIsoformsNames.py isoforms.pre.txt > isoforms.proc.txt
perl -lane 'print $F[0]."\t".$F[1]."\t"."isoform_of"; ' isoforms.proc.txt > isoform-rels.txt
rm isoforms.pre.txt isoforms.proc.txt

cut -f1 $ISODIR/isoform-rels.txt | sort -u > $ISODIR/isoforms.txt;
cut -f1 ${GOADIR}/${INFOFILE}.protein | grep '-' | sort -u > $ISODIR/isoforms.already.txt
comm -23 $ISODIR/isoforms.txt $ISODIR/isoforms.already.txt > $MOMENTDIR/tempfile && mv $MOMENTDIR/tempfile $ISODIR/isoforms.txt

perl -lane 'print "$F[0]\tprotein";' $ISODIR/isoforms.txt > $MOMENTDIR/tempfile && mv $MOMENTDIR/tempfile $ISODIR/isoforms.txt

rm $ISODIR/isoforms.already.txt

echo -e "id:ID\ttype" |cat - $ISODIR/isoforms.txt > $MOMENTDIR/tempfile && mv $MOMENTDIR/tempfile $ISODIR/isoforms.txt

echo -e "MOL:START_ID\tMOL:END_ID\t:TYPE" |cat - $ISODIR/isoform-rels.txt > $MOMENTDIR/tempfile && mv $MOMENTDIR/tempfile $ISODIR/isoform-rels.txt

echo "IMPORT ALL"

cd $TMPDIR

$NEO4JADMIN import --ignore-missing-nodes=true --array-delimiter=$ --delimiter=TAB --id-type=STRING \
					--nodes:GO=$GONODES --nodes:TAXID=$TAXNODES \
					--nodes:MOL=${GOADIR}/${INFOFILE}.protein --nodes:MOL=$ISODIR/isoforms.txt \
                    --relationships=$GORELS --relationships=$TAXRELS \
					--relationships=${GOADIR}/${INFOFILE}.reduced --relationships=${GOADIR}/${GOAFILE}.reduced \
					--relationships=$ISODIR/isoform-rels.txt


# rm go_weekly-assocdb-tables.tar.gz
# rm taxdump.tar.gz
#rm $GOAFILE.pre

