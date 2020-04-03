package os.tool.model.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import os.tool.model.Document;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Stream;

import static os.tool.model.impl.TestHelper.loadResourceAsString;

class JsonDocumentTest extends TestHelper {

    @ParameterizedTest
    @MethodSource("documentProvider")
    void textNode(Document doc) {
        String value = doc.textNode("$.store.book[0].author");
        Assertions.assertEquals("Nigel Rees", value);
    }

    @ParameterizedTest
    @MethodSource("documentProvider")
    void textNodeList(Document doc) {
        List<CharSequence> nodeList = doc.textNodeList("$.store.book[*].author");
        Assertions.assertEquals(4, nodeList.size());
        Assertions.assertEquals("Nigel Rees", nodeList.get(0));


    }

    @ParameterizedTest
    @MethodSource("documentProvider")
    void node(Document doc) {
        Document res = doc.node("$.store.book[0]");
        Assertions.assertNotNull(res);
        Assertions.assertEquals("reference", res.textNode("$.category"));
        Assertions.assertEquals("Nigel Rees", res.textNode("$.author"));
    }

    @ParameterizedTest
    @MethodSource("documentProvider")
    void nodeList(Document doc) {
        List<Document> res = doc.nodeList("$..book[-2:]");
        Assertions.assertNotNull(res);
        Assertions.assertEquals(2, res.size());
        Assertions.assertEquals("fiction", res.get(0).textNode("$.category"));

    }

    static Stream<Document> documentProvider() throws URISyntaxException, IOException {
        String text = loadResourceAsString("/test.json");
        return Stream.of(new JsonDocument(text));
    }

}