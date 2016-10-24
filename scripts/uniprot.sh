# CONFIG parameters

NEO4JSHELL=/data/soft/neo4j-community-3.0.6/bin/neo4j-shell
#GOA: ftp://ftp.ebi.ac.uk/pub/databases/GO/goa/UNIPROT/goa_uniprot_all.gpa.gz
GOADIR=/data/db/go/goa
#IDmapping: ftp://ftp.ebi.ac.uk/pub/databases/uniprot/current_release/knowledgebase/idmapping/idmapping.dat.gz
MAPPINGDIR=/data/db/go/mapping
MOMENTDIR=/data/toniher
SCRIPTPATH=`pwd`

#Info Uniprot: ftp://ftp.ebi.ac.uk/pub/databases/GO/goa/UNIPROT/goa_uniprot_all.gpi.gz
INFOFILE=goa_uniprot_all.gpi
GOAFILE=goa_uniprot_all.gpa

# Let's uncompress all files
cd $GOADIR
gunzip *gz

# Base entries
cut -f 1,3,5,6 $INFOFILE | perl -F'\t' -lane ' if ($F[0]!~/^\!/ ) { $F[1]=~s/\"/\\"/g; print join( "\t", @F[0..2] ); } ' > $INFOFILE.base
# We skip interaction stuff for now
cut -f 1,2,3 $INFOFILE.base | perl -F'\t' -lane ' if ($F[2]=~/^protein/ ) { print $_; } ' > $INFOFILE.protein

rm $INFOFILE.base

# Creating synonyms in Redis -> TODO, this MUST change

cd $MAPPINGDIR
gunzip *gz

python $SCRIPTPATH/neo4j2-synonyms-redis.py $MAPPINGDIR/idmapping.dat
python $SCRIPTPATH/neo4j2-synonyms-add-from-redis.py $GOADIR/$INFOFILE.protein > $GOADIR/$INFOFILE.extra


# Adding mols

# DIR of parts
DIR=$GOADIR/mol

mkdir -p $DIR; cd $DIR; split -l 5000000 ../$INFOFILE.extra $INFOFILE.extra

echo "Modify files"

for file in $DIR/*
do
	echo -e "id\tname\ttype\tsynonyms:string_array" |cat - $file > $MOMENTDIR/tempfile && mv $MOMENTDIR/tempfile $file
done

echo "CREATE CONSTRAINT ON (n:MOL) ASSERT n.id IS UNIQUE;" > $MOMENTDIR/script
$NEO4JSHELL -file $MOMENTDIR/script >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
echo "CREATE INDEX ON :MOL(synonyms);" > $MOMENTDIR/script
$NEO4JSHELL -file $MOMENTDIR/script >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
echo "CREATE INDEX ON :MOL(name);" > $MOMENTDIR/script 
$NEO4JSHELL -file $MOMENTDIR/script >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
echo "CREATE INDEX ON :MOL(type);" > $MOMENTDIR/script
$NEO4JSHELL -file $MOMENTDIR/script >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err

echo "Neo4j importing"

for file in $DIR/*
do
	echo $file
	echo "import-cypher -b 10000 -d\"\t\" -i $file -o $MOMENTDIR/out CREATE (n:MOL { id:{id}, name:{name}, type:{type}, synonyms: { synonyms } })" > $MOMENTDIR/script
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

