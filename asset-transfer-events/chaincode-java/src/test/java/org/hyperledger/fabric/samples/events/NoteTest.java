// ./gradlew clean test --info

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.hyperledger.fabric.samples.events.Note;

import java.io.StringWriter;
import java.io.StringReader;
import javax.xml.transform.Transformer;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

public final class NoteTest {

    //method to convert Document to String
    public String getStringFromDocument(final Document doc) {
        try {
            DOMSource domSource = new DOMSource(doc);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
            transformer.transform(domSource, result);
            return writer.toString();
        } catch (TransformerException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private String nodeToString(final Node node) {
        StringWriter sw = new StringWriter();
        try {
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            t.setOutputProperty(OutputKeys.INDENT, "no");
            t.transform(new DOMSource(node), new StreamResult(sw));
        } catch (TransformerException te) {
            System.out.println("nodeToString Transformer Exception");
        }
        return sw.toString();
    }

    private Document loadXML(final File file) throws FileNotFoundException {
        Document doc = null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = dbf.newDocumentBuilder();
            doc = builder.parse(new FileInputStream(file));
        } catch (ParserConfigurationException e2) {
            e2.printStackTrace();
        } catch (SAXException e3) {
            e3.printStackTrace();
        } catch (IOException e4) {
            e4.printStackTrace();
        }
        return doc;
    }

    private String loadXMLNode(final Document doc, final String xpath) {
        Node node = null;
        try {
            XPath xPath = XPathFactory.newInstance().newXPath();
            String expression = "/DESADV/SG10";
            node = (Node) xPath.compile(expression).evaluate(doc, XPathConstants.NODE);
        } catch (XPathExpressionException e5) {
            e5.printStackTrace();
        }
        return nodeToString(node);
    }

    public static Document loadXMLFromString(final String xml) {
        Document doc = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(xml));
            doc = builder.parse(is);
        } catch (ParserConfigurationException e2) {
            e2.printStackTrace();
        } catch (SAXException e3) {
            e3.printStackTrace();
        } catch (IOException e4) {
            e4.printStackTrace();
        }
        return doc;
    }

    private void printItems(final String items) {
        try {
            Document doc = loadXMLFromString(items);
            XPath xpath = XPathFactory.newInstance().newXPath();
            NodeList nodes = (NodeList) xpath.compile("SG10/SG17").evaluate(doc, XPathConstants.NODESET);
            System.out.printf("Quantity \tGTIN       \tNAME\n");
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                String gtin  = xpath.compile("LIN/C212/E7140").evaluate(node).trim();
                String qty  = xpath.compile("QTY/C186[child::E6411/text()!='OF']/E6060").evaluate(node).trim();
                String name  = xpath.compile("IMD/C273/E7008").evaluate(node).trim();
                System.out.printf("\t%s \t%s \t%s\n", qty, gtin, name);
            }
        } catch (XPathExpressionException e5) {
            e5.printStackTrace();
        }
    }

    @Test
    public void test1() {
        Note dn = new Note("01", "A", "B");
        assertEquals(dn.getID(), "01");
        assertEquals(dn.getShipper(), "A");
        assertEquals(dn.getReciever(), "B");
    }

    @Test
    public void test2() {
        Note dn = new Note("01", "A", "B");
        ClassLoader classLoader = getClass().getClassLoader();
        try {
            File file = new File(classLoader.getResource("desadv.xml").getFile());
            Document doc = loadXML(file);
            String expression = "/DESADV/SG10";
            String str = getStringFromDocument(doc);
            String goods = loadXMLNode(doc, expression);
            assertNotNull(goods);
            dn.setAsset(goods);
            dn.addAdvice(str);
            System.out.println(dn.serialize("{}"));
            Note nt = Note.deserialize("{\"Shipper\":\"10\",\"Advices\":[],\"ID\":\"asset1702977878704\",\"Reciever\":\"100\"}");
            System.out.println(nt.toString());
            printItems(goods);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }
    }
}
