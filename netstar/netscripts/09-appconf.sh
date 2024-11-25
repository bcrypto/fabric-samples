#!/bin/bash
SCRIPTDIR="$(dirname "$(realpath "$0")")"
source $SCRIPTDIR/../utils.sh

for (( k = 1; k < $ORG_COUNT + 1; ++k )); do
    setGlobals Org$k
echo "gateway.peer=localhost:$PEER_PORT
gateway.auth=peer0.org${k}.example.com
gateway.msp=Org${k}MSP
tls.cert.path=../../netstar/tmp/organizations/peerOrganizations/org${k}.example.com/peers/peer0.org${k}.example.com/tls/ca.crt
cert.path=../../netstar/tmp/organizations/peerOrganizations/org${k}.example.com/users/User1@org${k}.example.com/msp/signcerts/cert.pem
key.dir=../../netstar/tmp/organizations/peerOrganizations/org${k}.example.com/users/User1@org${k}.example.com/msp/keystore
bign.cert.path=../../netstar/tmp/organizations/peerOrganizations/org${k}.example.com/users/User1@org${k}.example.com/bign/cert.der
bign.key.path=../../netstar/tmp/organizations/peerOrganizations/org${k}.example.com/users/User1@org${k}.example.com/bign/privkey.der
bign.key.pwd=lrlrlr
channel.name=mychannel
msg.input=msg/desadv.xml
msg.output=result.xml
msg.ref=desadv1
" > ./tmp/organizations/peerOrganizations/org$k.example.com/users/User1@org$k.example.com/app.settings
done




