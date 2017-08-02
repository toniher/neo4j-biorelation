# CONFIG parameters

NEO4JSHELL=/data/soft/neo4j-community-3.1.5/bin/cypher-shell
GOAURL=ftp://ftp.ebi.ac.uk/pub/databases/GO/goa/UNIPROT/goa_uniprot_all.gpa.gz
GOADIR=/data/db/go/goa
IDURL=ftp://ftp.ebi.ac.uk/pub/databases/uniprot/current_release/knowledgebase/idmapping/idmapping.dat.gz
MAPPINGDIR=/data/db/go/mapping
MOMENTDIR=/data/toniher
SCRIPTPATH=`pwd`

INFOURL=ftp://ftp.ebi.ac.uk/pub/databases/GO/goa/UNIPROT/goa_uniprot_all.gpi.gz
INFOFILE=goa_uniprot_all.gpi
GOAFILE=goa_uniprot_all.gpa

mkdir -p $GOADIR
mkdir -p $MAPPINGDIR

# Let's uncompress all files
cd $GOADIR
wget -c -t0 $GOAURL
wget -c -t0 $INFOURL
gunzip *gz

# Base entries
cut -f 1,3,5,6 $INFOFILE | perl -F'\t' -lane ' if ($F[0]!~/^\!/ ) { $F[1]=~s/\"/\\"/g; print join( "\t", @F[0..2] ); } ' > $INFOFILE.base
# We skip interaction stuff for now
cut -f 1,2,3 $INFOFILE.base | perl -F'\t' -lane ' if ($F[2]=~/^protein/ ) { print $_; } ' > $INFOFILE.protein

rm $INFOFILE.base

cd $MAPPINGDIR
wget -c -t0 $IDURL
gunzip *gz

python $SCRIPTPATH/rewrite-IDmapping.py $MAPPINGDIR/idmapping.dat > $MAPPINGDIR/idmapping.new.dat

# Adding mols

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

# Adding alias relationship

# DIR of parts
DIR=$GOADIR/mapping

mkdir -p $DIR; cd $DIR; split -l 1000000 $MAPPINGDIR/idmapping.new.dat idmapping


echo "CREATE CONSTRAINT ON (a:ALIAS) ASSERT a.id IS UNIQUE;"
$NEO4JSHELL "CREATE CONSTRAINT ON (a:ALIAS) ASSERT a.id IS UNIQUE;" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
echo "CREATE INDEX ON :ALIAS(source);"
$NEO4JSHELL "CREATE INDEX ON :ALIAS(source);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err

echo "Preparing ALIAS files"
for file in $DIR/*
do
	echo -e "id\tsource\talias" |cat - $file > $MOMENTDIR/tempfile && mv $MOMENTDIR/tempfile $file
done

echo "Neo4j importing ALIAS files"

for file in $DIR/*
do
	echo $file
	echo "LOAD CSV WITH HEADERS FROM \"file://${file}\" AS row FIELDTERMINATOR \"\t\" CREATE (a:ALIAS { id: row.alias , source: row.source } ) ;"
    $NEO4JSHELL "LOAD CSV WITH HEADERS FROM \"file://${file}\" AS row FIELDTERMINATOR \"\t\" CREATE (a:ALIAS { id: row.alias , source: row.source } ) ;" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
done


for file in $DIR/*
do
	echo $file
	echo "LOAD CSV WITH HEADERS FROM \"file://${file}\" AS row FIELDTERMINATOR \"\t\" MATCH (c:MOL {id:row.id}), (a:ALIAS {id:row.alias}) CREATE (c)-[:has_alias]->(a) ;"
	$NEO4JSHELL "LOAD CSV WITH HEADERS FROM \"file://${file}\" AS row FIELDTERMINATOR \"\t\" MATCH (c:MOL {id:row.id}), (a:ALIAS {id:row.alias}) CREATE (c)-[:has_alias]->(a) ;" $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
done

cd $GOADIR


# Adding relationships to Taxon
cut -f 1,6 $INFOFILE | perl -F'\t' -lane ' if ($F[0]!~/^\!/ && $F[1]=~/^taxon/ ) { my $id=$F[0]; my $tax=$F[1]; $tax=~s/taxon\://g; print $id, "\t", $tax; } ' > $INFOFILE.reduced

# DIR of parts
DIR=$GOADIR/moltaxon

mkdir -p $DIR; cd $DIR; split -l 1000000 ../$INFOFILE.reduced $INFOFILE.reduced


echo "Modify files"

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


# Adding relationships to GO
cut -f 1,2,3,4,5,6 $GOAFILE | perl -F'\t' -lane ' if ($F[0]!~/^\!/ && $F[0]=~/^UniProt/ ) { print join("\t", @F[1..5]); } '  > $GOAFILE.reduced

#DIR of parts
DIR=$GOADIR/molgoa

mkdir -p $DIR; cd $DIR; split -l 1000000 ../$GOAFILE.reduced $GOAFILE.reduced

echo "Modify files"

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


echo "Neo4j importing"

for file in $DIR/*
do
	echo $file
	echo "LOAD CSV WITH HEADERS FROM \"file://${file}\" AS row FIELDTERMINATOR \"\t\" MATCH (c:MOL {id:row.id}), (p:GO_TERM {acc:row.goacc}) CREATE (c)-[:has_go { evidence: row.evidence, ref: row.ref, qualifier: row.qualifier }]->(p) ;"
    $NEO4JSHELL "LOAD CSV WITH HEADERS FROM \"file://${file}\" AS row FIELDTERMINATOR \"\t\" MATCH (c:MOL {id:row.id}), (p:GO_TERM {acc:row.goacc}) CREATE (c)-[:has_go { evidence: row.evidence, ref: row.ref, qualifier: row.qualifier }]->(p) ;" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
done

cd $GOADIR

rm $INFOFILE.reduced

