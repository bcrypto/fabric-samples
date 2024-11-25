  #!/bin/bash
SCRIPTDIR="$(dirname "$(realpath "$0")")"
source $SCRIPTDIR/../utils.sh
BPKI_DIR="tmp/bpki"

OUT_DIR=${1:-out}
PASS=${2:-lrlrlr}
mkdir -p $OUT_DIR
KEY_DIR=`realpath $OUT_DIR`

pushd $BPKI_DIR
export OPENSSL_CONF=openssl.cfg

echo "== Creating End Entity in $KEY_DIR"

openssl genpkey -paramfile out/params256 -out $KEY_DIR/privkey_plain

openssl pkcs8 -in $KEY_DIR/privkey_plain -topk8 \
  -v2 belt-kwp256 -v2prf belt-hmac -iter 10000 \
  -passout pass:$PASS -out $KEY_DIR/privkey

source $SCRIPTDIR/decode.sh $KEY_DIR/privkey

openssl req -new -utf8 -nameopt multiline,utf8 -config ./cfg/lr.cfg \
  -reqexts reqexts -key $KEY_DIR/privkey -passin pass:$PASS \
  -out $KEY_DIR/csr -batch

openssl ca -name ca1 -batch -in $KEY_DIR/csr -key ca1ca1ca1 -days 365 \
  -extfile ./cfg/lr.cfg -extensions exts -out $KEY_DIR/cert -notext \
  -utf8 2> /dev/null

source $SCRIPTDIR/decode.sh $KEY_DIR/cert

popd