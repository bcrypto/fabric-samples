  #!/bin/bash
SCRIPTDIR="$(dirname "$(realpath "$0")")"
source $SCRIPTDIR/../utils.sh
export PATH="$(dirname $(readlink -e ./))/bin:$PATH"

: ${CONTAINER_CLI:="docker"}
: ${CONTAINER_CLI_COMPOSE:="${CONTAINER_CLI} compose"}
infoln "Using ${CONTAINER_CLI} and ${CONTAINER_CLI_COMPOSE}"

# default database
DATABASE="leveldb"

COMPOSE_CONF="tmp"
COMPOSE_FILE_BASE=compose-test-net.yaml

# Get docker sock path from environment variable
SOCK="${DOCKER_HOST:-/var/run/docker.sock}"
DOCKER_SOCK="${SOCK##unix://}"

# Bring up the peer and orderer nodes using docker compose.
function networkUp() {

  COMPOSE_FILES="-f $COMPOSE_CONF/${COMPOSE_FILE_BASE} "

  DOCKER_SOCK="${DOCKER_SOCK}" ${CONTAINER_CLI_COMPOSE} ${COMPOSE_FILES} up -d 2>&1

  $CONTAINER_CLI ps -a
  if [ $? -ne 0 ]; then
    fatalln "Unable to start network"
  fi
}

if ! $CONTAINER_CLI info > /dev/null 2>&1 ; then
fatalln "$CONTAINER_CLI network is required to be running to create a channel"
fi

infoln "Bringing up network"
networkUp
