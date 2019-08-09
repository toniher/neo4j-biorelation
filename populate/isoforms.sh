set -ueo pipefail

# CONFIG parameters
set -a

. ./vars.sh

set +a

mkdir -p $ISODIR

# Let's uncompress all files
cd $ISODIR
curl --fail --silent --show-error --location --remote-name $ISOURL
gunzip $ISOFILE.gz

perl -lane 'if ( $_=~/^\>\w+\|(\S+)\|/ ) { print $1; } '  $ISOFILE |sort -u > isoforms.pre.txt
python $SCRIPTPATH/processIsoformsNames.py isoforms.pre.txt > isoforms.txt
rm isoforms.pre.txt

# Preprocess

echo "CREATE INDEX ON :ISO(id);"
$NEO4JSHELL "CREATE INDEX ON :ISO(id);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err

# DIR of parts
DIR=$ISODIR/id

mkdir -p $DIR; cd $DIR; split -l 5000000 $ISODIR/isoforms.txt ISO


echo "Preparing ID files"

for file in $DIR/*
do
	echo -e "iso\tuniprot" |cat - $file > $MOMENTDIR/tempfile && mv $MOMENTDIR/tempfile $file
done


for file in $DIR/*
do
	echo $file
	ISOAPOC1="CALL apoc.periodic.iterate( \"CALL apoc.load.csv('${file}', { sep:'TAB', header:true ) yield map as row return row\", \"CREATE (i:ISO {id: row.id} )\",{batchSize:5000, retries: 5, iterateList:true, parallel:true});"
	ISOAPOC2="CALL apoc.periodic.iterate( \"CALL apoc.load.csv('${file}', { sep:'TAB', header:true ) yield map as row return row\", \"MATCH (n:MOL {id:row.uniprot}), (i:ISO {id:row.iso }) call apoc.merge.relationship(n,'has_isoform',{},{},i) yield rel return count(*)\",{batchSize:5000, retries: 5, iterateList:true, parallel:false});"
	echo $ISOAPOC1	
	echo $ISOAPOC2
	$NEO4JSHELL $ISOAPOC1 >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
	$NEO4JSHELL $ISOAPOC2 >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
done




