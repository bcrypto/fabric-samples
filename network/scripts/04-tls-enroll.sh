export PATH="$(dirname $(readlink -e ./))/bin:$PATH"
export FABRIC_CA_CLIENT_HOME=`pwd`/client

IP=${2:-10.131.44.144}

fabric-ca-client enroll -d -u https://$1:$1pw@localhost:7053 --tls.certfiles tls-root-cert/tls-ca-cert.pem --enrollment.profile tls --csr.hosts "apmi5,localhost,$IP" --mspdir tls-ca/$1/msp