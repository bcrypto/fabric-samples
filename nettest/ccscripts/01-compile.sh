#!/bin/bash
SCRIPTDIR="$(dirname "$(realpath "$0")")"
source $SCRIPTDIR/utils.sh

CC_SRC_PATH=$1

rm -rf $CC_SRC_PATH/build/install/
infoln "Compiling Java code..."
pushd $CC_SRC_PATH
./gradlew installDist
popd
successln "Finished compiling Java code"