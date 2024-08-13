import java.io.InputStream;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.InvalidAlgorithmParameterException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.apache.jcp.xml.dsig.internal.dom.XMLDSigRI;

import by.bcrypto.bee2j.constants.XmlIdConstants;
import by.bcrypto.bee2j.provider.Bee2SecurityProvider;
import by.bcrypto.bee2j.provider.BignPrivateKeySpec;

public final class XmlSigner {

    private PrivateKey privateKey;
	private X509Certificate certificate;

    public XmlSigner() {
        Provider dsigProvider = new XMLDSigRI();
        Security.insertProviderAt(dsigProvider, 1);
        Bee2SecurityProvider bee2j = new Bee2SecurityProvider();
        Security.addProvider(bee2j);
    }

	public void loadPrivateKey(String path, String password) throws IOException {
        InputStream fs = Files.newInputStream(Paths.get(path));
        try{
            byte[] privateKeyContainer = fs.readAllBytes();
            BignPrivateKeySpec bignPrivateKeySpec = new BignPrivateKeySpec(privateKeyContainer, password);
            KeyFactory bignKeyFactory = KeyFactory.getInstance("Bign", "Bee2");

            this.privateKey = bignKeyFactory.generatePrivate(bignPrivateKeySpec);
        } catch (NoSuchAlgorithmException | NoSuchProviderException | 
            InvalidKeySpecException | NoSuchFileException e) {
            throw new IOException(e);
        } finally {
            fs.close();
        }
	}
	
	public void loadCertificate(String path) throws IOException {
        InputStream fs = Files.newInputStream(Paths.get(path));
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509", "Bee2");
            this.certificate = (X509Certificate) certificateFactory.generateCertificate(fs);
        } catch (CertificateException | NoSuchProviderException e) {
            throw new IOException(e);
        } finally {
            fs.close();
        }
	}

	public String signDocument(final Document doc, final String reference, int level) throws XMLSignatureException {
        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
        SignedInfo si;
        try { 
            Reference ref = fac.newReference(reference, 
			    fac.newDigestMethod(XmlIdConstants.Belt, null), null, null, null);
            si = fac.newSignedInfo(
                fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE,
                    (C14NMethodParameterSpec) null),
                fac.newSignatureMethod(XmlIdConstants.BignWithBelt, null),
                Collections.singletonList(ref));
        } catch (NoSuchAlgorithmException e) {
            throw new XMLSignatureException(e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new XMLSignatureException(e);
        }
        

        KeyInfoFactory kif = fac.getKeyInfoFactory();
		X509Data newX509Data = kif.newX509Data(
            Collections.singletonList(this.certificate));
		KeyInfo ki = kif.newKeyInfo(Collections.singletonList(newX509Data));

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document newDoc;
        try {
		    newDoc = dbf.newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            throw new XMLSignatureException(e);
        }

        DOMSignContext dsc = new DOMSignContext(this.privateKey, newDoc);
        dsc.setProperty("javax.xml.crypto.dsig.cacheReference", true);
        switch (level) {
            case 1:
                dsc.setIdAttributeNS(doc.getDocumentElement(), null, "id");
                break;
            case 2:
                NodeList children = doc.getDocumentElement().getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    Node node = children.item(i);
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element elem = (Element) node;
                        if (!elem.getAttribute("id").isEmpty()) {
                            String idValue = elem.getAttributeNS(null, "id");
                            System.out.println("idValue : " + idValue);
                            dsc.setIdAttributeNS(elem, null, "id");
                        }
                    }
                }
                break;       
            default:
                return null;
        }
        XMLSignature signature = fac.newXMLSignature(si, ki);
        try {
            signature.sign(dsc);
        } catch (MarshalException e) {
            throw new XMLSignatureException(e);
        }
		return XmlUtils.getStringFromDocument(newDoc);
	}
}
