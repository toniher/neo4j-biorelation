set -ueo pipefail

# CONFIG parameters
set -a

. ./vars.sh

set +a

mkdir -p $INTACTDIR

# Let's uncompress all files
cd $INTACTDIR
curl --fail --silent --show-error --location --remote-name $INTACTURL

cut -f1,2,7,12,15,32 intact.txt | perl -lane 'print if $F[0]=~/^uniprotkb/ && $F[1]=~/^uniprotkb/' > intact-reduced.txt
sed -i 's/uniprotkb://g' intact-reduced.txt

python $SCRIPTPATH/filterIntactReduced.py intact-reduced.txt > intact-reduced.txt

# Preprocess

echo "CREATE INDEX ON :INTERACT(id);"
$NEO4JSHELL "CREATE INDEX ON :INTERACT(id);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
echo "CREATE INDEX ON :INTERACT(source);"
$NEO4JSHELL "CREATE INDEX ON :INTERACT(source);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
echo "CREATE INDEX ON :INTERACT(type);"
$NEO4JSHELL "CREATE INDEX ON :INTERACT(type);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
echo "CREATE INDEX ON :INTERACT(method);"
$NEO4JSHELL "CREATE INDEX ON :INTERACT(method);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
echo "CREATE INDEX ON :INTERACT(confidence);"
$NEO4JSHELL "CREATE INDEX ON :INTERACT(confidence);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
echo "CREATE INDEX ON :INTERACT(update);"
$NEO4JSHELL "CREATE INDEX ON :INTERACT(update);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err

# DIR of parts
DIR=$INTACTDIR/id

mkdir -p $DIR; cd $DIR; split -l 5000000 $INTACTFILEPATH INTACT


echo "Preparing ID files"

for file in $DIR/*
do
	echo -e "mola\tmolb\tmethod\tintype\tconfidence\tupdate" |cat - $file > $MOMENTDIR/tempfile && mv $MOMENTDIR/tempfile $file
done


for file in $DIR/*
do
	echo $file
	# TODO -> Add relationship and node at the same time??? -> Source default intact
	echo "CALL apoc.periodic.iterate( \"CALL apoc.load.csv('${file}', { sep:'TAB', header:true, mapping: { confidence:{type:'float'}, update:{ type:'date'} } ) yield map as row return row\", \"MATCH (a:MOL {id:row.mola}), (b:MOL {id:row.molb }) call apoc.merge.relationship(a,'has_intact',{},{ confidence: row.confidence, update: row.update },b) yield rel return count(*)\",{batchSize:5000, retries: 5, iterateList:true, parallel:false});"
	$NEO4JSHELL "" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
done




