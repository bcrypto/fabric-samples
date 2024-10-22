  #!/bin/bash
SCRIPTDIR="$(dirname "$(realpath "$0")")"
source $SCRIPTDIR/utils.sh
export PATH="$(dirname $(readlink -e ./))/bin:$PATH"

export FABRIC_CFG_PATH=${PWD}/configtx
export VERBOSE=false
# scripts/createChannel.sh $CHANNEL_NAME $CLI_DELAY $MAX_RETRY $VERBOSE

# timeout duration - the duration the CLI should wait for a response from
# another container before giving up
MAX_RETRY=5
# default for delay between commands
DELAY=3
# channel name defaults to "mychannel"
CHANNEL_NAME="mychannel"
# default database
DATABASE="leveldb"


# joinChannel ORG
joinChannel() {
  FABRIC_CFG_PATH=$PWD/../config/
  ORG=$1
  setGlobals $ORG
	local rc=1
	local COUNTER=1
	## Sometimes Join takes time, hence retry
	while [ $rc -ne 0 -a $COUNTER -lt $MAX_RETRY ] ; do
    sleep $DELAY
    set -x
    peer channel join -b $BLOCKFILE >&log.txt
    res=$?
    { set +x; } 2>/dev/null
		let rc=$res
		COUNTER=$(expr $COUNTER + 1)
	done
	cat log.txt
	verifyResult $res "After $MAX_RETRY attempts, peer0.${ORG} has failed to join channel '$CHANNEL_NAME' "
}


FABRIC_CFG_PATH=$PWD/../config/
BLOCKFILE="./channel-artifacts/${CHANNEL_NAME}.block"

## Join all the peers to the channel
infoln "Joining org1 peer to the channel..."
joinChannel org1
infoln "Joining org2 peer to the channel..."
joinChannel org2