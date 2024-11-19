  #!/bin/bash
SCRIPTDIR="$(dirname "$(realpath "$0")")"
source $SCRIPTDIR/../utils.sh
export PATH="$(dirname $(readlink -e ./))/bin:$PATH"

export VERBOSE=true
ORG1_NAME=$1
ORG2_NAME=$2

# scripts/createChannel.sh $CHANNEL_NAME $CLI_DELAY $MAX_RETRY $VERBOSE

# timeout duration - the duration the CLI should wait for a response from
# another container before giving up
MAX_RETRY=5
# default for delay between commands
CLI_DELAY=3
# channel name defaults to "mychannel"
CHANNEL_NAME="channel-${ORG1_NAME,,}-${ORG2_NAME,,}"
# default database
DATABASE="leveldb"

export ORDERER_CA=${PWD}/tmp/organizations/ordererOrganizations/example.com/tlsca/tlsca.example.com-cert.pem
export ORDERER_ADMIN_TLS_SIGN_CERT=${PWD}/tmp/organizations/ordererOrganizations/example.com/orderers/orderer.example.com/tls/server.crt
export ORDERER_ADMIN_TLS_PRIVATE_KEY=${PWD}/tmp/organizations/ordererOrganizations/example.com/orderers/orderer.example.com/tls/server.key


if [ ! -d "channel-artifacts" ]; then
	mkdir channel-artifacts
fi

createChannelGenesisBlock() {
	which configtxgen
	if [ "$?" -ne 0 ]; then
		fatalln "configtxgen tool not found."
	fi
	set -x
	configtxgen -profile TwoOrgsApplicationGenesis -outputBlock ./channel-artifacts/${CHANNEL_NAME}.block -channelID $CHANNEL_NAME
	res=$?
	{ set +x; } 2>/dev/null
  verifyResult $res "Failed to generate channel configuration transaction..."
}

mkdir -p ${PWD}/tmp/configtx/${CHANNEL_NAME}
sed -e "s/__Org1__/$ORG1_NAME/g" \
    -e "s/__org1__/${ORG1_NAME,,}/g" \
  	-e "s/__Org2__/$ORG2_NAME/g" \
    -e "s/__org2__/${ORG2_NAME,,}/g" \
      ${PWD}/configtx/Org1Org2/configtx.yaml > ${PWD}/tmp/configtx/${CHANNEL_NAME}/configtx.yaml

export FABRIC_CFG_PATH=${PWD}/tmp/configtx/${CHANNEL_NAME}/

## Create channel genesis block
infoln "Generating channel genesis block '${CHANNEL_NAME}.block'"
createChannelGenesisBlock
