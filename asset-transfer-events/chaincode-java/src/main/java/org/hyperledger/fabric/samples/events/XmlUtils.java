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

    public static String getMessageId(final String message) {
        String id = null;
        Document doc;
        try {
            doc = loadXML(message);
            id = getMessageId(doc);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return id;
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
