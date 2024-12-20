#!/bin/bash

ORG_COUNT=${1:-5}

for((i=1;i<10;i++)); do 
    echo "$i "
    for (( k = 1; k < $ORG_COUNT + 1; ++k )); do
        for (( j = k + 1; j < $ORG_COUNT + 1; ++j )); do
            echo "$i ($k, $j)"
            nohup ./gradlew run --args "../../netstar/tmp/organizations/peerOrganizations/org$k.example.com/users/User1@org$k.example.com/app.properties channel.name=channel-org$k-org$j mode=command channel.action=add note.action=add" & 
        done
    done
done

