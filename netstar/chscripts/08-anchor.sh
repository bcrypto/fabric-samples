#!/bin/bash
SCRIPTDIR="$(dirname "$(realpath "$0")")"
source $SCRIPTDIR/../utils.sh
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


#export CORE_PEER_TLS_ENABLED=true
#export ORDERER_CA=${PWD}/organizations/ordererOrganizations/example.com/tlsca/tlsca.example.com-cert.pem
export PEER0_ORG1_CA=${PWD}/organizations/peerOrganizations/org1.example.com/tlsca/tlsca.org1.example.com-cert.pem
export PEER0_ORG2_CA=${PWD}/organizations/peerOrganizations/org2.example.com/tlsca/tlsca.org2.example.com-cert.pem
export PEER0_ORG3_CA=${PWD}/organizations/peerOrganizations/org3.example.com/tlsca/tlsca.org3.example.com-cert.pem
#export ORDERER_ADMIN_TLS_SIGN_CERT=${PWD}/organizations/ordererOrganizations/example.com/orderers/orderer.example.com/tls/server.crt
#export ORDERER_ADMIN_TLS_PRIVATE_KEY=${PWD}/organizations/ordererOrganizations/example.com/orderers/orderer.example.com/tls/server.key

# fetchChannelConfig <org> <channel_id> <output_json>
# Writes the current channel config for a given channel to a JSON file
# NOTE: this must be run in a CLI container since it requires configtxlator
fetchChannelConfig() {
  ORG=$1
  CHANNEL=$2
  OUTPUT=$3

  infoln "Fetching the most recent configuration block for the channel"
  set -x
  peer channel fetch config $TMPDIR/config_block.pb -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com -c $CHANNEL --tls --cafile "$ORDERER_CA"
  { set +x; } 2>/dev/null

  infoln "Decoding config block to JSON and isolating config to ${OUTPUT}"
  set -x
  configtxlator proto_decode --input $TMPDIR/config_block.pb --type common.Block --output $TMPDIR/config_block.json
  jq .data.data[0].payload.data.config $TMPDIR/config_block.json >"${OUTPUT}"
  { set +x; } 2>/dev/null
}

# createConfigUpdate <channel_id> <original_config.json> <modified_config.json> <output.pb>
# Takes an original and modified config, and produces the config update tx
# which transitions between the two
# NOTE: this must be run in a CLI container since it requires configtxlator
createConfigUpdate() {
  CHANNEL=$1
  ORIGINAL=$2
  MODIFIED=$3
  OUTPUT=$4

  set -x
  configtxlator proto_encode --input "${ORIGINAL}" --type common.Config --output $TMPDIR/original_config.pb
  configtxlator proto_encode --input "${MODIFIED}" --type common.Config --output $TMPDIR/modified_config.pb
  configtxlator compute_update --channel_id "${CHANNEL}" --original $TMPDIR/original_config.pb --updated $TMPDIR/modified_config.pb --output $TMPDIR/config_update.pb
  configtxlator proto_decode --input $TMPDIR/config_update.pb --type common.ConfigUpdate --output $TMPDIR/config_update.json
  echo '{"payload":{"header":{"channel_header":{"channel_id":"'$CHANNEL'", "type":2}},"data":{"config_update":'$(cat $TMPDIR/config_update.json)'}}}' | jq . >$TMPDIR/config_update_in_envelope.json
  configtxlator proto_encode --input $TMPDIR/config_update_in_envelope.json --type common.Envelope --output "${OUTPUT}"
  { set +x; } 2>/dev/null
}

## Set the anchor peers for each org in the channel
setAnchorPeer() {
  infoln "Setting anchor peer for $1..."
  ORG=$1
  setGlobals $ORG
  export HOST="peer0.${ORG,,}.example.com"
  export PORT=$PEER_PORT
# NOTE: this must be run in a CLI container since it requires jq and configtxlator 
# createAnchorPeerUpdate() {
  infoln "Fetching channel config for channel $CHANNEL_NAME"
  fetchChannelConfig $ORG $CHANNEL_NAME $TMPDIR/${CORE_PEER_LOCALMSPID}config.json

  infoln "Generating anchor peer update transaction for Org${ORG} on channel $CHANNEL_NAME"
  set -x
  # Modify the configuration to append the anchor peer 
  jq '.channel_group.groups.Application.groups.'${CORE_PEER_LOCALMSPID}'.values += {"AnchorPeers":{"mod_policy": "Admins","value":{"anchor_peers": [{"host": "'$HOST'","port": '$PORT'}]},"version": "0"}}' $TMPDIR/${CORE_PEER_LOCALMSPID}config.json > $TMPDIR/${CORE_PEER_LOCALMSPID}modified_config.json
  { set +x; } 2>/dev/null

  # Compute a config update, based on the differences between 
  # {orgmsp}config.json and {orgmsp}modified_config.json, write
  # it as a transaction to {orgmsp}anchors.tx
  createConfigUpdate ${CHANNEL_NAME} $TMPDIR/${CORE_PEER_LOCALMSPID}config.json $TMPDIR/${CORE_PEER_LOCALMSPID}modified_config.json $TMPDIR/${CORE_PEER_LOCALMSPID}anchors.tx
}

setAnchorPeer $ORG1_NAME
setAnchorPeer $ORG2_NAME

successln "Config for '$CHANNEL_NAME' updated"
