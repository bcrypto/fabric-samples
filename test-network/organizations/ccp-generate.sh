#!/bin/bash

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
        organizations/ccp-template.json
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
        organizations/ccp-template.yaml | sed -e $'s/\\\\n/\\\n          /g'
}

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

ORG=Operator
ORG_LITTLE=operator
ORG_DOMAIN=operator.by
P0PORT=11051
CAPORT=11054
PEERPEM=organizations/peerOrganizations/operator.by/tlsca/tlsca.operator.by-cert.pem
CAPEM=organizations/peerOrganizations/operator.by/ca/ca.operator.by-cert.pem

echo "$(json_ccp $ORG $ORG_LITTLE $ORG_DOMAIN $P0PORT $CAPORT $PEERPEM $CAPEM)" > organizations/peerOrganizations/operator.by/connection-operator.json
echo "$(yaml_ccp $ORG $ORG_LITTLE $ORG_DOMAIN $P0PORT $CAPORT $PEERPEM $CAPEM)" > organizations/peerOrganizations/operator.by/connection-operator.yaml
