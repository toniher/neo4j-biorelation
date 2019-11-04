#!/bin/bash

set -ueo pipefail

# CONFIG parameters
set -a

. ./vars.sh

set +a

cp *sh $SCRATCHDIR
cp *py $SCRATCHDIR

docker exec $DOCKERNAME /bin/bash -c 'cd /scratch; /scratch/steps.sh'

