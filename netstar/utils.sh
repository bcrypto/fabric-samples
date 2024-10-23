#!/bin/bash

C_RESET='\033[0m'
C_RED='\033[0;31m'
C_GREEN='\033[0;32m'
C_BLUE='\033[0;34m'
C_YELLOW='\033[1;33m'

# println echos string
function println() {
  echo -e "$1"
}

# errorln echos i red color
function errorln() {
  println "${C_RED}${1}${C_RESET}"
}

# successln echos in green color
function successln() {
  println "${C_GREEN}${1}${C_RESET}"
}

# infoln echos in blue color
function infoln() {
  println "${C_BLUE}${1}${C_RESET}"
}

# warnln echos in yellow color
function warnln() {
  println "${C_YELLOW}${1}${C_RESET}"
}

# fatalln echos in red color and exits with fail status
function fatalln() {
  errorln "$1"
  exit 1
}

function verifyResult() {
  if [ $1 -ne 0 ]; then
    fatalln "$2"
  fi
}

ORG_COUNT=5
NET=${PWD}
PEER_ORGANIZATIONS=$NET/organizations/peerOrganizations
ORDERER_ORGANIZATIONS=$NET/organizations/ordererOrganizations
export CORE_PEER_TLS_ENABLED=true
export ORDERER_CA=$ORDERER_ORGANIZATIONS/example.com/tlsca/tlsca.example.com-cert.pem
export ORDERER_ADMIN_TLS_SIGN_CERT=$ORDERER_ORGANIZATIONS/example.com/orderers/orderer.example.com/tls/server.crt
export ORDERER_ADMIN_TLS_PRIVATE_KEY=$ORDERER_ORGANIZATIONS/example.com/orderers/orderer.example.com/tls/server.key


# Set environment variables for the peer org
setGlobals() {
  ORG=$1
  ORG_NUM=${ORG:3}
  PEER_PORT=$(( 100*ORG_NUM + 7051))
  infoln "Using organization ${ORG^}"
  if [ $ORG_NUM -le $ORG_COUNT ]; then
    export CORE_PEER_LOCALMSPID="${ORG^}MSP"
    export CORE_PEER_TLS_ROOTCERT_FILE=$PEER_ORGANIZATIONS/${ORG}.example.com/tlsca/tlsca.${ORG}.example.com-cert.pem
    export CORE_PEER_MSPCONFIGPATH=$PEER_ORGANIZATIONS/${ORG}.example.com/users/Admin@${ORG}.example.com/msp
    export CORE_PEER_ADDRESS=localhost:$PEER_PORT
  else
    errorln "ORG Unknown"
  fi

  if [ "$VERBOSE" == "true" ]; then
    env | grep CORE
  fi
}

export -f errorln
export -f successln
export -f infoln
export -f warnln
export -f verifyResult
