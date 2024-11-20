  #!/bin/bash
SCRIPTDIR="$(dirname "$(realpath "$0")")"
source $SCRIPTDIR/../utils.sh
source $SCRIPTDIR/chutils.sh
export PATH="$(dirname $(readlink -e ./))/bin:$PATH"

export FABRIC_CFG_PATH=${PWD}/configtx
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

FABRIC_CFG_PATH=$PWD/../config/
BLOCKFILE="./channel-artifacts/${CHANNEL_NAME}.block"

## Join all the peers to the channel
infoln "Joining ${ORG1_NAME} peer to the channel..."
joinChannel ${ORG1_NAME,,}
infoln "Joining ${ORG2_NAME} peer to the channel..."
joinChannel ${ORG2_NAME,,}