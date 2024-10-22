./network.sh up createChannel -c mychannel    


. scripts/utils.sh

: ${CONTAINER_CLI:="docker"}
: ${CONTAINER_CLI_COMPOSE:="${CONTAINER_CLI} compose"}
infoln "Using ${CONTAINER_CLI} and ${CONTAINER_CLI_COMPOSE}"

# Using crpto vs CA. default is cryptogen
CRYPTO="cryptogen"
# timeout duration - the duration the CLI should wait for a response from
# another container before giving up
MAX_RETRY=5
# default for delay between commands
CLI_DELAY=3
# channel name defaults to "mychannel"
CHANNEL_NAME="mychannel"
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
# default database
DATABASE="leveldb"

# Get docker sock path from environment variable
SOCK="${DOCKER_HOST:-/var/run/docker.sock}"
DOCKER_SOCK="${SOCK##unix://}"

bash chscripts/01-check.sh
bash chscripts/02-orgs.sh
bash chscripts/03-netup.sh
#bash chscripts/04-netdown.sh

bash chscripts/05-conf.sh
bash chscripts/06-create.sh
bash chscripts/07-join.sh
bash chscripts/07-join.sh
bash chscripts/08-anchor.sh
