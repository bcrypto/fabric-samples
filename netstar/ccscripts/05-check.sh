#!/bin/bash
SCRIPTDIR="$(dirname "$(realpath "$0")")"
source $SCRIPTDIR/../utils.sh
export PATH="$(dirname $(readlink -e ./))/bin:$PATH"
export FABRIC_CFG_PATH=$PWD/../config/

CC_NAME=$1
ORG1_NAME=$2
ORG2_NAME=$3
CHANNEL_NAME="channel-${ORG1_NAME,,}-${ORG2_NAME,,}"
CC_VERSION="1.0"
DELAY=3
MAX_RETRY=5
CC_SEQUENCE=1
INIT_REQUIRED=""
VERBOSE=false
TMPDIR=./tmp
CC_END_POLICY="OR('${ORG1_NAME^}MSP.peer','${ORG2_NAME^}MSP.peer')"
CC_COLL_CONFIG=$TMPDIR/configtx/${CHANNEL_NAME}/collection_config.json

# checkCommitReadiness VERSION PEER ORG
function checkCommitReadiness() {
  ORG=$1
  shift 1
  setGlobals $ORG
  infoln "Checking the commit readiness of the chaincode definition on peer0.${ORG} on channel '$CHANNEL_NAME'..."
  local rc=1
  local COUNTER=1
  # continue to poll
  # we either get a successful response, or reach MAX RETRY
  while [ $rc -ne 0 -a $COUNTER -lt $MAX_RETRY ]; do
    sleep $DELAY
    infoln "Attempting to check the commit readiness of the chaincode definition on peer0.${ORG}, Retry after $DELAY seconds."
    set -x
    peer lifecycle chaincode checkcommitreadiness --channelID $CHANNEL_NAME --name ${CC_NAME} --version ${CC_VERSION} --sequence ${CC_SEQUENCE} ${INIT_REQUIRED} ${CC_END_POLICY} ${CC_COLL_CONFIG} --output json >&log.txt
    res=$?
    { set +x; } 2>/dev/null
    let rc=0
    for var in "$@"; do
      grep "$var" log.txt &>/dev/null || let rc=1
    done
    COUNTER=$(expr $COUNTER + 1)
  done
  cat log.txt
  if test $rc -eq 0; then
    infoln "Checking the commit readiness of the chaincode definition successful on peer0.${ORG} on channel '$CHANNEL_NAME'"
  else
    fatalln "After $MAX_RETRY attempts, Check commit readiness result on peer0.${ORG} is INVALID!"
  fi
}

## check whether the chaincode definition is ready to be committed
## expect them both to have approved 
checkCommitReadiness ${ORG1_NAME,,} "\"${ORG1_NAME^}MSP\": true" "\"${ORG2_NAME^}MSP\": true" 
checkCommitReadiness ${ORG2_NAME,,} "\"${ORG1_NAME^}MSP\": true" "\"${ORG2_NAME^}MSP\": true" 
