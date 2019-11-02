!/bin/bash

set -ueo pipefail

# CONFIG parameters

set -a 
. ./vars.sh
set +a

# GO
$NEO4JSHELL "CREATE INDEX ON :GO(id);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
$NEO4JSHELL "CREATE INDEX ON :GO(acc);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
$NEO4JSHELL "CREATE INDEX ON :GO(term_type);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err

# TAXID
$NEO4JSHELL "CREATE INDEX ON :TAXID(id);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
$NEO4JSHELL "CREATE INDEX ON :TAXID(taxid);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
$NEO4JSHELL "CREATE INDEX ON :TAXID(rank);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
$NEO4JSHELL "CREATE INDEX ON :TAXID(scientific_name);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
$NEO4JSHELL "CREATE INDEX ON :TAXID(name);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err

# MOL
$NEO4JSHELL "CREATE INDEX ON :MOL(id);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
$NEO4JSHELL "CREATE INDEX ON :MOL(name);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
$NEO4JSHELL "CREATE INDEX ON :MOL(type);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err

# has_go
$NEO4JSHELL "CREATE INDEX ON :has_go(evidence);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
$NEO4JSHELL "CREATE INDEX ON :has_go(ref);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
$NEO4JSHELL "CREATE INDEX ON :has_go(qualifier);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err

# interacts_with
$NEO4JSHELL "CREATE INDEX ON :interacts_with(method);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
$NEO4JSHELL "CREATE INDEX ON :interacts_with(intype);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
$NEO4JSHELL "CREATE INDEX ON :interacts_with(confidence);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
$NEO4JSHELL "CREATE INDEX ON :interacts_with(update);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
$NEO4JSHELL "CREATE INDEX ON :interacts_with(source);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err