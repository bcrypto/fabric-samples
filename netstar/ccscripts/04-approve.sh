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
CC_SEQUENCE=1
INIT_REQUIRED=""
VERBOSE=false
TMPDIR=./tmp
CC_END_POLICY="OR('${ORG1_NAME^}MSP.peer','${ORG2_NAME^}MSP.peer')"
CC_COLL_CONFIG=$TMPDIR/configtx/${CHANNEL_NAME}/collections_config.json
PACKAGE_ID=$(peer lifecycle chaincode calculatepackageid $TMPDIR/${CC_NAME}.tar.gz)

sed -e "s/__Org1__/${ORG1_NAME^}/g" \
  	-e "s/__Org2__/${ORG2_NAME^}/g" \
      ${PWD}/configtx/Org1Org2/collections_config.json > ${CC_COLL_CONFIG}

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
approveForMyOrg ${ORG1_NAME,,}

## now approve also for org2
approveForMyOrg ${ORG2_NAME,,}
