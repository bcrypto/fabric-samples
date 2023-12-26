package application.java;

import java.io.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;


public class Input {

    private static final String IdAttributeKey = "id";
    private InputStream resource;
    private Node doc;

    public Input(InputStream resource) {
        this.resource = resource;
    }

    public Node getDoc() {
        if (doc != null) {
            return doc;
        }
        var dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        try {
            doc = dbf.newDocumentBuilder().parse(resource);
            markIdAttribute(doc);

            return doc;
        } catch (SAXException | IOException | ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public void print(OutputStream os) {
        var tf = TransformerFactory.newInstance();
        try {
            var trans = tf.newTransformer();
            trans.transform(new DOMSource(getDoc()), new StreamResult(os));
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
    }

    public static void markIdAttribute(Node node) {
        var attrs = node.getAttributes();
        if (attrs != null && attrs.getLength() != 0)
        {
            var attr = (Attr) attrs.item(0);
            if (attr != null && attr.getName().equals(IdAttributeKey))
            {
                attr.getOwnerElement().setIdAttribute(IdAttributeKey, true);
            }
        }

        var nodeList = node.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node currentNode = nodeList.item(i);
            if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                markIdAttribute(currentNode);
            }
        }
    }
}
