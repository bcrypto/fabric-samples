# fetchChannelConfig <org> <channel_id> <output_json>
# Writes the current channel config for a given channel to a JSON file
# NOTE: this must be run in a CLI container since it requires configtxlator
# PRESET: $TMPDIR, $ORDERER_CA
fetchChannelConfig() {
  CHANNEL=$1
  OUTPUT=$2

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
# PRESET: $TMPDIR
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

# joinChannel ORG
# PRESET: $CHANNEL_NAME, $BLOCKFILE, $MAX_RETRY, $DELAY 
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

## Set the anchor peers for each org in the channel
# NOTE: this must be run in a CLI container since it requires jq and configtxlator 
# PRESET: $TMPDIR
setAnchorConfig() {
  infoln "Setting anchor peer for $1..."
  ORG=$1
  setGlobals $ORG
  export HOST="peer0.${ORG,,}.example.com"
  export PORT=$PEER_PORT
# createAnchorPeerUpdate() {
  infoln "Fetching channel config for channel $CHANNEL_NAME"
  fetchChannelConfig $CHANNEL_NAME $TMPDIR/${CORE_PEER_LOCALMSPID}config.json

  infoln "Generating anchor peer update transaction for ${ORG} on channel $CHANNEL_NAME"
  set -x
  # Modify the configuration to append the anchor peer 
  jq '.channel_group.groups.Application.groups.'${CORE_PEER_LOCALMSPID}'.values += {"AnchorPeers":{"mod_policy": "Admins","value":{"anchor_peers": [{"host": "'$HOST'","port": '$PORT'}]},"version": "0"}}' $TMPDIR/${CORE_PEER_LOCALMSPID}config.json > $TMPDIR/${CORE_PEER_LOCALMSPID}modified_config.json
  { set +x; } 2>/dev/null

  # Compute a config update, based on the differences between 
  # {orgmsp}config.json and {orgmsp}modified_config.json, write
  # it as a transaction to {orgmsp}anchors.tx
  createConfigUpdate ${CHANNEL_NAME} $TMPDIR/${CORE_PEER_LOCALMSPID}config.json $TMPDIR/${CORE_PEER_LOCALMSPID}modified_config.json $TMPDIR/${CORE_PEER_LOCALMSPID}anchors.tx
}

updateAnchorPeer() {
  peer channel update -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com -c $CHANNEL_NAME -f $TMPDIR/${CORE_PEER_LOCALMSPID}anchors.tx --tls --cafile "$ORDERER_CA" >&log.txt
  res=$?
  cat log.txt
  verifyResult $res "Anchor peer update failed"
  successln "Anchor peer set for org '$CORE_PEER_LOCALMSPID' on channel '$CHANNEL_NAME'"
}

setAnchorPeer() {
  infoln "Update anchor peer for $1..."
  ORG=$1
  setGlobals $ORG
  updateAnchorPeer
}