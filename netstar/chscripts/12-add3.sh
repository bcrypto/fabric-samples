#!/bin/bash
SCRIPTDIR="$(dirname "$(realpath "$0")")"
source $SCRIPTDIR/../utils.sh
source $SCRIPTDIR/chutils.sh
export PATH="$(dirname $(readlink -e ./))/bin:$PATH"

  # Use the CLI container to create the configuration transaction needed to add
  # Org3 to the network
#   infoln "Generating and submitting config tx to add Org3"
#   ${CONTAINER_CLI} exec cli ./scripts/org3-scripts/updateChannelConfig.sh $CHANNEL_NAME $CLI_DELAY $CLI_TIMEOUT $VERBOSE
#   if [ $? -ne 0 ]; then
#     fatalln "ERROR !!!! Unable to create config tx"
#   fi

CHANNEL_NAME="$1"
NEW_ORG="$2"
TMPDIR=./tmp
: ${CHANNEL_NAME:="mychannel"}
: ${DELAY:="3"}
: ${TIMEOUT:="10"}
: ${VERBOSE:="false"}
COUNTER=1
MAX_RETRY=5

export FABRIC_CFG_PATH=$TMPDIR/peercfg

infoln "Creating config transaction to add org3 to network"

# Fetch the config for the channel, writing it to config.json
setGlobals Org1
fetchChannelConfig ${CHANNEL_NAME} $TMPDIR/config.json

function generateOrg3Definition() {
  which configtxgen
  if [ "$?" -ne 0 ]; then
    fatalln "configtxgen tool not found. exiting"
  fi
  infoln "Generating ${NEW_ORG^} organization definition"
  export FABRIC_CFG_PATH=$TMPDIR/configtx/channel-org3-org5
  set -x
  configtxgen -printOrg ${NEW_ORG^}MSP > $TMPDIR/organizations/peerOrganizations/${NEW_ORG,,}.example.com/${NEW_ORG,,}.json
  res=$?
  { set +x; } 2>/dev/null
  if [ $res -ne 0 ]; then
    fatalln "Failed to generate ${NEW_ORG^} organization definition..."
  fi
}

setGlobals Org3

generateOrg3Definition

# Modify the configuration to append the new org
set -x
jq -s ".[0] * {\"channel_group\":{\"groups\":{\"Application\":{\"groups\": {\"${NEW_ORG^}MSP\":.[1]}}}}}" $TMPDIR/config.json $TMPDIR/organizations/peerOrganizations/${NEW_ORG,,}.example.com/${NEW_ORG,,}.json > $TMPDIR/modified_config.json
{ set +x; } 2>/dev/null

# Compute a config update, based on the differences between config.json and modified_config.json, write it as a transaction to org3_update_in_envelope.pb
createConfigUpdate ${CHANNEL_NAME} $TMPDIR/config.json $TMPDIR/modified_config.json $TMPDIR/${NEW_ORG,,}_update_in_envelope.pb


# signConfigtxAsPeerOrg <org> <configtx.pb>
# Set the peerOrg admin of an org and sign the config update
signConfigtxAsPeerOrg() {
  ORG=$1
  CONFIGTXFILE=$2
  setGlobals $ORG
  set -x
  peer channel signconfigtx -f "${CONFIGTXFILE}"
  { set +x; } 2>/dev/null
}

export FABRIC_CFG_PATH=$TMPDIR/peercfg

infoln "Signing config transaction"
signConfigtxAsPeerOrg Org1 $TMPDIR/org3_update_in_envelope.pb

infoln "Submitting transaction from a different peer (peer0.org2) which also signs it"
signConfigtxAsPeerOrg Org2 $TMPDIR/org3_update_in_envelope.pb

set -x
peer channel update -f $TMPDIR/org3_update_in_envelope.pb -c ${CHANNEL_NAME} -o orderer.example.com:7050 --ordererTLSHostnameOverride orderer.example.com --tls --cafile "$ORDERER_CA"
{ set +x; } 2>/dev/null

successln "Config transaction to add org3 to network submitted"
