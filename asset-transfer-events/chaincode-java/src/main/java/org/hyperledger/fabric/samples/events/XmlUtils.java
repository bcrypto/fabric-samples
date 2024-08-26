package org.hyperledger.fabric.samples.events;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public final class XmlUtils {

    private XmlUtils() { }

    public static Document loadXML(final String str) throws IOException {
        Document doc = null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = dbf.newDocumentBuilder();
            doc = builder.parse(new InputSource(new StringReader(str)));
        } catch (ParserConfigurationException e) {
            throw new IOException(e);
        } catch (SAXException e) {
            throw new IOException(e);
        }
        return doc;
    }

    public static String getMessageId(final Document doc) {
        String id = null;
        Node root = doc.getDocumentElement();
        id = root.getAttributes().getNamedItem("id").getNodeValue();
        return id;
    }

    public static String getMessageId(final String message) throws IOException {
        Document doc = loadXML(message);
        return getMessageId(doc);
    }

    public static String getDsigReference(final Document doc) {
        return XmlUtils.loadXMLString(doc, "/Signature/SignedInfo/Reference/@URI")
        .replace("#", "");
    }

    public static String getDsigReference(final String signature) throws IOException {
        Document doc = XmlUtils.loadXML(signature);
        return getDsigReference(doc);
    }

    public static boolean isWaitingMode(final Document doc) {
        Node qvr = XmlUtils.loadXMLNode(doc, "//QVR/E4221[text()=\"PN\" or text()=\"PS\"]");
        return (qvr != null);
    }

    public static boolean hasQvr(final Document doc) {
        Node qvr = XmlUtils.loadXMLNode(doc, "//QVR");
        return (qvr != null);
    }

    public static boolean hasDiff(final Document doc) {
        Node qvr = XmlUtils.loadXMLNode(doc, "//QVR/E4221[text()!=\"PN\" and text()!=\"PS\"]");
        return (qvr != null);
    }


    public static Node loadXMLNode(final Document doc, final String xpath) {
        Node node = null;
        try {
            XPath xPath = XPathFactory.newInstance().newXPath();
            node = (Node) xPath.compile(xpath).evaluate(doc, XPathConstants.NODE);
        } catch (XPathExpressionException e5) {
            e5.printStackTrace();
        }
        return node;
    }

    public static String loadXMLString(final Document doc, final String xpath) {
        String res = null;
        try {
            XPath xPath = XPathFactory.newInstance().newXPath();
            res = (String) xPath.compile(xpath).evaluate(doc, XPathConstants.STRING);
        } catch (XPathExpressionException e5) {
            e5.printStackTrace();
        }
        return res;
    }
}