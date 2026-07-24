#!/bin/bash

set -ueo pipefail

# CONFIG parameters
set -a

. ./vars.sh

set +a

# This optional step loads UniProt idmapping xrefs and is OFF by default:
# IDMAPDIR / IDMAPFILEPATH are commented out in vars.sh. Bail out cleanly here
# instead of dying on an unbound variable (set -u) further down.
if [ -z "${IDMAPDIR:-}" ]; then
	echo "IDMAPDIR is not set in vars.sh — the optional xref idmapping step is disabled." >&2
	echo "Uncomment the IDmapping URL/paths in vars.sh and prepare the id-part files to enable it." >&2
	exit 0
fi

#mkdir -p $IDMAPDIR

# Let's uncompress all files
#cd $IDMAPDIR
#curl --fail --silent --show-error --location --remote-name $IDMAPURL
#gunzip *gz
# $IDMAPFILE is a previously processed file with Spark

# Preprocess

# DIR of parts
DIR=$IDMAPDIR/id

#mkdir -p $DIR; cd $DIR; split -l 5000000 $IDMAPFILEPATH idmap


echo "Preparing ID files"

#for file in $DIR/*
#do
#	echo -e "id\tsource\txref" |cat - $file > $MOMENTDIR/tempfile && mv $MOMENTDIR/tempfile $file
#done


echo "CREATE INDEX xref_id ..."
$NEO4JSHELL "CREATE INDEX xref_id IF NOT EXISTS FOR (n:XREF) ON (n.id);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
echo "CREATE INDEX xref_source ..."
$NEO4JSHELL "CREATE INDEX xref_source IF NOT EXISTS FOR (n:XREF) ON (n.source);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err

for file in $DIR/*
do
	echo $file
	#echo "LOAD CSV WITH HEADERS FROM \"file://${file}\" AS row FIELDTERMINATOR \"\t\" CREATE (n:MOL) SET n = row ;"
	echo "CALL apoc.periodic.iterate(\"CALL apoc.load.csv('${file}', { sep:'TAB', header:true } ) yield map as row return row\",\"CREATE (n:XREF) SET n = { id:row.xref, source:row.source }\",{batchSize:10000, retries: 5, iterateList:true, parallel:true});" 
    #$NEO4JSHELL "LOAD CSV WITH HEADERS FROM \"file://${file}\" AS row FIELDTERMINATOR \"\t\" CREATE (n:MOL) SET n = row ;" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
	$NEO4JSHELL "CALL apoc.periodic.iterate(\"CALL apoc.load.csv('${file}', { sep:'TAB', header:true } ) yield map as row return row\",\"CREATE (n:XREF) SET n = { id:row.xref, source:row.source }\",{batchSize:10000, retries: 5, iterateList:true, parallel:true});" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
done

for file in $DIR/*
do
	echo $file
	
	echo "CALL apoc.periodic.iterate( \"CALL apoc.load.csv('${file}', { sep:'TAB', header:true } ) yield map as row return row\", \"MATCH (c:MOL {id:row.id}), (n:XREF {id:row.xref }) call apoc.merge.relationship(c,'has_xref',{},{},n) yield rel return count(*)\",{batchSize:5000, retries: 5, iterateList:true, parallel:false});"
	$NEO4JSHELL "CALL apoc.periodic.iterate( \"CALL apoc.load.csv('${file}', { sep:'TAB', header:true } ) yield map as row return row\", \"MATCH (c:MOL {id:row.id}), (n:XREF {id:row.xref}) call apoc.merge.relationship(c,'has_xref',{},{},n) yield rel return count(*)\",{batchSize:5000, retries: 5, iterateList:true, parallel:false});" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
done




