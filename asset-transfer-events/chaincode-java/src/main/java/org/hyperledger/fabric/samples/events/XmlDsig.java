/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.samples.events;

import by.bcrypto.bee2j.Bee2Library;
import by.bcrypto.bee2j.constants.XmlIdConstants;
import by.bcrypto.bee2j.provider.Bee2SecurityProvider;
import by.bcrypto.bee2j.provider.BrngSecureRandom;
import by.bcrypto.bee2j.provider.pki.BignCertificate;

import java.security.InvalidAlgorithmParameterException;
import java.security.Key;
import java.security.KeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Security;
import java.security.Provider;
import java.security.cert.X509Certificate;

import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import java.util.Collections;
import java.util.Iterator;

import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.KeySelectorException;
import javax.xml.crypto.KeySelectorResult;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import javax.xml.crypto.dsig.keyinfo.X509Data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.apache.jcp.xml.dsig.internal.dom.XMLDSigRI;

public final class XmlDsig {

    public XmlDsig() {
        Provider dsigProvider = new XMLDSigRI();
        Security.insertProviderAt(dsigProvider, 1);
        Bee2SecurityProvider bee2j = new Bee2SecurityProvider();
        Security.addProvider(bee2j);
    }

    /**
     * KeySelector which retrieves the public key out of the
     * KeyValue element and returns it.
     * NOTE: If the key algorithm doesn't match signature algorithm,
     * then the public key will be ignored.
     */
    private static class KeyValueKeySelector extends KeySelector {
        public KeySelectorResult select(final KeyInfo keyInfo,
                                        final KeySelector.Purpose purpose,
                                        final AlgorithmMethod method,
                                        final XMLCryptoContext context)
            throws KeySelectorException {
            if (keyInfo == null) {
                throw new KeySelectorException("Null KeyInfo object!");
            }
            SignatureMethod sm = (SignatureMethod) method;
            List<XMLStructure> list = keyInfo.getContent();

            for (int i = 0; i < list.size(); i++) {
                XMLStructure xmlStructure = list.get(i);
                if (xmlStructure instanceof KeyValue) {
                    PublicKey pk = null;
                    try {
                        pk = ((KeyValue) xmlStructure).getPublicKey();
                    } catch (KeyException ke) {
                        throw new KeySelectorException(ke);
                    }
                    //System.out.println(sm.getAlgorithm());
                    //System.out.println(pk.getAlgorithm());
                    // make sure algorithm is compatible with method
                    if (algEquals(sm.getAlgorithm(), pk.getAlgorithm())) {
                        return new SimpleKeySelectorResult(pk);
                    }
                }
                if (xmlStructure instanceof X509Data) {
                    PublicKey pk = null;
                    X509Data x509Data = (X509Data) xmlStructure;
                    List<?> x509DataList = x509Data.getContent();
                    for (Object x509DataObject : x509DataList) {
                        if (x509DataObject instanceof X509Certificate) {
                            BignCertificate bc = new BignCertificate((X509Certificate) x509DataObject);
                            pk = bc.getPublicKey();
                            break;
                        }
                    }
                    // make sure algorithm is compatible with method
                    if (algEquals(sm.getAlgorithm(), pk.getAlgorithm())) {
                        return new SimpleKeySelectorResult(pk);
                    }
                }
            }
            throw new KeySelectorException("No KeyValue element found!");
        }

        static boolean algEquals(final String algURI, final String algName) {
            if (algName.equalsIgnoreCase("DSA")
                && algURI.equalsIgnoreCase("http://www.w3.org/2009/xmldsig11#dsa-sha256")) {
                return true;
            } else if (algName.equalsIgnoreCase("RSA")
                && algURI.equalsIgnoreCase("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256")) {
                return true;
            } else if (algName.equalsIgnoreCase("Bign")
                && algURI.toLowerCase().startsWith("http://www.w3.org/2009/xmldsig11#bign")) {
                return true;
            }
            return false;
        }
    }

    private static class SimpleKeySelectorResult implements KeySelectorResult {
        private PublicKey pk;
        SimpleKeySelectorResult(final PublicKey pk) {
            this.pk = pk;
        }

        public Key getKey() {
            return pk;
        }
    }

    private String status;

    /**
     * @param statusValue the status to set
     */
    public void setStatus(final String statusValue) {
        this.status = statusValue;
    }

