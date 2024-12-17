#!/bin/bash
SCRIPTDIR="$(dirname "$(realpath "$0")")"
source $SCRIPTDIR/../utils.sh
source $SCRIPTDIR/chutils.sh
export PATH="$(dirname $(readlink -e ./))/bin:$PATH"

#   infoln "Joining Org3 peers to network"
#   ${CONTAINER_CLI} exec cli ./scripts/org3-scripts/joinChannel.sh $CHANNEL_NAME $CLI_DELAY $CLI_TIMEOUT $VERBOSE
#   if [ $? -ne 0 ]; then
#     fatalln "ERROR !!!! Unable to join Org3 peers to network"
#   fi
#

CHANNEL_NAME="$1"
NEW_ORG="$2"
TMPDIR=./tmp

: ${CHANNEL_NAME:="mychannel"}
: ${DELAY:="3"}
: ${TIMEOUT:="10"}
: ${VERBOSE:="false"}
COUNTER=1
MAX_RETRY=5

# import environment variables
. scripts/envVar.sh

setAnchorPeer() {
  ORG=$1
  scripts/setAnchorPeer.sh $ORG $CHANNEL_NAME
}

setGlobals ${NEW_ORG}
BLOCKFILE="${CHANNEL_NAME}.block"

echo "Fetching channel config block from orderer..."
set -x
peer channel fetch 0 $BLOCKFILE -o orderer.example.com:7050 --ordererTLSHostnameOverride orderer.example.com -c $CHANNEL_NAME --tls --cafile "$ORDERER_CA" >&log.txt
res=$?
{ set +x; } 2>/dev/null
cat log.txt
verifyResult $res "Fetching config block from orderer has failed"

infoln "Joining ${NEW_ORG} peer to the channel..."
joinChannel ${NEW_ORG}

infoln "Setting anchor peer for ${NEW_ORG}..."
setAnchorConfig ${NEW_ORG}
setAnchorPeer ${NEW_ORG}



successln "Channel '$CHANNEL_NAME' joined"
successln " ${NEW_ORG} peer successfully added to network"
