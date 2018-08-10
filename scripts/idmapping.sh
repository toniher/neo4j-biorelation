set -ueo pipefail

# CONFIG parameters

NEO4JSHELL=/data/soft/neo4j-community-3.1.5/bin/cypher-shell
IDURL=ftp://ftp.ebi.ac.uk/pub/databases/uniprot/current_release/knowledgebase/idmapping/idmapping.dat.gz
MAPPINGDIR=/data/db/go/mapping
MOMENTDIR=/data/toniher
SCRIPTPATH=`pwd`

TMPDIR=/data/tmp

export $TMPDIR

mkdir -p $MAPPINGDIR

cd $MAPPINGDIR
wget -c -t0 $IDURL
gunzip *gz

python $SCRIPTPATH/rewrite-IDmapping.py $MAPPINGDIR/idmapping.dat > $MAPPINGDIR/idmapping.new.dat
sed -i '/^$/d' $MAPPINGDIR/idmapping.new.dat

# Huge processing of Mapping ID
cut -f 2,3 $MAPPINGDIR/idmapping.new.dat > $MAPPINGDIR/idmapping.pre.dat

DIR=$MAPPINGDIR/idmappart

mkdir -p $DIR; cd $DIR; split -l 10000000 $MAPPINGDIR/idmapping.pre.dat idmapping

cd ..

for file in $DIR/*
do
    sort -u $file > $file.sort
done

sort -u -m $DIR/*.sort > $MAPPINGDIR/idmapping.sort.dat

rm -rf $DIR

DIR=$MAPPINGDIR/idmappart

mkdir -p $DIR; cd $DIR; split -l 10000000 $MAPPINGDIR/idmapping.new.dat idmapping

cd ..

for file in $DIR/*
do
    sort -u $file > $file.sort
done

sort -u -m $DIR/*.sort > $MAPPINGDIR/idmapping.sort.full.dat

rm -rf $DIR

# DIR of parts
DIR=$MAPPINGDIR/map

mkdir -p $DIR; cd $DIR; split -l 1000000 $MAPPINGDIR/idmapping.sort.full.dat idmapping
cd $MAPPINGDIR

# Adding alias relationship


echo "CREATE INDEX ON :MOLID(id);"
$NEO4JSHELL "CREATE INDEX ON :MOLID(id);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
echo "CREATE INDEX ON :MOLID(source);"
$NEO4JSHELL "CREATE INDEX ON :MOLID(source);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
#echo "CREATE INDEX ON :MOLID(id,source);"
#$NEO4JSHELL "CREATE INDEX ON :MOLID(id,source);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err


# DIR of parts
DIR=$MAPPINGDIR/mapimport

mkdir -p $DIR; cd $DIR; split -l 1000000 $MAPPINGDIR/idmapping.sort.dat idmapping


echo "Preparing MOLID files"
for file in $DIR/*
do
	echo -e "id\tsource\talias" |cat - $file > $MOMENTDIR/tempfile && mv $MOMENTDIR/tempfile $file
done

for file in $DIR/*
do
	echo $file
	echo "LOAD CSV WITH HEADERS FROM \"file://${file}\" AS row FIELDTERMINATOR \"\t\" MATCH (c:MOL {id:row.id}), (a:MOLID {id:row.alias, source:row.source}) CREATE (c)-[:has_alias]->(a) ;"
	$NEO4JSHELL "LOAD CSV WITH HEADERS FROM \"file://${file}\" AS row FIELDTERMINATOR \"\t\" MATCH (c:MOL {id:row.id}), (a:MOLID {id:row.alias, source:row.source}) CREATE (c)-[:has_alias]->(a) ;" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
done


