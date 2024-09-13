export PATH="$(dirname $(readlink -e ./))/bin:$PATH"

export OSN_TLS_CA_ROOT_CERT=./organizations/ordererOrganizations/ord1/orderers/node1/tls/tls-ca-cert.pem
export ADMIN_TLS_SIGN_CERT=./organizations/ordererOrganizations/ord1/orderers/node1/tls/cert.pem
export ADMIN_TLS_PRIVATE_KEY=./organizations/ordererOrganizations/ord1/orderers/node1/tls/key.pem

osnadmin channel join --channelID channel1  --config-block genesis_block.pb -o 10.131.44.144:7053 --ca-file $OSN_TLS_CA_ROOT_CERT --client-cert $ADMIN_TLS_SIGN_CERT --client-key $ADMIN_TLS_PRIVATE_KEY