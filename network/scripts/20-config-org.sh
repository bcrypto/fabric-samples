node_type=$1
org=$2

files=(organizations/${node_type}Organizations/${org}/msp/cacerts/*)    
ca_cert=$(basename ${files[0]})
# Create local MSP config.yaml
echo "NodeOUs:
  Enable: true
  ClientOUIdentifier:
    Certificate: cacerts/${ca_cert}
    OrganizationalUnitIdentifier: client
  PeerOUIdentifier:
    Certificate: cacerts/${ca_cert}
    OrganizationalUnitIdentifier: peer
  AdminOUIdentifier:
    Certificate: cacerts/${ca_cert}
    OrganizationalUnitIdentifier: admin
  OrdererOUIdentifier:
    Certificate: cacerts/${ca_cert}
    OrganizationalUnitIdentifier: orderer" > organizations/${node_type}Organizations/${org}/msp/config.yaml