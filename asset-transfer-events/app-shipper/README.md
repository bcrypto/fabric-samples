# Start network
```
test-network$ ./network.sh up createChannel -c mychannel -ca -s couchdb
```

# Load smart-contract
```
test-network$ ./network.sh deployCC -ccn events -ccp ../asset-transfer-events/chaincode-java/ -cccg ../asset-transfer-events/chaincode-java/collections_config.json -ccl java -ccep "OR('Org1MSP.peer','Org2MSP.peer')"
```

# Start shipper app
```
asset-transfer-events/app-shipper$ ./gradlew run
```

# Stop test network
```
test-network$ ./network.sh down   
```

# Connect to CouchDB
```
http://127.0.0.1:5984/_utils
login:admin
pass:adminpw
```