#!/bin/bash
SCRIPTDIR="$(dirname "$(realpath "$0")")"
source $SCRIPTDIR/utils.sh
export PATH="$(dirname $(readlink -e ./))/bin:$PATH"
export FABRIC_CFG_PATH=$PWD/../config/

NET=${PWD}/../test-network
export CORE_PEER_TLS_ENABLED=true
CC_NAME=$1
CHANNEL_NAME=$2
DELAY=3
MAX_RETRY=5
CC_VERSION="1.0"
CC_SEQUENCE=1
INIT_REQUIRED=""
VERBOSE=false
CC_END_POLICY="OR('Org1MSP.peer','Org2MSP.peer')"
CC_COLL_CONFIG="../asset-transfer-events/chaincode-java/collections_config.json"
PACKAGE_ID=$(peer lifecycle chaincode calculatepackageid ${CC_NAME}.tar.gz)

# approveForMyOrg VERSION PEER ORG
function approveForMyOrg() {
  ORG=$1
  setGlobals $ORG
  set -x
  peer lifecycle chaincode approveformyorg -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com --tls --cafile "$ORDERER_CA" --channelID $CHANNEL_NAME --name ${CC_NAME} --version ${CC_VERSION} --package-id ${PACKAGE_ID} --sequence ${CC_SEQUENCE} ${INIT_REQUIRED} ${CC_END_POLICY} ${CC_COLL_CONFIG} >&log.txt
  res=$?
  { set +x; } 2>/dev/null
  cat log.txt
  verifyResult $res "Chaincode definition approved on peer0.${ORG} on channel '$CHANNEL_NAME' failed"
  successln "Chaincode definition approved on peer0.${ORG} on channel '$CHANNEL_NAME'"
}

## approve the definition for org1
approveForMyOrg org1

## now approve also for org2
approveForMyOrg org2
