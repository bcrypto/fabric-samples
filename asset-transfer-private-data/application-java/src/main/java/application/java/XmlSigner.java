package application.java;

import java.security.*;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;

public class XmlSigner {

    private final Input input;
    private final PrivateKey privateKey;
    private final X509Certificate  cert;

    public XmlSigner(Input input, PrivateKey privateKey, X509Certificate cert) {
        this.input = input;
        this.privateKey = privateKey;
        this.cert = cert;
    }

    public void generateXMLSignature(String uri) {
        try {
            var xmlSigFac = XMLSignatureFactory.getInstance("DOM");
            var digestMethod = xmlSigFac.newDigestMethod(DigestMethod.SHA256, null);
            var transform = xmlSigFac.newTransform(Transform.ENVELOPED,
                    (TransformParameterSpec) null);
            var ref = xmlSigFac.newReference(uri, digestMethod,
                    Collections.singletonList(transform), null, null);
            var canonMethod = xmlSigFac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE,
                    (C14NMethodParameterSpec) null);
            var sigMethod = xmlSigFac.newSignatureMethod("http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha256", null);
            var signedInfo = xmlSigFac.newSignedInfo(canonMethod, sigMethod, Collections.singletonList(ref));
            var kif = xmlSigFac.getKeyInfoFactory();
            var x509Content = new ArrayList();
            x509Content.add(cert.getSubjectX500Principal().getName());
            x509Content.add(cert);
            var xd = kif.newX509Data(x509Content);
            var keyInfo = kif.newKeyInfo(Collections.singletonList(xd));
            var signature = xmlSigFac.newXMLSignature(signedInfo, keyInfo);
            signature.sign(new DOMSignContext(privateKey, input.getDoc().getFirstChild()));
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | MarshalException | XMLSignatureException e) {
            throw new RuntimeException(e);
        }
    }

}