# TLS CA deploy

[Guide](https://hyperledger-fabric-ca.readthedocs.io/en/latest/deployguide/cadeploy.html)

## Start server
```
export PATH="$(dirname `pwd`)/bin:$PATH"
mkdir fabric-ca-server-tls
cd fabric-ca-server-tls
fabric-ca-server init -b tls-admin:tls-adminpw
fabric-ca-server start 
```
## Enroll bootstrap admin identity with TLS CA
```
cd ..
mkdir fabric-ca-client
cd fabric-ca-client
mkdir tls-ca
mkdir tls-root-cert
export FABRIC_CA_CLIENT_HOME=`pwd`
cp ../fabric-ca-server-tls/ca-cert.pem tls-root-cert/tls-ca-cert.pem
fabric-ca-client enroll -d -u https://tls-admin:tls-adminpw@apmi5:7054 --tls.certfiles tls-root-cert/tls-ca-cert.pem --enrollment.profile tls --mspdir tls-ca/tlsadmin/msp
```
## Register and enroll the organization CA bootstrap identity with the TLS CA
```
fabric-ca-client register -d --id.name rcaadmin --id.secret rcaadminpw -u https://apmi5:7054  --tls.certfiles tls-root-cert/tls-ca-cert.pem --mspdir tls-ca/tlsadmin/msp

fabric-ca-client enroll -d -u https://rcaadmin:rcaadminpw@apmi5:7054 --tls.certfiles tls-root-cert/tls-ca-cert.pem --enrollment.profile tls --csr.hosts 'apmi5,localhost' --mspdir tls-ca/rcaadmin/msp
```

# CA deploy

## Initialize the CA server
```
mkdir fabric-ca-server-org1
cd fabric-ca-server-org1
mkdir tls
cp ../fabric-ca-client/tls-ca/rcaadmin/msp/signcerts/cert.pem tls 
cp ../fabric-ca-client/tls-ca/rcaadmin/msp/keystore/* tls/key.pem
fabric-ca-server init -b rcaadmin:rcaadminpw
```

## Start the CA server
```
export PATH="$(dirname $(readlink -e ../))/bin:$PATH"
fabric-ca-server start
```

## Enroll the CA admin
```
cd ../fabric-ca-client
mkdir org1-ca
fabric-ca-client enroll -d -u https://rcaadmin:rcaadminpw@apmi5:7055 --tls.certfiles tls-root-cert/tls-ca-cert.pem --mspdir org1-ca/rcaadmin/msp
```

# Register and enroll operation server
```
fabric-ca-client register -d --id.name operation --id.secret operationpw -u https://apmi5:7054  --tls.certfiles tls-root-cert/tls-ca-cert.pem --mspdir tls-ca/tlsadmin/msp

fabric-ca-client enroll -d -u https://operation:operationpw@apmi5:7054 --tls.certfiles tls-root-cert/tls-ca-cert.pem --enrollment.profile tls --csr.hosts 'apmi5,localhost' --mspdir tls-ca/operation/msp
```

# Organization deploy

## Register and enroll user identity with the TLS CA
```
export FABRIC_CA_CLIENT_HOME=`pwd`
export PATH="$(dirname $(readlink -e ../))/bin:$PATH"
fabric-ca-client register -d --id.name org1user --id.secret org1userpw -u https://apmi5:7054  --tls.certfiles tls-root-cert/tls-ca-cert.pem --mspdir tls-ca/tlsadmin/msp

fabric-ca-client enroll -d -u https://org1user:org1userpw@apmi5:7054 --tls.certfiles tls-root-cert/tls-ca-cert.pem --enrollment.profile tls --csr.hosts 'apmi5,localhost' --mspdir tls-ca/org1user/msp
```
Clients don't need TLS certificates with server-side authorization.

## Register and enroll peer
```
fabric-ca-client register -d --id.name org1user --id.secret org1userpw -u https://apmi5:7055 --mspdir ./org1-ca/rcaadmin/msp --id.type peer --tls.certfiles tls-root-cert/tls-ca-cert.pem

fabric-ca-client enroll -u https://org1user:org1userpw@apmi5:7055 --mspdir ./org1user/msp --csr.hosts 'apmi5,localhost' --tls.certfiles tls-root-cert/tls-ca-cert.pem

mkdir org1user/tls

cp ./tls-ca/org1user/msp/signcerts/cert.pem org1user/tls 
cp ./tls-ca/org1user/msp/keystore/* org1user/tls/key.pem
```

## Register and enroll client (app)
```
fabric-ca-client register -d --id.name org1client --id.secret org1clientpw -u https://apmi5:7055 --mspdir ./org1-ca/rcaadmin/msp --id.type client --tls.certfiles tls-root-cert/tls-ca-cert.pem

fabric-ca-client enroll -u https://org1client:org1clientpw@apmi5:7055 --mspdir ./org1client/msp --csr.hosts 'apmi5,localhost' --tls.certfiles tls-root-cert/tls-ca-cert.pem
```
