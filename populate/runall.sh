#!/bin/bash

# CONFIG parameters

set -a 

. ./vars.sh

set +a

rm -rf $GODIR $TAXDIR $GOADIR $MOMENTDIR $TMPDIR

mkdir -p $MOMENTDIR
mkdir -p $TMPDIR

echo GO
./go.sh
echo TAX
./taxonomy.sh
echo GOA
./goa.sh


