SCRIPTDIR="$(dirname "$(realpath "$0")")"

ORG1_NAME=$1
ORG2_NAME=$2

source $SCRIPTDIR/05-conf.sh $ORG1_NAME $ORG2_NAME
source $SCRIPTDIR/06-create.sh $ORG1_NAME $ORG2_NAME
source $SCRIPTDIR/07-join.sh $ORG1_NAME $ORG2_NAME
source $SCRIPTDIR/08-anchor.sh $ORG1_NAME $ORG2_NAME
source $SCRIPTDIR/09-update.sh $ORG1_NAME $ORG2_NAME
