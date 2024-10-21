  #!/bin/bash
SCRIPTDIR="$(dirname "$(realpath "$0")")"
source $SCRIPTDIR/utils.sh
export PATH="$(dirname $(readlink -e ./))/bin:$PATH"
export FABRIC_CFG_PATH=$PWD/../config/
CRYPTO="cryptogen"
CRYPTOGEN_CONFIG="../test-network/organizations/cryptogen"
ORG_CONFIG="../test-network/organizations"
  

function one_line_pem {
  echo "`awk 'NF {sub(/\\n/, ""); printf "%s\\\\\\\n",$0;}' $1`"
}

function json_ccp {
  local PP=$(one_line_pem $6)
  local CP=$(one_line_pem $7)
  sed -e "s/\${ORG}/$1/"	\
    -e "s/\${ORG_LITTLE}/$2/" \
  -e "s/\${ORG_DOMAIN}/$3/" \
      -e "s/\${P0PORT}/$4/" \
      -e "s/\${CAPORT}/$5/" \
      -e "s#\${PEERPEM}#$PP#" \
      -e "s#\${CAPEM}#$CP#" \
      $ORG_CONFIG/ccp-template.json
}

function yaml_ccp {
  local PP=$(one_line_pem $6)
  local CP=$(one_line_pem $7)
  sed -e "s/\${ORG}/$1/" \
    -e "s/\${ORG_LITTLE}/$2/" \
  -e "s/\${ORG_DOMAIN}/$3/" \
      -e "s/\${P0PORT}/$4/" \
      -e "s/\${CAPORT}/$5/" \
      -e "s#\${PEERPEM}#$PP#" \
      -e "s#\${CAPEM}#$CP#" \
      $ORG_CONFIG/ccp-template.yaml | sed -e $'s/\\\\n/\\\n          /g'
 }

function generateCCP() {
  #!/bin/bash

  ORG=Org1
  ORG_LITTLE=org1
  ORG_DOMAIN=org1.example.com
  P0PORT=7051
  CAPORT=7054
  PEERPEM=organizations/peerOrganizations/org1.example.com/tlsca/tlsca.org1.example.com-cert.pem
  CAPEM=organizations/peerOrganizations/org1.example.com/ca/ca.org1.example.com-cert.pem

  echo "$(json_ccp $ORG $ORG_LITTLE $ORG_DOMAIN $P0PORT $CAPORT $PEERPEM $CAPEM)" > organizations/peerOrganizations/org1.example.com/connection-org1.json
  echo "$(yaml_ccp $ORG $ORG_LITTLE $ORG_DOMAIN $P0PORT $CAPORT $PEERPEM $CAPEM)" > organizations/peerOrganizations/org1.example.com/connection-org1.yaml

  ORG=Org2
  ORG_LITTLE=org2
  ORG_DOMAIN=org2.example.com
  P0PORT=9051
  CAPORT=8054
  PEERPEM=organizations/peerOrganizations/org2.example.com/tlsca/tlsca.org2.example.com-cert.pem
  CAPEM=organizations/peerOrganizations/org2.example.com/ca/ca.org2.example.com-cert.pem

  echo "$(json_ccp $ORG $ORG_LITTLE $ORG_DOMAIN $P0PORT $CAPORT $PEERPEM $CAPEM)" > organizations/peerOrganizations/org2.example.com/connection-org2.json
  echo "$(yaml_ccp $ORG $ORG_LITTLE $ORG_DOMAIN $P0PORT $CAPORT $PEERPEM $CAPEM)" > organizations/peerOrganizations/org2.example.com/connection-org2.yaml
}

# Create Organization crypto material using cryptogen or CAs
function createOrgs() {
  if [ -d "organizations/peerOrganizations" ]; then
    rm -Rf organizations/peerOrganizations && rm -Rf organizations/ordererOrganizations
  fi

  # Create crypto material using cryptogen
  if [ "$CRYPTO" == "cryptogen" ]; then
    which cryptogen
    if [ "$?" -ne 0 ]; then
      fatalln "cryptogen tool not found. exiting"
    fi
    infoln "Generating certificates using cryptogen tool"

    infoln "Creating Org1 Identities"

    set -x
    cryptogen generate --config=$CRYPTOGEN_CONFIG/crypto-config-org1.yaml --output="organizations"
    res=$?
    { set +x; } 2>/dev/null
    if [ $res -ne 0 ]; then
      fatalln "Failed to generate certificates..."
    fi

    infoln "Creating Org2 Identities"

    set -x
    cryptogen generate --config=$CRYPTOGEN_CONFIG/crypto-config-org2.yaml --output="organizations"
    res=$?
    { set +x; } 2>/dev/null
    if [ $res -ne 0 ]; then
      fatalln "Failed to generate certificates..."
    fi

    infoln "Creating Orderer Org Identities"

    set -x
    cryptogen generate --config=$CRYPTOGEN_CONFIG/crypto-config-orderer.yaml --output="organizations"
    res=$?
    { set +x; } 2>/dev/null
    if [ $res -ne 0 ]; then
      fatalln "Failed to generate certificates..."
    fi
  fi

  infoln "Generating CCP files for Org1 and Org2"
  generateCCP
}  

# generate artifacts if they don't exist
#if [ ! -d "organizations/peerOrganizations" ]; then
  createOrgs
#fi