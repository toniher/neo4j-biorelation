set -ueo pipefail

# CONFIG parameters
set -a

. ./vars.sh

set +a

mkdir -p $IDMAPDIR

# Let's uncompress all files
#cd $IDMAPDIR
#curl --fail --silent --show-error --location --remote-name $IDMAPURL
#gunzip *gz
# $IDMAPFILE is a previously processed file with Spark

# Preprocess

# DIR of parts
DIR=$IDMAPDIR/id

mkdir -p $DIR; cd $DIR; split -l 10000000 $IDMAPFILEPATH idmap


echo "Preparing ID files"

for file in $DIR/*
do
	echo -e "id\tsource\txref" |cat - $file > $MOMENTDIR/tempfile && mv $MOMENTDIR/tempfile $file
done


echo "CREATE CONSTRAINT ON (n:XREF) ASSERT n.id IS UNIQUE;"
$NEO4JSHELL "CREATE CONSTRAINT ON (n:XREF) ASSERT n.id IS UNIQUE;" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
echo "CREATE INDEX ON :XREF(source);"
$NEO4JSHELL "CREATE INDEX ON :XREF(source);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err

for file in $DIR/*
do
	echo $file
	#echo "LOAD CSV WITH HEADERS FROM \"file://${file}\" AS row FIELDTERMINATOR \"\t\" CREATE (n:MOL) SET n = row ;"
	echo "CALL apoc.periodic.iterate(\"CALL apoc.load.csv('${file}', { sep:'TAB', header:true } ) yield map as row return row\",\"CREATE (n:XREF) SET n = { id:row.xref, source:row.source }\",{batchSize:10000, retries: 5, iterateList:true, parallel:false});" 
    #$NEO4JSHELL "LOAD CSV WITH HEADERS FROM \"file://${file}\" AS row FIELDTERMINATOR \"\t\" CREATE (n:MOL) SET n = row ;" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
	$NEO4JSHELL "CALL apoc.periodic.iterate(\"CALL apoc.load.csv('${file}', { sep:'TAB', header:true } ) yield map as row return row\",\"CREATE (n:XREF) SET n = { id:row.xref, source:row.source }\",{batchSize:10000, retries: 5, iterateList:true, parallel:false});" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
done

for file in $DIR/*
do
	echo $file
	
	echo "CALL apoc.periodic.iterate( \"CALL apoc.load.csv('${file}', { sep:'TAB', header:true } ) yield map as row return row\", \"MATCH (c:MOL {id:row.id}), (n:XREF {id:row.xref }) call apoc.merge.relationship(c,'has_xref',{},{},p) yield rel return count(*)\",{batchSize:2500, retries: 5, iterateList:true, parallel:false});"
	$NEO4JSHELL "CALL apoc.periodic.iterate( \"CALL apoc.load.csv('${file}', { sep:'TAB', header:true } ) yield map as row return row\", \"MATCH (c:MOL {id:row.id}), (n:XREF {id:row.xref}) call apoc.merge.relationship(c,'has_xref',{},{},n) yield rel return count(*)\",{batchSize:2500, retries: 5, iterateList:true, parallel:false});" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
done




