package os.tool.model.impl;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import os.tool.model.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class XmlDocument implements Document {
    private final String xml;
    private final org.w3c.dom.Document document;
    private final XPathFactory xFactory = XPathFactory.newInstance();

    public XmlDocument(String xml) {
        this.xml = xml;

        try {
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            document = db.parse(new ByteArrayInputStream(xml.getBytes()));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RuntimeException("Failed to initialize XmlDocument", e);
        }

    }


    @Override
    public String textNode(String expr) {
        try {
            return (String) xFactory.newXPath().compile(expr).
                    evaluate(document, XPathConstants.STRING);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<CharSequence> textNodeList(String expr) {
        List<CharSequence> list = new ArrayList<>();
        try {
            NodeList nodes = (NodeList) xFactory.newXPath().compile(expr).evaluate(document, XPathConstants.NODESET);
            for (int i = 0; i < nodes.getLength(); i++)
                list.add(nodes.item(i).getNodeValue());
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    @Override
    public Document node(String expr) {
        Document result = null;
        try {
            Node xnode = (Node) xFactory.newXPath().compile(expr).evaluate(document, XPathConstants.NODE);

            result = new XmlDocument(toString(xnode));
        } catch (XPathExpressionException | TransformerException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    @Override
    public List<Document> nodeList(String expr) {
        List<Document> docs = new LinkedList<>();
        try {
            NodeList nodes = (NodeList) xFactory.newXPath().compile(expr).
                    evaluate(document, XPathConstants.NODESET);
            for (int i = 0; i < nodes.getLength(); i++) {
                Node item = nodes.item(i);
                docs.add(new XmlDocument(toString(item)));
            }
        } catch (XPathExpressionException | TransformerException e) {
            throw new RuntimeException(e);
        }
        return docs;
    }

    private String toString(Node node) throws TransformerException {
        StringWriter writer = new StringWriter();
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.transform(new DOMSource(node), new StreamResult(writer));
        return writer.toString();
    }

    @Override
    public String toString() {
        return xml;
    }
}
