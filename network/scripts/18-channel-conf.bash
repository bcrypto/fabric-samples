export PATH="$(dirname $(readlink -e ./))/bin:$PATH"
export FABRIC_CFG_PATH=`pwd`

configtxgen -profile TwoOrgsApplicationGenesis -outputBlock genesis_block.pb -channelID channel1