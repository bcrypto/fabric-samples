# Start network
```
test-network$ ./network.sh up createChannel -c mychannel -ca -s couchdb
```

# Load smart-contract
```
test-network$ ./network.sh deployCC -ccn events -ccp ../asset-transfer-events/chaincode-java/ -cccg ../asset-transfer-events/chaincode-java/collections_config.json -ccl java -ccep "OR('Org1MSP.peer','Org2MSP.peer')"
```

# Load second smart-contract
```
test-network$ ./network.sh deployCC -ccn opcode -ccp ../asset-transfer-events/opcode-java/ -cccg ../asset-transfer-events/opcode-java/collections_config.json -ccl java -ccep "OR('Org1MSP.peer')"
```

# Start app
## Start shipper app
```
asset-transfer-events/app-shipper$ ./gradlew run
```

## Start reciever app
```
asset-transfer-events/app-shipper$ ./gradlew run --args './settings/rec.properties'
```

## Start app with overriding properties
```
asset-transfer-events/app-shipper$ ./gradlew run --args './settings/rec.properties msg.input=msg/recadv1.xml'
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

# Start app with generated properties
```
./gradlew run --args '../../netstar/tmp/organizations/peerOrganizations/org2.example.com/users/User1@org2.example.com/app.properties channel.name=channel-org2-org4'
```
