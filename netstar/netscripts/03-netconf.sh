#!/bin/bash
SCRIPTDIR="$(dirname "$(realpath "$0")")"
source $SCRIPTDIR/../utils.sh
COMPOSE_CONF="tmp"
COMPOSE_FILE_BASE=compose-test-net.yaml

SOCK="${DOCKER_HOST:-/var/run/docker.sock}"
DOCKER_SOCK="${SOCK##unix://}"

function gen_orderer() {
  ORG=${1,,}
  OUTPUT=$2
  echo "
  ${ORG}.example.com:
    container_name: ${ORG}.example.com
    image: hyperledger/fabric-orderer:latest
    labels:
      service: hyperledger-fabric
    environment:
      - FABRIC_LOGGING_SPEC=INFO
      - ORDERER_GENERAL_LISTENADDRESS=0.0.0.0
      - ORDERER_GENERAL_LISTENPORT=7050
      - ORDERER_GENERAL_LOCALMSPID=${ORG^}MSP
      - ORDERER_GENERAL_LOCALMSPDIR=/var/hyperledger/${ORG}/msp
      # enabled TLS
      - ORDERER_GENERAL_TLS_ENABLED=true
      - ORDERER_GENERAL_TLS_PRIVATEKEY=/var/hyperledger/${ORG}/tls/server.key
      - ORDERER_GENERAL_TLS_CERTIFICATE=/var/hyperledger/${ORG}/tls/server.crt
      - ORDERER_GENERAL_TLS_ROOTCAS=[/var/hyperledger/${ORG}/tls/ca.crt]
      - ORDERER_GENERAL_CLUSTER_CLIENTCERTIFICATE=/var/hyperledger/${ORG}/tls/server.crt
      - ORDERER_GENERAL_CLUSTER_CLIENTPRIVATEKEY=/var/hyperledger/${ORG}/tls/server.key
      - ORDERER_GENERAL_CLUSTER_ROOTCAS=[/var/hyperledger/${ORG}/tls/ca.crt]
      - ORDERER_GENERAL_BOOTSTRAPMETHOD=none
      - ORDERER_CHANNELPARTICIPATION_ENABLED=true
      - ORDERER_ADMIN_TLS_ENABLED=true
      - ORDERER_ADMIN_TLS_CERTIFICATE=/var/hyperledger/${ORG}/tls/server.crt
      - ORDERER_ADMIN_TLS_PRIVATEKEY=/var/hyperledger/${ORG}/tls/server.key
      - ORDERER_ADMIN_TLS_ROOTCAS=[/var/hyperledger/${ORG}/tls/ca.crt]
      - ORDERER_ADMIN_TLS_CLIENTROOTCAS=[/var/hyperledger/${ORG}/tls/ca.crt]
      - ORDERER_ADMIN_LISTENADDRESS=0.0.0.0:7053
      - ORDERER_OPERATIONS_LISTENADDRESS=${ORG}.example.com:9443
      - ORDERER_METRICS_PROVIDER=prometheus
    working_dir: /root
    command: orderer
    volumes:
        - ../organizations/ordererOrganizations/example.com/orderers/${ORG}.example.com/msp:/var/hyperledger/${ORG}/msp
        - ../organizations/ordererOrganizations/example.com/orderers/${ORG}.example.com/tls/:/var/hyperledger/${ORG}/tls
        - ${ORG}.example.com:/var/hyperledger/production/${ORG}
    ports:
      - 7050:7050
      - 7053:7053
      - 9443:9443
    networks:
      - test" >> $OUTPUT
}  

function gen_peer() {
  ORG=${1,,}
  OUTPUT=$2
  ORG_NUM=${ORG:3}
  P0PORT=$((100*ORG_NUM + 7051))
  CCPORT=$((100*ORG_NUM + 7052))
  OLPORT=$((100*ORG_NUM + 7061))
  echo "
  peer0.${ORG}.example.com:
    container_name: peer0.${ORG}.example.com
    image: hyperledger/fabric-peer:latest
    labels:
      service: hyperledger-fabric
    environment:
      - CORE_VM_ENDPOINT=unix:///host/var/run/docker.sock
      - CORE_VM_DOCKER_HOSTCONFIG_NETWORKMODE=fabric_test
      - FABRIC_CFG_PATH=/etc/hyperledger/peercfg
      - FABRIC_LOGGING_SPEC=INFO
      - CORE_PEER_TLS_ENABLED=true
      - CORE_PEER_PROFILE_ENABLED=false
      - CORE_PEER_TLS_CERT_FILE=/etc/hyperledger/fabric/tls/server.crt
      - CORE_PEER_TLS_KEY_FILE=/etc/hyperledger/fabric/tls/server.key
      - CORE_PEER_TLS_ROOTCERT_FILE=/etc/hyperledger/fabric/tls/ca.crt
      # Peer specific variables
      - CORE_PEER_ID=peer0.${ORG}.example.com
      - CORE_PEER_ADDRESS=peer0.${ORG}.example.com:$P0PORT
      - CORE_PEER_LISTENADDRESS=0.0.0.0:$P0PORT
      - CORE_PEER_CHAINCODEADDRESS=peer0.${ORG}.example.com:$CCPORT
      - CORE_PEER_CHAINCODELISTENADDRESS=0.0.0.0:$CCPORT
      - CORE_PEER_GOSSIP_BOOTSTRAP=peer0.${ORG}.example.com:$P0PORT
      - CORE_PEER_GOSSIP_EXTERNALENDPOINT=peer0.${ORG}.example.com:$P0PORT
      - CORE_PEER_LOCALMSPID=${ORG^}MSP
      - CORE_PEER_MSPCONFIGPATH=/etc/hyperledger/fabric/msp
      - CORE_OPERATIONS_LISTENADDRESS=peer0.${ORG}.example.com:$OLPORT
      - CORE_METRICS_PROVIDER=prometheus
      - CHAINCODE_AS_A_SERVICE_BUILDER_CONFIG={"peername":"peer0${ORG}"}
      - CORE_CHAINCODE_EXECUTETIMEOUT=300s
    volumes:
      - ../organizations/peerOrganizations/${ORG}.example.com/peers/peer0.${ORG}.example.com:/etc/hyperledger/fabric
      - peer0.${ORG}.example.com:/var/hyperledger/production
      - ./peercfg:/etc/hyperledger/peercfg
      - ${DOCKER_SOCK}:/host/var/run/docker.sock
    working_dir: /root
    command: peer node start
    ports:
      - $P0PORT:$P0PORT
      - $OLPORT:$OLPORT
    networks:
      - test" >> $OUTPUT
} 


function gen_head() {
  COUNT=$1
  OUTPUT=$2
  VOLUMES="peer0.org1.example.com:"
  for (( k = 2; k < $COUNT+1; ++k )); do
    VOLUMES="$VOLUMES\n  peer0.org$k.example.com:"
  done

  echo -e "volumes:
  orderer.example.com:
  $VOLUMES

networks:
  test:
    name: fabric_test

services:" > $OUTPUT
} 

mkdir -p $COMPOSE_CONF

OUTPUT=$COMPOSE_CONF/$COMPOSE_FILE_BASE

gen_head $ORG_COUNT $OUTPUT

gen_orderer Orderer $OUTPUT

for (( k = 1; k < $COUNT+1; ++k )); do
  gen_peer Org$k $OUTPUT
done