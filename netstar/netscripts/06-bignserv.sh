  #!/bin/bash
SCRIPTDIR="$(dirname "$(realpath "$0")")"
source $SCRIPTDIR/../utils.sh
BPKI_DIR="tmp/bpki"
BPKI_SRC="../submodules/bpki/ca_server"

mkdir -p $BPKI_DIR
cp -r $BPKI_SRC/cfg $BPKI_DIR
cp $BPKI_SRC/openssl.cfg $BPKI_DIR/openssl.cfg

cd $BPKI_DIR
export OPENSSL_CONF=openssl.cfg
mkdir -p out
openssl genpkey -genparam -algorithm bign -pkeyopt params:bign-curve256v1 -out out/params256
openssl genpkey -genparam -algorithm bign -pkeyopt params:bign-curve384v1 -out out/params384
openssl genpkey -genparam -algorithm bign -pkeyopt params:bign-curve512v1 -out out/params512

echo "== 2 Creating CA0 (Root CA) =============================================="

cd out
mkdir ca0 2> /dev/null
cd ca0
cat /dev/null > index.txt
echo 01 > crlnumber
mkdir certs 2> /dev/null
cd ..
cd ..

openssl genpkey -paramfile out/params512 \
  -pkeyopt enc_params:specified -pkeyopt enc_params:cofactor \
  -out out/ca0/privkey_plain

# decode out/ca0/privkey_plain > nul

openssl pkcs8 -in out/ca0/privkey_plain -topk8 \
  -v2 belt-kwp256 -v2prf belt-hmac -iter 10000 \
  -passout pass:ca0ca0ca0 -out out/ca0/privkey

openssl pkey -in out/ca0/privkey -passin pass:ca0ca0ca0 -noout -check

source $SCRIPTDIR/decode.sh out/ca0/privkey

openssl req -new -utf8 -nameopt multiline,utf8 -config ./cfg/ca0.cfg \
  -key out/ca0/privkey -passin pass:ca0ca0ca0 -out out/ca0/csr -batch

# call decode out/ca0/csr > nul

openssl x509 -req -extfile ./cfg/ca0.cfg -extensions exts -days 3650 \
  -in out/ca0/csr -signkey out/ca0/privkey -passin pass:ca0ca0ca0 \
  -out out/ca0/cert 2> /dev/null

source $SCRIPTDIR/decode.sh out/ca0/cert

openssl ca -gencrl -name ca0 -key ca0ca0ca0 -crldays 180 -crlhours 6 \
  -crlexts crlexts -out out/ca0/crl0 -batch 2> /dev/null

source $SCRIPTDIR/decode.sh out/ca0/crl0

echo ok

echo "== 3 Creating CA1 (Republican CA) ========================================"

cd out
mkdir ca1 2> /dev/null
cd ca1
cat /dev/null > index.txt
echo 01 > crlnumber
mkdir certs 2> /dev/null
cd ..
cd ..

openssl genpkey -paramfile out/params384 \
-pkeyopt enc_params:specified -out out/ca1/privkey_plain

#call decode out/ca1/privkey_plain > nul#

openssl pkcs8 -in out/ca1/privkey_plain -topk8 \
  -v2 belt-kwp256 -v2prf belt-hmac -iter 10000 \
  -passout pass:ca1ca1ca1 -out out/ca1/privkey

openssl pkey -in out/ca1/privkey -passin pass:ca1ca1ca1 -noout -pubcheck

source $SCRIPTDIR/decode.sh out/ca1/privkey

openssl req -new -utf8 -nameopt multiline,utf8 -config ./cfg/ca1.cfg \
  -key out/ca1/privkey -passin pass:ca1ca1ca1 -out out/ca1/csr -batch

#call decode out/ca1/csr > nul#

openssl ca -name ca0 -create_serial -batch -in out/ca1/csr -days 1825 \
  -key ca0ca0ca0 -extfile ./cfg/ca1.cfg -extensions exts \
  -out out/ca1/cert -notext -utf8 2> /dev/null

source $SCRIPTDIR/decode.sh out/ca1/cert

echo ok

echo "== 4 Creating CA2 (Subordinate CA) ======================================="

cd out
mkdir ca2 2> /dev/null
cd ca2
cat /dev/null > index.txt
echo 01 > crlnumber
mkdir certs 2> /dev/null
cd ..
cd ..

openssl genpkey -paramfile out/params256 -pkeyopt enc_params:cofactor \
  -out out/ca2/privkey_plain

#call decode out/ca2/privkey_plain > nul#

openssl pkcs8 -in out/ca2/privkey_plain -topk8 \
  -v2 belt-kwp256 -v2prf belt-hmac -iter 10000 \
  -passout pass:ca2ca2ca2 -out out/ca2/privkey

source $SCRIPTDIR/decode.sh out/ca1/privkey

openssl req -new -utf8 -nameopt multiline,utf8 -config ./cfg/ca2.cfg \
  -key out/ca2/privkey -passin pass:ca2ca2ca2 -out out/ca2/csr -batch

#call decode out/ca2/csr > nul#

openssl ca -name ca1 -create_serial -batch -in out/ca2/csr \
  -key ca1ca1ca1 -extfile ./cfg/ca2.cfg -extensions exts \
  -out out/ca2/cert -notext -utf8 2> /dev/null

source $SCRIPTDIR/decode.sh out/ca2/cert

echo ok