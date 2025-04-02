# Start network
## Remove old network
```
bash netscripts/05-netdown.sh
rm -rf tmp
```

## Change network size
```
sed -iE "s/ORG_COUNT=.*$/ORG_COUNT=20/" utils.sh
```

## Create new network
```
mkdir tmp
bash netscripts/02-orgs.sh 
bash netscripts/03-netconf.sh 
bash netscripts/04-netup.sh
```

## Add BPKI
```
bash netscripts/06-bignserv.sh  
bash netscripts/08-certs.sh
bash netscripts/09-appconf.sh 
```

### Run BPKI in Docker
```
docker compose build
docker compose run bpki_ca
```

## Add channels and SC
```
bash chscripts/20-star.sh

bash ccscripts/10-install.sh  
bash ccscripts/20-star.sh 
```

## Test
```
cd ../asset-transfer-events/app-shipper
bash test.sh
```
