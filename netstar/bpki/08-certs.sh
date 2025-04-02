#!/bin/bash
SCRIPTDIR="$(dirname "$(realpath "$0")")"
source $SCRIPTDIR/../utils.sh

for (( k = 1; k < $ORG_COUNT + 1; ++k )); do
    source $SCRIPTDIR/07-keygen.sh ./tmp/organizations/peerOrganizations/org$k.example.com/users/User1@org$k.example.com/bign
done

