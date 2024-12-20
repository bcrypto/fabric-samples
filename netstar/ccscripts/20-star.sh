#!/bin/bash
SCRIPTDIR="$(dirname "$(realpath "$0")")"
source $SCRIPTDIR/../utils.sh
export PATH="$(dirname $(readlink -e ./))/bin:$PATH"

for (( k = 1; k < $ORG_COUNT + 1; ++k )); do
    for (( j = k + 1; j < $ORG_COUNT + 1; ++j )); do
        source $SCRIPTDIR/11-channel.sh events Org$k Org$j
    done
done