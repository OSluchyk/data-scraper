package os.tool.model.impl;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import os.tool.model.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
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
import java.util.stream.Stream;

public class XmlDocument implements Document {
    private static final long serialVersionUID = 1L;
    private static final TransformerFactory TRANSFORMER_FACTORY = TransformerFactory.newInstance();
    private static final ThreadLocal<Transformer> TRANSFORMER_CACHE =
            ThreadLocal.withInitial(() -> {
                try {
                    return TRANSFORMER_FACTORY.newTransformer();
                } catch (TransformerConfigurationException e) {
                    throw new RuntimeException("Failed to create Transformer", e);
                }
            });

    private final String xml;
    private transient org.w3c.dom.Document document;
    private transient XPathFactory xFactory;

    public XmlDocument(String xml) {
        this.xml = xml;
        // Lazy parsing - don't parse until needed
    }

    private org.w3c.dom.Document getDocument() {
        if (document == null) {
            try {
                DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                document = db.parse(new ByteArrayInputStream(xml.getBytes()));
            } catch (ParserConfigurationException | SAXException | IOException e) {
                throw new RuntimeException("Failed to parse XML", e);
            }
        }
        return document;
    }

    private XPathFactory getXPathFactory() {
        if (xFactory == null) {
            xFactory = XPathFactory.newInstance();
        }
        return xFactory;
    }


    @Override
    public String textNode(String expr) {
        try {
            return (String) getXPathFactory().newXPath().compile(expr)
                    .evaluate(getDocument(), XPathConstants.STRING);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<CharSequence> textNodeList(String expr) {
        List<CharSequence> list = new ArrayList<>();
        try {
            NodeList nodes = (NodeList) getXPathFactory().newXPath().compile(expr)
                    .evaluate(getDocument(), XPathConstants.NODESET);
            for (int i = 0; i < nodes.getLength(); i++)
                list.add(nodes.item(i).getNodeValue());
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    @Override
    public Document node(String expr) {
        Document result;
        try {
            Node xnode = (Node) getXPathFactory().newXPath().compile(expr)
                    .evaluate(getDocument(), XPathConstants.NODE);
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
            NodeList nodes = (NodeList) getXPathFactory().newXPath().compile(expr)
                    .evaluate(getDocument(), XPathConstants.NODESET);
            for (int i = 0; i < nodes.getLength(); i++) {
                Node item = nodes.item(i);
                docs.add(new XmlDocument(toString(item)));
            }
        } catch (XPathExpressionException | TransformerException e) {
            throw new RuntimeException(e);
        }
        return docs;
    }

    @Override
    public Stream<Document> nodeStream(String expr) {
        try {
            NodeList nodes = (NodeList) getXPathFactory().newXPath().compile(expr)
                    .evaluate(getDocument(), XPathConstants.NODESET);
            Stream.Builder<Document> builder = Stream.builder();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node item = nodes.item(i);
                builder.add(new XmlDocument(toString(item)));
            }
            return builder.build();
        } catch (XPathExpressionException | TransformerException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isEmpty() {
        return xml.isEmpty();
    }

    private String toString(Node node) throws TransformerException {
        StringWriter writer = new StringWriter();
        TRANSFORMER_CACHE.get().transform(new DOMSource(node), new StreamResult(writer));
        return writer.toString();
    }

    @Override
    public String toString() {
        return xml;
    }
}
