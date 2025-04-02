Test on complete graph topology network with one smart contract
```
# Clear configs
rm -rf tmp
mkdir tmp
# Create orgs and nodes
time bash netscripts/02-orgs.sh
time bash netscripts/03-netconf.sh
time bash netscripts/04-netup.sh
# Generate BPKI keys, certificates and user settings
time bash netscripts/06-bignserv.sh
time bash netscripts/08-certs.sh
time bash netscripts/09-appconf.sh
# Create channels
time bash chscripts/20-star.sh
# Install smart contracts
time bash ccscripts/10-install.sh
# Apply smart contract to channel
time bash ccscripts/20-star.sh
# Test with 1 transaction to each channel
cd ../asset-transfer-events/app-shipper
time bash test.sh 20 1
```