#!/bin/bash

ORG_COUNT=${1:-5}
CYCLES=${2:-2}

for(( i=0; i < $CYCLES; i++)); do 
    echo "$i "
    for (( k = 2; k < $ORG_COUNT + 1; ++k )); do
        echo "$i (1, $k)"
        ./gradlew run --args "../../netstar/tmp/organizations/peerOrganizations/org$k.example.com/users/User1@org$k.example.com/app.properties channel.name=channel-org1-org$k mode=command channel.action=add note.action=add"  
        done
    done
done
