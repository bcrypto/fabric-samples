package org.hyperledger.fabric.samples.privatedata;

import javax.xml.crypto.*;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

public class X509KeySelector extends KeySelector {
    public KeySelectorResult select(KeyInfo keyInfo,
                                    KeySelector.Purpose purpose,
                                    AlgorithmMethod method,
                                    XMLCryptoContext context)
            throws KeySelectorException {
        for (XMLStructure info : keyInfo.getContent()) {
            if (!(info instanceof X509Data))
                continue;
            X509Data x509Data = (X509Data) info;
            for (Object o : x509Data.getContent()) {
                if (!(o instanceof X509Certificate))
                    continue;
                final PublicKey key = ((X509Certificate) o).getPublicKey();
                return () -> key;
            }
        }
        throw new KeySelectorException("No key found!");
    }
}
