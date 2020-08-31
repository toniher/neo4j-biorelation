#!/bin/bash

set -ueo pipefail

# CONFIG parameters
set -a

. ./vars.sh

set +a

cp *sh $SCRATCHDIR
cp *py $SCRATCHDIR

singularity exec -e instance://$DOCKERNAME /bin/bash -c 'cd /scratch; /scratch/steps.sh'

