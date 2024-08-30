mkdir organizations/fabric-ca/$1
mkdir organizations/fabric-ca/$1/tls
cp client/tls-ca/$1ca/msp/signcerts/cert.pem  organizations/fabric-ca/$1/tls/ 
cp client/tls-ca/$1ca/msp/keystore/* organizations/fabric-ca/$1/tls/key.pem
