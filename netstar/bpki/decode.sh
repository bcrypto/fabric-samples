#!/bin/bash

openssl asn1parse -in "$1" -inform pem -out "$1.der" > /dev/null
chmod 777 $1.der