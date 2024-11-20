#!/bin/bash
SCRIPTDIR="$(dirname "$(realpath "$0")")"
source $SCRIPTDIR/../utils.sh
source $SCRIPTDIR/chutils.sh
export PATH="$(dirname $(readlink -e ./))/bin:$PATH"

export FABRIC_CFG_PATH=${PWD}/../config
export VERBOSE=false
# scripts/createChannel.sh $CHANNEL_NAME $CLI_DELAY $MAX_RETRY $VERBOSE

ORG1_NAME=$1
ORG2_NAME=$2

# timeout duration - the duration the CLI should wait for a response from
# another container before giving up
MAX_RETRY=5
# default for delay between commands
DELAY=3
# channel name defaults to "mychannel"
CHANNEL_NAME="channel-${ORG1_NAME,,}-${ORG2_NAME,,}"
# default database
DATABASE="leveldb"

TMPDIR=./tmp
mkdir -p $TMPDIR

: ${CONTAINER_CLI:="docker"}
: ${CONTAINER_CLI_COMPOSE:="${CONTAINER_CLI} compose"}
infoln "Using ${CONTAINER_CLI} and ${CONTAINER_CLI_COMPOSE}"

setAnchorConfig $ORG1_NAME
setAnchorConfig $ORG2_NAME

successln "Config for '$CHANNEL_NAME' updated"
