package os.tool.model.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import os.tool.model.Document;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Stream;

class XmlDocumentTest {

    @ParameterizedTest
    @MethodSource("documentProvider")
    void textNode(Document doc) {
        Assertions.assertEquals("Corets, Eva", doc.textNode("/catalog/book[@id='bk103']/author"));
        Assertions.assertEquals("Corets, Eva", doc.textNode("//book[@id='bk103']/author"));

    }

    @ParameterizedTest
    @MethodSource("documentProvider")
    void textNodeList(Document doc) {
        List<CharSequence> nodes = doc.textNodeList("/catalog/book/author/text()");
        Assertions.assertEquals(12, nodes.size());
        Assertions.assertEquals("Gambardella, Matthew", nodes.get(0));
    }

    @ParameterizedTest
    @MethodSource("documentProvider")
    void node(Document doc) {
        Document node = doc.node("/catalog/book[@id='bk103']");
        Assertions.assertNotNull(node);
    }

    @ParameterizedTest
    @MethodSource("documentProvider")
    void nodeList(Document doc) {
        List<Document> nodes = doc.nodeList("/catalog/book");
        Assertions.assertEquals(12, nodes.size());
        Assertions.assertEquals("Gambardella, Matthew", nodes.get(0).textNode("//author/text()"));
    }

    static Stream<Document> documentProvider() throws URISyntaxException, IOException {
        String text = TestHelper.loadResourceAsString("/test.xml");
        return Stream.of(new XmlDocument(text));
    }
}