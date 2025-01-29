#!/bin/bash
SCRIPTDIR="$(dirname "$(realpath "$0")")"
source $SCRIPTDIR/../utils.sh
export PATH="$(dirname $(readlink -e ./))/bin:$PATH"

for (( k = 2; k < $ORG_COUNT + 1; ++k )); do
    source $SCRIPTDIR/10-channel.sh Org1 Org$k
done