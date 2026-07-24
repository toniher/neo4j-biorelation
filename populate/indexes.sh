#!/bin/bash

set -ueo pipefail

# CONFIG parameters

set -a
. ./vars.sh
set +a

# Neo4j 5 index DDL: `CREATE INDEX ... FOR (n:Label) ON (n.prop)` replaces the
# removed `CREATE INDEX ON :Label(prop)` syntax. Full-text indexes are created
# with `CREATE FULLTEXT INDEX` Cypher instead of the db.index.fulltext.* procs.

# GO
$NEO4JSHELL "CREATE INDEX go_id IF NOT EXISTS FOR (n:GO) ON (n.id);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
$NEO4JSHELL "CREATE INDEX go_acc IF NOT EXISTS FOR (n:GO) ON (n.acc);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
$NEO4JSHELL "CREATE INDEX go_term_type IF NOT EXISTS FOR (n:GO) ON (n.term_type);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err

# TAXID
$NEO4JSHELL "CREATE INDEX tax_id IF NOT EXISTS FOR (n:TAXID) ON (n.id);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
$NEO4JSHELL "CREATE INDEX tax_taxid IF NOT EXISTS FOR (n:TAXID) ON (n.taxid);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
$NEO4JSHELL "CREATE INDEX tax_rank IF NOT EXISTS FOR (n:TAXID) ON (n.rank);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
$NEO4JSHELL "CREATE INDEX tax_scientific_name IF NOT EXISTS FOR (n:TAXID) ON (n.scientific_name);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err

# MOL
$NEO4JSHELL "CREATE INDEX mol_id IF NOT EXISTS FOR (n:MOL) ON (n.id);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
$NEO4JSHELL "CREATE INDEX mol_name IF NOT EXISTS FOR (n:MOL) ON (n.name);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
$NEO4JSHELL "CREATE INDEX mol_type IF NOT EXISTS FOR (n:MOL) ON (n.type);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err

# has_go (relationship property indexes)
$NEO4JSHELL "CREATE INDEX has_go_evidence IF NOT EXISTS FOR ()-[r:has_go]-() ON (r.evidence);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
$NEO4JSHELL "CREATE INDEX has_go_ref IF NOT EXISTS FOR ()-[r:has_go]-() ON (r.ref);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
$NEO4JSHELL "CREATE INDEX has_go_qualifier IF NOT EXISTS FOR ()-[r:has_go]-() ON (r.qualifier);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err

$NEO4JSHELL "CREATE FULLTEXT INDEX go_evidence IF NOT EXISTS FOR ()-[r:has_go]-() ON EACH [r.evidence];" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
$NEO4JSHELL "CREATE FULLTEXT INDEX go_ref IF NOT EXISTS FOR ()-[r:has_go]-() ON EACH [r.ref];" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
$NEO4JSHELL "CREATE FULLTEXT INDEX go_qualifier IF NOT EXISTS FOR ()-[r:has_go]-() ON EACH [r.qualifier];" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err

# interacts_with
$NEO4JSHELL "CREATE INDEX interacts_method IF NOT EXISTS FOR ()-[r:interacts_with]-() ON (r.method);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
$NEO4JSHELL "CREATE INDEX interacts_intype IF NOT EXISTS FOR ()-[r:interacts_with]-() ON (r.intype);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
$NEO4JSHELL "CREATE INDEX interacts_confidence IF NOT EXISTS FOR ()-[r:interacts_with]-() ON (r.confidence);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
$NEO4JSHELL "CREATE INDEX interacts_update IF NOT EXISTS FOR ()-[r:interacts_with]-() ON (r.update);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
$NEO4JSHELL "CREATE INDEX interacts_source IF NOT EXISTS FOR ()-[r:interacts_with]-() ON (r.source);" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err

$NEO4JSHELL "CREATE FULLTEXT INDEX interact_method IF NOT EXISTS FOR ()-[r:interacts_with]-() ON EACH [r.method];" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
$NEO4JSHELL "CREATE FULLTEXT INDEX interact_type IF NOT EXISTS FOR ()-[r:interacts_with]-() ON EACH [r.intype];" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
$NEO4JSHELL "CREATE FULLTEXT INDEX interact_source IF NOT EXISTS FOR ()-[r:interacts_with]-() ON EACH [r.source];" >> $MOMENTDIR/syn.out 2>> $MOMENTDIR/syn.err
