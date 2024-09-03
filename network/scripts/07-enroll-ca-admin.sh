export PATH="$(dirname $(readlink -e ./))/bin:$PATH"
export FABRIC_CA_CLIENT_HOME=`pwd`/client

ORGTYPE=${2:-peerOrganizations}

fabric-ca-client enroll -d -u https://$1ca:$1capw@localhost:7055 --tls.certfiles tls-root-cert/tls-ca-cert.pem --mspdir ../organizations/$ORGTYPE/$1/msp