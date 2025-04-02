#!/bin/bash
SCRIPTDIR="$(dirname "$(realpath "$0")")"
PEERDIR="$(readlink -f "../netstar/tmp/organizations/peerOrganizations")"
source $SCRIPTDIR/../utils.sh

for (( k = 1; k < $ORG_COUNT + 1; ++k )); do
    setGlobals Org$k
echo "gateway.peer=localhost:$PEER_PORT
gateway.auth=peer0.org${k}.example.com
gateway.msp=Org${k}MSP
tls.cert.path=${PEERDIR}/org${k}.example.com/peers/peer0.org${k}.example.com/tls/ca.crt
cert.path=${PEERDIR}/org${k}.example.com/users/User1@org${k}.example.com/msp/signcerts/User1@org${k}.example.com-cert.pem
key.dir=${PEERDIR}/org${k}.example.com/users/User1@org${k}.example.com/msp/keystore
bign.cert.path=${PEERDIR}/org${k}.example.com/users/User1@org${k}.example.com/bign/cert.der
bign.key.path=${PEERDIR}/org${k}.example.com/users/User1@org${k}.example.com/bign/privkey.der
bign.key.pwd=lrlrlr
channel.name=mychannel
msg.input=msg/desadv.xml
msg.output=result.xml
msg.ref=desadv1
lang=en
country=US
gln=4812409900005,481098700054
" > ./tmp/organizations/peerOrganizations/org$k.example.com/users/User1@org$k.example.com/app.properties
    chmod 777 ./tmp/organizations/peerOrganizations/org$k.example.com/users/User1@org$k.example.com/app.properties
done




