export PATH="$(dirname $(readlink -e ./))/bin:$PATH"
export FABRIC_CA_CLIENT_HOME=`pwd`/client

fabric-ca-client register -d --id.name $1 --id.secret $1pw -u https://localhost:7053  --tls.certfiles tls-root-cert/tls-ca-cert.pem --mspdir tlsadmin/msp
