package os.tool.model.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import os.tool.model.Document;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Stream;

class HtmlDocumentTest {

    @ParameterizedTest
    @MethodSource("documentProvider")
    void textNode(Document doc) {
        Assertions.assertNotNull(doc);
        Assertions.assertEquals("HTML - Wikipedia", doc.textNode("/html/head/title/text()"));
        Assertions.assertEquals("UTF-8", doc.textNode("/html/head/meta/@charset"));
        Assertions.assertEquals("HTML", doc.textNode("//*[@id=\"firstHeading\"]"));
    }


    @ParameterizedTest
    @MethodSource("documentProvider")
    void textNodeList(Document doc) {
        Assertions.assertNotNull(doc);
        List<CharSequence> nodes = doc.textNodeList("//*[@id=\"toc\"]/ul/li/a/span[@class=\"toctext\"]/text()");
        Assertions.assertEquals(11, nodes.size());
        Assertions.assertEquals("History", nodes.get(0));
    }

    @ParameterizedTest
    @MethodSource("documentProvider")
    void node(Document doc) {
        Assertions.assertNotNull(doc);
        Document node = doc.node("//*[@id=\"mw-content-text\"]/div/table[1]");
        Assertions.assertNotNull(node);
        Assertions.assertEquals("HTML(Hypertext Markup Language)", node.textNode("//caption"));

    }

    @ParameterizedTest
    @MethodSource("documentProvider")
    void nodeList(Document doc) {
        Assertions.assertNotNull(doc);
        List<Document> nodes = doc.nodeList("//*[@id=\"toc\"]/ul/li/a");
        Assertions.assertEquals(11, nodes.size());
        Assertions.assertEquals("History", nodes.get(0).textNode("//span[@class=\"toctext\"]/text()"));
    }

    static Stream<Document> documentProvider() throws URISyntaxException, IOException {
        String text = TestHelper.loadResourceAsString("/test.html");
        return Stream.of(new XmlDocument(text));
    }
}