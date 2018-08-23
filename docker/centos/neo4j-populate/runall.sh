#!/bin/bash

# CONFIG parameters

set -a 

. ./vars.sh

set +a

rm -rf $GODIR $TAXDIR $GOADIR $MOMENTDIR $TMPDIR

mkdir -p $MOMENTDIR
mkdir -p $TMPDIR

./go.sh
#./taxonomy.sh
#./goa.sh


