#!/bin/bash
SCRIPTDIR="$(dirname "$(realpath "$0")")"

# bash ccscripts/11-channel.sh <chaincode> <org1> <org2>
source $SCRIPTDIR/04-approve.sh $1 $2 $3
source $SCRIPTDIR/05-check.sh $1 $2 $3
source $SCRIPTDIR/06-commit.sh $1 $2 $3