#!/bin/bash
SCRIPTDIR="$(dirname "$(realpath "$0")")"
source $SCRIPTDIR/utils.sh
export PATH="$(dirname $(readlink -e ./))/bin:$PATH"
export FABRIC_CFG_PATH=$PWD/../config/

export CORE_PEER_TLS_ENABLED=true
ORG=$1
CC_NAME=$2

PACKAGE_ID=$(peer lifecycle chaincode calculatepackageid ${CC_NAME}.tar.gz)

# installChaincode PEER ORG
function installChaincode() {
  ORG=$1
  setGlobals $ORG
  set -x
  peer lifecycle chaincode queryinstalled --output json | jq -r 'try (.installed_chaincodes[].package_id)' | grep ^${PACKAGE_ID}$ >&log.txt
  if test $? -ne 0; then
    peer lifecycle chaincode install ${CC_NAME}.tar.gz >&log.txt
    res=$?
  fi
  { set +x; } 2>/dev/null
  cat log.txt
  verifyResult $res "Chaincode installation on peer0.${ORG} has failed"
  successln "Chaincode is installed on peer0.${ORG}"
}

# queryInstalled PEER ORG
function queryInstalled() {
  ORG=$1
  setGlobals $ORG
  set -x
  peer lifecycle chaincode queryinstalled --output json | jq -r 'try (.installed_chaincodes[].package_id)' | grep ^${PACKAGE_ID}$ >&log.txt
  res=$?
  { set +x; } 2>/dev/null
  cat log.txt
  verifyResult $res "Query installed on peer0.${ORG} has failed"
  successln "Query installed successful on peer0.${ORG} on channel"
}

## Install chaincode on peer0.org1, peer0.org2 and operator
infoln "Installing chaincode on peer0.org1..."
installChaincode $ORG

## query whether the chaincode is installed
queryInstalled $ORG
