export PATH="$(dirname $(readlink -e ./))/bin:$PATH"
export FABRIC_CA_CLIENT_HOME=`pwd`/client

IP=${3:-10.131.44.144}
SERVER=${4:-10.131.44.144}
PORT=${5:-7055}

fabric-ca-client enroll -d -u https://$1$2:$1$2pw@$SERVER:$PORT --mspdir ../organizations/peerOrganizations/$1/peers/$2/msp --csr.hosts "apmi5,localhost,$IP" --tls.certfiles tls-root-cert/tls-ca-cert.pem

mkdir -p organizations/peerOrganizations/$1/peers/$2/tls

cp client/tls-ca/$1$2/msp/signcerts/cert.pem organizations/peerOrganizations/$1/peers/$2/tls 
cp client/tls-ca/$1$2/msp/keystore/* organizations/peerOrganizations/$1/peers/$2/tls/key.pem