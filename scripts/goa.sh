set -ueo pipefail

# CONFIG parameters

NEO4JSHELL=/data/soft/neo4j-community-3.4.5/bin/cypher-shell
GOAURL=ftp://ftp.ebi.ac.uk/pub/databases/GO/goa/UNIPROT/goa_uniprot_all.gpa.gz
GOADIR=/data/db/go/goa
MOMENTDIR=/data/toniher
SCRIPTPATH=`pwd`

INFOURL=ftp://ftp.ebi.ac.uk/pub/databases/GO/goa/UNIPROT/goa_uniprot_all.gpi.gz
INFOFILE=goa_uniprot_all.gpi
GOAFILE=goa_uniprot_all.gpa
TMPDIR=/data/tmp

mkdir -p $GOADIR

# Let's uncompress all files
cd $GOADIR
#wget -c -t0 $GOAURL
#wget -c -t0 $INFOURL
#gunzip *gz

# Base entries
# cut -f 2,4,6 $INFOFILE | perl -F'\t' -lane ' if ($F[0]!~/^\!/ ) { $F[1]=~s/\"/\\"/g; print join( "\t", @F[0..2] ); } ' > $INFOFILE.protein
cut -f 2,4,6 $INFOFILE | perl -F'\t' -lane ' if ($F[0]!~/^\!/ ) { print join( "\t", @F[0..2] ); } ' > $INFOFILE.protein

# DIR of parts
DIR=$GOADIR/mol

mkdir -p $DIR; cd $DIR; split -l 1000000 ../$INFOFILE.protein $INFOFILE.protein


echo "Preparing MOL files"

for file in $DIR/*
do
	echo -e "id\tname\ttype\t" |cat - $file > $MOMENTDIR/tempfile && mv $MOMENTDIR/tempfile $file
done

echo "CREATE CONSTRAINT ON (n:MOL) ASSERT n.id IS UNIQUE;"
$NEO4JSHELL "CREATE CONSTRAINT ON (n:MOL) ASSERT n.id IS UNIQUE;" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
echo "CREATE INDEX ON :MOL(name);"
$NEO4JSHELL "CREATE INDEX ON :MOL(name);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
echo "CREATE INDEX ON :MOL(type);"
$NEO4JSHELL "CREATE INDEX ON :MOL(type);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err

echo "Neo4j importing MOL files"

for file in $DIR/*
do
	echo $file
	echo "LOAD CSV WITH HEADERS FROM \"file://${file}\" AS row FIELDTERMINATOR \"\t\" CREATE (n:MOL) SET n = row ;"
    $NEO4JSHELL "LOAD CSV WITH HEADERS FROM \"file://${file}\" AS row FIELDTERMINATOR \"\t\" CREATE (n:MOL) SET n = row ;" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
done


cd $GOADIR


# Adding relationships to Taxon
cut -f 2,7 $INFOFILE | perl -F'\t' -lane ' if ($F[0]!~/^\!/ && $F[1]=~/^taxon/ ) { my $id=$F[0]; my $tax=$F[1]; $tax=~s/taxon\://g; print $id, "\t", $tax; } ' > $INFOFILE.reduced

# DIR of parts
DIR=$GOADIR/moltaxon

mkdir -p $DIR; cd $DIR; split -l 1000000 ../$INFOFILE.reduced $INFOFILE.reduced


echo "Preparing Taxon Files"

for file in $DIR/*
do
	echo -e "id\ttaxon" |cat - $file > $MOMENTDIR/tempfile && mv $MOMENTDIR/tempfile $file
done

for file in $DIR/*
do
	echo $file
	echo "LOAD CSV WITH HEADERS FROM \"file://${file}\" AS row FIELDTERMINATOR \"\t\" MATCH (c:MOL {id:row.id}), (p:TAXID {id:toInt( row.taxon )}) CREATE (c)-[:has_taxon]->(p) ;"
	$NEO4JSHELL "LOAD CSV WITH HEADERS FROM \"file://${file}\" AS row FIELDTERMINATOR \"\t\" MATCH (c:MOL {id:row.id}), (p:TAXID {id:toInt( row.taxon )}) CREATE (c)-[:has_taxon]->(p) ;"  >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
done

cd $GOADIR

rm $INFOFILE.reduced
rm $INFOFILE.protein

# Adding relationships to GO
cut -f 2,3,4,5,6 $GOAFILE  > $GOAFILE.reduced

#DIR of parts
DIR=$GOADIR/molgoa

mkdir -p $DIR; cd $DIR; split -l 1000000 ../$GOAFILE.reduced $GOAFILE.reduced

echo "Preparing GO files"

for file in $DIR/*
do
		echo -e "id\tqualifier\tgoacc\tref\tevidence" |cat - $file > $MOMENTDIR/tempfile && mv $MOMENTDIR/tempfile $file

done


echo "CREATE INDEX ON :has_go(evidence);"
$NEO4JSHELL "CREATE INDEX ON :has_go(evidence);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
echo "CREATE INDEX ON :has_go(ref);"
$NEO4JSHELL "CREATE INDEX ON :has_go(ref);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
echo "CREATE INDEX ON :has_go(qualifier);"
$NEO4JSHELL "CREATE INDEX ON :has_go(qualifier);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err


echo "Neo4j importing GO"

for file in $DIR/*
do
	echo $file
	echo "LOAD CSV WITH HEADERS FROM \"file://${file}\" AS row FIELDTERMINATOR \"\t\" MATCH (c:MOL {id:row.id}), (p:GO_TERM {acc:row.goacc}) CREATE (c)-[:has_go { evidence: row.evidence, ref: row.ref, qualifier: row.qualifier }]->(p) ;"
    $NEO4JSHELL "LOAD CSV WITH HEADERS FROM \"file://${file}\" AS row FIELDTERMINATOR \"\t\" MATCH (c:MOL {id:row.id}), (p:GO_TERM {acc:row.goacc}) CREATE (c)-[:has_go { evidence: row.evidence, ref: row.ref, qualifier: row.qualifier }]->(p) ;" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
done

cd $GOADIR

rm $GOAFILE.reduced
