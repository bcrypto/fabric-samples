export PATH="$(dirname $(readlink -e ./))/bin:$PATH"
export FABRIC_CA_CLIENT_HOME=`pwd`/client

IP=${3:-10.131.44.144}
PORT=${4:-7055}

fabric-ca-client enroll -d -u https://$1$2:$1$2pw@localhost:$PORT --mspdir ../organizations/peerOrganizations/$1/clients/$2/msp --csr.hosts "apmi5,localhost,$IP" --tls.certfiles tls-root-cert/tls-ca-cert.pem
