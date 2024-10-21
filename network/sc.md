./network.sh deployCC -ccn events -ccp ../asset-transfer-events/chaincode-java/ -cccg ../asset-transfer-events/chaincode-java/collections_config.json -ccl java -ccep "OR('Org1MSP.peer','Org2MSP.peer')"


# timeout duration - the duration the CLI should wait for a response from
# another container before giving up
MAX_RETRY=5
# default for delay between commands
CLI_DELAY=3
# channel name defaults to "mychannel"
CHANNEL_NAME="mychannel"
# chaincode name defaults to "NA"
CC_NAME="events"
# chaincode path defaults to "NA"
CC_SRC_PATH="../asset-transfer-events/chaincode-java/"
# endorsement policy defaults to "NA". This would allow chaincodes to use the majority default policy.
CC_END_POLICY="OR('Org1MSP.peer','Org2MSP.peer')"
# collection configuration defaults to "NA"
CC_COLL_CONFIG="../asset-transfer-events/chaincode-java/collections_config.json"
# chaincode init function defaults to "NA"
CC_INIT_FCN="NA"
# use this as the default docker-compose yaml definition
COMPOSE_FILE_BASE=compose-test-net.yaml
# docker-compose.yaml file if you are using couchdb
COMPOSE_FILE_COUCH=compose-couch.yaml
# certificate authorities compose file
COMPOSE_FILE_CA=compose-ca.yaml
# use this as the default docker-compose yaml definition for org3
COMPOSE_FILE_ORG3_BASE=compose-org3.yaml
# use this as the docker compose couch file for org3
COMPOSE_FILE_ORG3_COUCH=compose-couch-org3.yaml
# certificate authorities compose file
COMPOSE_FILE_ORG3_CA=compose-ca-org3.yaml
#
# chaincode language defaults to "NA"
CC_SRC_LANGUAGE="java"
CC_RUNTIME_LANGUAGE=java
# default to running the docker commands for the CCAAS
CCAAS_DOCKER_RUN=true
# Chaincode version
CC_VERSION="1.0"
# Chaincode definition sequence
CC_SEQUENCE=1
# default database
DATABASE="leveldb"

# Get docker sock path from environment variable
SOCK="${DOCKER_HOST:-/var/run/docker.sock}"
DOCKER_SOCK="${SOCK##unix://}"


scripts/deployCC.sh $CHANNEL_NAME $CC_NAME $CC_SRC_PATH $CC_SRC_LANGUAGE $CC_VERSION $CC_SEQUENCE $CC_INIT_FCN $CC_END_POLICY $CC_COLL_CONFIG $CLI_DELAY $MAX_RETRY $VERBOSE


Using docker and docker compose
deploying chaincode on channel 'mychannel'
executing with the following
- CHANNEL_NAME: mychannel
- CC_NAME: events
- CC_SRC_PATH: ../asset-transfer-events/chaincode-java/
- CC_SRC_LANGUAGE: java
- CC_VERSION: 1.0
- CC_SEQUENCE: 1
- CC_END_POLICY: OR('Org1MSP.peer','Org2MSP.peer')
- CC_COLL_CONFIG: ../asset-transfer-events/chaincode-java/collections_config.json
- CC_INIT_FCN: NA
- DELAY: 3
- MAX_RETRY: 5
- VERBOSE: false

bash ccscripts/01-compile.sh ../asset-transfer-events/chaincode-java/

bash ccscripts/02-package.sh events ../asset-transfer-events/chaincode-java/build/install/events java 1.0

bash ccscripts/03-install.sh org1 events
bash ccscripts/03-install.sh org2 events
bash ccscripts/04-approve.sh events mychannel
bash ccscripts/05-check.sh events mychannel 
bash ccscripts/06-commit.sh events mychannel 