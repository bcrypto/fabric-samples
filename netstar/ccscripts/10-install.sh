#!/bin/bash
SCRIPTDIR="$(dirname "$(realpath "$0")")"
source $SCRIPTDIR/../utils.sh

source $SCRIPTDIR/01-compile.sh ../asset-transfer-events/chaincode-java/
source $SCRIPTDIR/01-compile.sh ../asset-transfer-events/opcode-java/

source $SCRIPTDIR/02-package.sh events ../asset-transfer-events/chaincode-java/build/install/events java 1.0
source $SCRIPTDIR/02-package.sh opcode ../asset-transfer-events/opcode-java/build/install/opcode java 1.0

for (( k = 1; k < $ORG_COUNT + 1; ++k )); do
    source $SCRIPTDIR/03-install.sh org$k events
    source $SCRIPTDIR/03-install.sh org$k opcode
done