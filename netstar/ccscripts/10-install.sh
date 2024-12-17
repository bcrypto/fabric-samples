#!/bin/bash
SCRIPTDIR="$(dirname "$(realpath "$0")")"
source $SCRIPTDIR/../utils.sh

source $SCRIPTDIR/01-compile.sh ./chaincode-java/

source $SCRIPTDIR/02-package.sh events ./chaincode-java/build/install/events java 1.0

for (( k = 1; k < $ORG_COUNT + 1; ++k )); do
    source $SCRIPTDIR/03-install.sh org$k events
done