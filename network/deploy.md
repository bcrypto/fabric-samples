# Start TLS server
```
bash scripts/01-start-tls.sh
bash scripts/02-enroll-tls-admin.sh   
```
# Register all and enroll Org1 peers and Orderer nodes on TLS
```
bash scripts/03-tls-register.sh org1ca 
bash scripts/03-tls-register.sh org2ca 
bash scripts/03-tls-register.sh org3ca 
bash scripts/03-tls-register.sh ord1ca 
bash scripts/03-tls-register.sh org1peer
bash scripts/03-tls-register.sh org2peer
bash scripts/03-tls-register.sh org3peer
bash scripts/03-tls-register.sh ord1node1
bash scripts/03-tls-register.sh ord1node2
bash scripts/03-tls-register.sh ord1node3

bash scripts/04-tls-enroll.sh org1ca
bash scripts/04-tls-enroll.sh org1peer
bash scripts/04-tls-enroll.sh ord1ca
bash scripts/04-tls-enroll.sh ord1node1
bash scripts/04-tls-enroll.sh ord1node2
bash scripts/04-tls-enroll.sh ord1node3
```

# Enroll org1 entities
```
bash scripts/05-init-ca.sh org1 
bash scripts/06-start-ca.sh org1  
bash scripts/07-enroll-ca-admin.sh org1 
bash scripts/08-ca-register.sh org1 peer
bash scripts/08-ca-register.sh org1 client  
bash scripts/09-ca-enroll-peer.sh org1 peer
bash scripts/10-ca-enroll-client.sh org1 client 
bash scripts/12-stop-ca.sh org1
```

# Enroll orderer nodes
```
bash scripts/05-init-ca.sh ord1 
bash scripts/06-start-ca.sh ord1  
bash scripts/07-enroll-ca-admin.sh ord1 ordererOrganizations
bash scripts/08-ca-register.sh ord1 node1 orderer ordererOrganizations
bash scripts/08-ca-register.sh ord1 node2 orderer ordererOrganizations
bash scripts/08-ca-register.sh ord1 node3 orderer ordererOrganizations
bash scripts/11-ca-enroll-orderer.sh ord1 node1
bash scripts/11-ca-enroll-orderer.sh ord1 node2
bash scripts/11-ca-enroll-orderer.sh ord1 node3
bash scripts/12-stop-ca.sh ord1
```