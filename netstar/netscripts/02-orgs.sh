  #!/bin/bash
SCRIPTDIR="$(dirname "$(realpath "$0")")"
source $SCRIPTDIR/../utils.sh
export PATH="$(dirname $(readlink -e ./))/bin:$PATH"
export FABRIC_CFG_PATH=$PWD/../config/
CRYPTO="cryptogen"
CRYPTOGEN_CONF="tmp/cryptogen"
ORG_CONFIG="../test-network/organizations"

function gen_crypto_config() {
  ORG=$1
  if [ ${ORG::3} == Org ]; then
    echo "PeerOrgs:
  - Name: ${ORG}
    Domain: ${ORG,,}.example.com
    EnableNodeOUs: true
    Template:
      Count: 1
      SANS:
        - localhost
    Users:
      Count: 1" > $CRYPTOGEN_CONF/crypto-config-${ORG,,}.yaml
  else 
    echo "OrdererOrgs:
  - Name: ${ORG}
    Domain: example.com
    EnableNodeOUs: true
    Specs:
      - Hostname: ${ORG,,}
        SANS:
          - localhost" > $CRYPTOGEN_CONF/crypto-config-${ORG,,}.yaml
  fi
}  

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

function generateOrgCCP() {
  #!/bin/bash
  ORG=$1
  ORG_LITTLE=${ORG,,}
  ORG_DOMAIN=${ORG,,}.example.com
  ORG_NUM=${ORG:3}
  P0PORT=$((100*ORG_NUM + 7051))
  CAPORT=$((100*ORG_NUM + 7054))
  PEERPEM=organizations/peerOrganizations/${ORG_LITTLE}.example.com/tlsca/tlsca.${ORG_LITTLE}.example.com-cert.pem
  CAPEM=organizations/peerOrganizations/${ORG_LITTLE}.example.com/ca/ca.${ORG_LITTLE}.example.com-cert.pem

  infoln "Generating CCP files for ${ORG}"
  echo "$(json_ccp $ORG $ORG_LITTLE $ORG_DOMAIN $P0PORT $CAPORT $PEERPEM $CAPEM)" > organizations/peerOrganizations/${ORG_LITTLE}.example.com/connection-${ORG_LITTLE}.json
  echo "$(yaml_ccp $ORG_NUM $ORG_LITTLE $ORG_DOMAIN $P0PORT $CAPORT $PEERPEM $CAPEM)" > organizations/peerOrganizations/${ORG_LITTLE}.example.com/connection-${ORG_LITTLE}.yaml
}

function generateCCP() {
  #!/bin/bash
  infoln "Generating CCP files"
  for (( k = 1; k < $ORG_COUNT + 1; ++k )); do
    generateOrgCCP Org$k
  done
}

function createOrg() {
  ORG=$1
  infoln "Creating ${ORG^} Identities"

  set -x
  cryptogen generate --config=$CRYPTOGEN_CONF/crypto-config-${ORG,,}.yaml --output="organizations"
  res=$?
  { set +x; } 2>/dev/null
  if [ $res -ne 0 ]; then
    fatalln "Failed to generate certificates..."
  fi
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

    infoln "Generating configs for cryptogen tool"

    gen_crypto_config Orderer

    for (( k = 1; k < $ORG_COUNT + 1; ++k )); do
      gen_crypto_config Org$k
    done

    infoln "Generating certificates using cryptogen tool"

    createOrg Orderer

    for (( k = 1; k < $ORG_COUNT + 1; ++k )); do
      createOrg Org$k
    done
  fi
}  

mkdir -p $CRYPTOGEN_CONF

createOrgs

generateCCP