    /**
     * @return String return the status
     */
    public String getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());
    }


    public boolean verify(final String docString) throws XMLSignatureException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);

        InputStream targetStream = new ByteArrayInputStream(docString.getBytes());
        Document doc;
        try {
            doc = dbf.newDocumentBuilder().parse(targetStream);
        } catch (IOException e) {
            throw new XMLSignatureException(e);
        } catch (ParserConfigurationException e) {
            throw new XMLSignatureException(e);
        } catch (SAXException e) {
            throw new XMLSignatureException(e);
        }

        NodeList nl = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
        if (1 != nl.getLength()) {
            return false;
        }

        DOMValidateContext valContext = new DOMValidateContext(
            new KeyValueKeySelector(), nl.item(0));

        final NodeList elements = doc.getDocumentElement().getChildNodes();
        for (int i = 0; i < elements.getLength(); i++) {
            Node node = elements.item(i);
            if (node.getLocalName() != "Signature") {
                valContext.setIdAttributeNS((Element) node, null, "id");
            }
        }

        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

        XMLSignature signature;
        try {
            signature = fac.unmarshalXMLSignature(valContext);
        } catch (MarshalException e) {
            throw new XMLSignatureException(e);
        }
        valContext.setProperty("javax.xml.crypto.dsig.cacheReference", true);

        boolean coreValidity = signature.validate(valContext);
        boolean sv = true;
        // Check core validation status
        if (!coreValidity) {
            System.err.println("Signature failed core validation");
            sv = signature.getSignatureValue().validate(valContext);
            System.out.println("signature validation status: " + sv);
            // check the validation status of each Reference
            Iterator<Reference> i =
                signature.getSignedInfo().getReferences().iterator();
            for (int j = 0; i.hasNext(); j++) {
                boolean refValid = i.next().validate(valContext);
                System.out.println("ref[" + j + "] validity status: " + refValid);
            }
        } else {
            System.out.println("Signature passed core validation");
        }
        if (!coreValidity) {
            return false;
        }
        if (!sv) {
            return false;
        }
        return true;
    }

    public boolean verify(final String message, final String signature) throws XMLSignatureException {
        if (message == null || signature == null) {
            return false;
        }
        String doc = "<DELNOTE>" + message + signature + "</DELNOTE>";
        return verify(doc);
    }

    public boolean testXMLDsigSign() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, KeyException, SAXException, IOException, ParserConfigurationException, MarshalException, XMLSignatureException, TransformerException {


        // Create a DOM XMLSignatureFactory that will be used to generate the
        // enveloped signature
        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

        // Create a Reference to the enveloped document (in this case we are
        // signing the whole document, so a URI of "" signifies that) and
        // also specify the Belt digest algorithm and the ENVELOPED Transform.
        Reference ref = fac.newReference("",
            fac.newDigestMethod(XmlIdConstants.Belt, null),
            Collections.singletonList(fac.newTransform(
                Transform.ENVELOPED, (TransformParameterSpec) null)),
            null, null);
        // Create the SignedInfo
        SignedInfo si = fac.newSignedInfo(
            fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE,
                (C14NMethodParameterSpec) null),
            fac.newSignatureMethod(XmlIdConstants.BignWithBelt, null),
            Collections.singletonList(ref));

        KeyPairGenerator bignKeyPairGenerator = KeyPairGenerator.getInstance("Bign", "Bee2");
        BrngSecureRandom brngSecureRandom = new BrngSecureRandom();

        brngSecureRandom.setRng(new Bee2Library.TestBrngForPK());
        bignKeyPairGenerator.initialize(128, brngSecureRandom);
        KeyPair keyPair =  bignKeyPairGenerator.generateKeyPair();

        // Create a KeyValue containing the Bign PublicKey that was generated
        KeyInfoFactory kif = fac.getKeyInfoFactory();
        KeyValue kv = kif.newKeyValue(keyPair.getPublic());

        // Create a KeyInfo and add the KeyValue to it
        KeyInfo ki = kif.newKeyInfo(Collections.singletonList(kv));
        // Instantiate the document to be signed
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        String testDoc = "<doc><body>text</body></doc>";
        InputStream targetStream = new ByteArrayInputStream(testDoc.getBytes());
        Document doc = dbf.newDocumentBuilder().parse(targetStream);

        // Create a DOMSignContext and specify the DSA PrivateKey and
        // location of the resulting XMLSignature's parent element
        DOMSignContext dsc = new DOMSignContext(keyPair.getPrivate(),
            doc.getDocumentElement());
        dsc.setProperty("javax.xml.crypto.dsig.cacheReference", true);

        // Create the XMLSignature (but don't sign it yet)
        XMLSignature signature = fac.newXMLSignature(si, ki);

        // Marshal, generate (and sign) the enveloped signature
        signature.sign(dsc);

        // output the resulting document
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer trans = tf.newTransformer();
        trans.transform(new DOMSource(doc), new StreamResult(os));
        String result = os.toString();
        String[] parts = result.split("SignatureValue", 3);
        if (parts.length != 3) {
            return false;
        }
        String test0 = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>"
        + "<doc><body>text</body>"
        + "<Signature xmlns=\"http://www.w3.org/2000/09/xmldsig#\">"
        + "<SignedInfo><CanonicalizationMethod Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315\"/>"
        + "<SignatureMethod Algorithm=\"http://www.w3.org/2009/xmldsig11#bign-with-hbelt\"/>"
        + "<Reference URI=\"\"><Transforms>"
        + "<Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/></Transforms>"
        + "<DigestMethod Algorithm=\"http://www.w3.org/2009/xmldsig11#belt-hash256\"/>"
        + "<DigestValue>It6biT6uZrxUf15Nu1y2SjUKh1I/cjdVi3Z0vRe/bwo=</DigestValue></Reference></SignedInfo>"
        + "<";
        if (!test0.equals(parts[0])) {
            return false;
        }
        String test2 = ">"
        + "<KeyInfo><KeyValue><BignKeyValue xmlns=\"http://www.w3.org/2009/xmldsig11#\">"
        + "<DomainParameters><NamedCurve URN=\"http://www.w3.org/2009/xmldsig11#bign-curve256v1\"/></DomainParameters>"
        + "<PublicKey>vRpWUBedeeA/zuSdTCvV3fVM5G0M8R5P+Hv3qJCFf9B6xqYDYejIFzSRaG1GGygmGQwu2lkJBUqa&#13;\nuE0qudmakA==</PublicKey>"
        + "</BignKeyValue></KeyValue></KeyInfo></Signature></doc>";
        if (!test2.equals(parts[2])) {
            return false;
        }
        //assertEquals("", parts[1]);
        return true;
    }

    public boolean testXMLDsigVerify() throws NoSuchProviderException, SAXException, IOException, ParserConfigurationException, MarshalException, XMLSignatureException {

        Provider dsigProvider = new XMLDSigRI();
        Security.insertProviderAt(dsigProvider, 1);
        Bee2SecurityProvider bee2j = new Bee2SecurityProvider();
        Security.addProvider(bee2j);
        //System.setProperty("javax.xml.crypto.dsig.cacheReference", "true");

        String testDoc = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<doc><body>text</body>"
        + "<Signature xmlns=\"http://www.w3.org/2000/09/xmldsig#\">"
        + "<SignedInfo><CanonicalizationMethod Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315\"/>"
        + "<SignatureMethod Algorithm=\"http://www.w3.org/2009/xmldsig11#bign-with-hbelt\"/>"
        + "<Reference URI=\"\"><Transforms>"
        + "<Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/></Transforms>"
        + "<DigestMethod Algorithm=\"http://www.w3.org/2009/xmldsig11#belt-hash256\"/>"
        + "<DigestValue>It6biT6uZrxUf15Nu1y2SjUKh1I/cjdVi3Z0vRe/bwo=</DigestValue></Reference></SignedInfo>"
        + "<SignatureValue>Y7/H9mlTbn1uMUxBjVqunNQ/E4LUR0OJRBFasLDlYwE+N7QhXqADMecaUc3J9lkU</SignatureValue>"
        + "<KeyInfo><KeyValue><BignKeyValue xmlns=\"http://www.w3.org/2009/xmldsig11#\">"
        + "<DomainParameters><NamedCurve URN=\"http://www.w3.org/2009/xmldsig11#bign-curve256v1\"/></DomainParameters>"
        + "<PublicKey>vRpWUBedeeA/zuSdTCvV3fVM5G0M8R5P+Hv3qJCFf9B6xqYDYejIFzSRaG1GGygmGQwu2lkJBUqa&#13;\nuE0qudmakA==</PublicKey>"
        + "</BignKeyValue></KeyValue></KeyInfo></Signature></doc>";

        // Instantiate the document to be signed
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);

        InputStream targetStream = new ByteArrayInputStream(testDoc.getBytes());
        Document doc = dbf.newDocumentBuilder().parse(targetStream);

        NodeList nl = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
        if (1 != nl.getLength()) {
            return false;
        }

        // Create a DOMValidateContext and specify a KeyValue KeySelector
        // and document context
        DOMValidateContext valContext = new DOMValidateContext(
            new KeyValueKeySelector(), nl.item(0));

        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

        XMLSignature signature = fac.unmarshalXMLSignature(valContext);
        valContext.setProperty("javax.xml.crypto.dsig.cacheReference", true);

        boolean coreValidity = signature.validate(valContext);
        boolean sv = true;
        // Check core validation status
        if (!coreValidity) {
            System.err.println("Signature failed core validation");
            sv = signature.getSignatureValue().validate(valContext);
            System.out.println("signature validation status: " + sv);
            // check the validation status of each Reference
            Iterator<Reference> i =
                signature.getSignedInfo().getReferences().iterator();
            for (int j = 0; i.hasNext(); j++) {
                boolean refValid = i.next().validate(valContext);
                System.out.println("ref[" + j + "] validity status: " + refValid);
            }
        } else {
            System.out.println("Signature passed core validation");
        }
        if (!coreValidity) {
            return false;
        }
        if (!sv) {
            return false;
        }
        return true;
    }
}
