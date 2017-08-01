# CONFIG parameters

NEO4JSHELL=/data/soft/neo4j-community-3.0.6/bin/neo4j-shell
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

# Check mapping not in GOA INFO. about it
cut -f1 $INFOFILE.protein > $MOMENTDIR/ID-info.txt
cut -f1 $MAPPINGDIR/idmapping.dat > $MOMENTDIR/ID-mapping.txt

comm -1 -3  $MOMENTDIR/ID-info.txt $MOMENTDIR/ID-mapping.txt > $MOMENTDIR/ID-mapping.exclude.txt

# Generate different mapping
python rewrite-IDmapping.py $MAPPINGDIR/idmapping.dat $MOMENTDIR/ID-mapping.exclude.txt > $MAPPINGDIR/idmapping.new.dat

rm $MOMENTDIR/ID-mapping.txt; rm $MOMENTDIR/ID-info.txt; rm $MOMENTDIR/ID-mapping.exclude.txt;


# Adding mols

# DIR of parts
DIR=$GOADIR/mol

mkdir -p $DIR; cd $DIR; split -l 5000000 ../$INFOFILE.protein $INFOFILE.protein

echo "Modify files"

for file in $DIR/*
do
	echo -e "id\tname\ttype\t" |cat - $file > $MOMENTDIR/tempfile && mv $MOMENTDIR/tempfile $file
done

echo "CREATE CONSTRAINT ON (n:MOL) ASSERT n.id IS UNIQUE;" > $MOMENTDIR/script
$NEO4JSHELL -file $MOMENTDIR/script >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
echo "CREATE INDEX ON :MOL(name);" > $MOMENTDIR/script 
$NEO4JSHELL -file $MOMENTDIR/script >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
echo "CREATE INDEX ON :MOL(type);" > $MOMENTDIR/script
$NEO4JSHELL -file $MOMENTDIR/script >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err

echo "Neo4j importing"

for file in $DIR/*
do
	echo $file
	echo "import-cypher -b 10000 -d\"\t\" -i $file -o $MOMENTDIR/out CREATE (n:MOL { id:{id}, name:{name}, type:{type} })" > $MOMENTDIR/script
       $NEO4JSHELL -file $MOMENTDIR/script >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
done

cd $GOADIR

# Adding alias relationship

# DIR of parts
DIR=$GOADIR/mapping

mkdir -p $DIR; cd $DIR; split -l 5000000 $MAPPINGDIR/idmapping.new.dat idmapping


echo "CREATE CONSTRAINT ON (a:ALIAS) ASSERT a.id IS UNIQUE;" > $MOMENTDIR/script
$NEO4JSHELL -file $MOMENTDIR/script >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
echo "CREATE INDEX ON :ALIAS(source);" > $MOMENTDIR/script 
$NEO4JSHELL -file $MOMENTDIR/script >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err

echo "Modify files"
for file in $DIR/*
do
	echo -e "id\tsource\talias" |cat - $file > $MOMENTDIR/tempfile && mv $MOMENTDIR/tempfile $file
done

for file in $DIR/*
do
	echo $file
	echo "import-cypher -b 10000 -d\"\t\" -i $file -o $MOMENTDIR/out CREATE (a:ALIAS { id:{alias}, source:{source} })" > $MOMENTDIR/script
       $NEO4JSHELL -file $MOMENTDIR/script >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
done

for file in $DIR/*
do
	echo $file
	echo "import-cypher -b 10000 -d\"\t\" -i $file -o $MOMENTDIR/out MATCH (c:MOL {id:{id}}), (a:ALIAS {id:{alias}}) CREATE (c)-[:has_alias]->(a)" > $MOMENTDIR/script
       $NEO4JSHELL -file $MOMENTDIR/script >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
done

cd $GOADIR


# Adding relationships to Taxon
cut -f 1,6 $INFOFILE | perl -F'\t' -lane ' if ($F[0]!~/^\!/ && $F[1]=~/^taxon/ ) { my $id=$F[0]; my $tax=$F[1]; $tax=~s/taxon\://g; print $id, "\t", $tax; } ' > $INFOFILE.reduced

# DIR of parts
DIR=$GOADIR/moltaxon

mkdir -p $DIR; cd $DIR; split -l 5000000 ../$INFOFILE.reduced $INFOFILE.reduced


echo "Modify files"

for file in $DIR/*
do
	echo -e "id\ttaxon:int" |cat - $file > $MOMENTDIR/tempfile && mv $MOMENTDIR/tempfile $file
done

for file in $DIR/*
do
	echo $file
	echo "import-cypher -b 10000 -d\"\t\" -i $file -o $MOMENTDIR/out MATCH (c:MOL {id:{id}}), (p:TAXID {id:{taxon}}) CREATE (c)-[:has_taxon]->(p)" > $MOMENTDIR/script
       $NEO4JSHELL -file $MOMENTDIR/script >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
done

cd $GOADIR

rm $INFOFILE.reduced


# Adding relationships to GO
cut -f 1,2,3,4,5,6 $GOAFILE | perl -F'\t' -lane ' if ($F[0]!~/^\!/ && $F[0]=~/^UniProt/ ) { print join("\t", @F[1..5]); } '  > $GOAFILE.reduced

#DIR of parts
DIR=$GOADIR/molgoa

mkdir -p $DIR; cd $DIR; split -l 5000000 ../$GOAFILE.reduced $GOAFILE.reduced

echo "Modify files"

for file in $DIR/*
do
		echo -e "id\tqualifier\tgoacc\tref\tevidence" |cat - $file > $MOMENTDIR/tempfile && mv $MOMENTDIR/tempfile $file

done


echo "CREATE INDEX ON :has_go(evidence);" > $MOMENTDIR/script
$NEO4JSHELL -file $MOMENTDIR/script >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
echo "CREATE INDEX ON :has_go(ref);" > $MOMENTDIR/script
$NEO4JSHELL -file $MOMENTDIR/script >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
echo "CREATE INDEX ON :has_go(qualifier);" > $MOMENTDIR/script
$NEO4JSHELL -file $MOMENTDIR/script >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err


echo "Neo4j importing"

for file in $DIR/*
do
	echo $file
	echo "import-cypher -b 10000 -d\"\t\" -i $file -o $MOMENTDIR/out MATCH (c:MOL {id:{id}}), (p:GO_TERM {acc:{goacc}}) CREATE (c)-[:has_go { evidence: {evidence}, ref: {ref}, qualifier: {qualifier} }]->(p)" > $MOMENTDIR/script
       $NEO4JSHELL -file $MOMENTDIR/script >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
done

cd $GOADIR

rm $INFOFILE.reduced

