export PATH="$(dirname $(readlink -e ./))/bin:$PATH"
export FABRIC_CA_CLIENT_HOME=`pwd`/client

ROLE=${3:-peer}

fabric-ca-client register -d --id.name $1$2 --id.secret $1$2pw -u https://localhost:7055 --mspdir ../organizations/peerOrganizations/$1/msp --id.type $ROLE --tls.certfiles tls-root-cert/tls-ca-cert.pem
