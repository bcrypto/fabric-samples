export PATH="$(dirname $(readlink -e ./))/bin:$PATH"
mkdir client
export FABRIC_CA_CLIENT_HOME=`pwd`/client
mkdir client/tls-ca
mkdir client/tls-root-cert
cp organizations/tls-ca/ca-cert.pem client/tls-root-cert/tls-ca-cert.pem
fabric-ca-client enroll -d -u https://admin:adminpw@localhost:7053 --tls.certfiles tls-root-cert/tls-ca-cert.pem --enrollment.profile tls --mspdir tlsadmin/msp
