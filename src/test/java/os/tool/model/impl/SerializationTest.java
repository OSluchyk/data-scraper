package os.tool.model.impl;

import org.junit.jupiter.api.Test;
import os.tool.model.Document;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify that all Document implementations are properly serializable.
 * This is critical for distributed processing frameworks like Apache Spark and Apache Beam.
 */
class SerializationTest {

    @Test
    void testHtmlDocumentSerialization() throws IOException, ClassNotFoundException {
        String html = "<html><head><title>Test</title></head><body><div class='item'>Item 1</div><div class='item'>Item 2</div></body></html>";
        HtmlDocument original = new HtmlDocument(html);

        // Access document to trigger parsing
        String title = original.textNode("title");
        assertEquals("Test", title);

        // Serialize
        byte[] serialized = serialize(original);

        // Deserialize
        HtmlDocument deserialized = (HtmlDocument) deserialize(serialized);

        // Verify functionality after deserialization
        assertEquals("Test", deserialized.textNode("title"));
        assertEquals(2, deserialized.nodeList("div.item").size());

        // Verify memory efficiency - serialized size should be reasonable
        // (includes Java serialization overhead but not the parsed JSoup Document)
        assertTrue(serialized.length < html.length() * 5,
                "Serialized size should not include parsed JSoup Document");
    }

    @Test
    void testJsonDocumentSerialization() throws IOException, ClassNotFoundException {
        String json = "{\"name\":\"John\",\"city\":\"New York\",\"items\":[{\"id\":1},{\"id\":2}]}";
        JsonDocument original = new JsonDocument(json);

        // Access document to trigger parsing
        String name = original.textNode("$.name");
        assertEquals("John", name);

        // Serialize
        byte[] serialized = serialize(original);

        // Deserialize
        JsonDocument deserialized = (JsonDocument) deserialize(serialized);

        // Verify functionality after deserialization
        assertEquals("John", deserialized.textNode("$.name"));
        assertEquals("New York", deserialized.textNode("$.city"));

        // Verify memory efficiency - serialized size should be reasonable
        // (includes Java serialization overhead but not the parsed DocumentContext)
        assertTrue(serialized.length < json.length() * 5,
                "Serialized size should not include parsed DocumentContext");
    }

    @Test
    void testXmlDocumentSerialization() throws IOException, ClassNotFoundException {
        String xml = "<?xml version=\"1.0\"?><root><item id=\"1\">Value 1</item><item id=\"2\">Value 2</item></root>";
        XmlDocument original = new XmlDocument(xml);

        // Access document to trigger parsing
        String value = original.textNode("//item[@id='1']/text()");
        assertEquals("Value 1", value);

        // Serialize
        byte[] serialized = serialize(original);

        // Deserialize
        XmlDocument deserialized = (XmlDocument) deserialize(serialized);

        // Verify functionality after deserialization
        assertEquals("Value 1", deserialized.textNode("//item[@id='1']/text()"));
        assertEquals(2, deserialized.nodeList("//item").size());

        // Verify memory efficiency - serialized size should be reasonable
        // (includes Java serialization overhead but not the parsed DOM Document)
        assertTrue(serialized.length < xml.length() * 5,
                "Serialized size should not include parsed DOM Document");
    }

    @Test
    void testLazyParsingBehavior() {
        String html = "<html><body><h1>Title</h1></body></html>";
        HtmlDocument doc = new HtmlDocument(html);

        // Document created instantly without parsing
        assertNotNull(doc);

        // Parsing happens on first access
        String title = doc.textNode("h1");
        assertEquals("Title", title);

        // Subsequent access uses cached parsed document
        String title2 = doc.textNode("h1");
        assertEquals("Title", title2);
    }

    @Test
    void testNodeStreamMethod() {
        String html = "<html><body>" +
                "<div class='item'>Item 1</div>" +
                "<div class='item'>Item 2</div>" +
                "<div class='item'>Item 3</div>" +
                "</body></html>";
        HtmlDocument doc = new HtmlDocument(html);

        // Test streaming API
        long count = doc.nodeStream("div.item").count();
        assertEquals(3, count);

        // Test streaming with filters
        long limited = doc.nodeStream("div.item")
                .limit(2)
                .count();
        assertEquals(2, limited);
    }

    @Test
    void testSerializedDocumentReusability() throws IOException, ClassNotFoundException {
        String html = "<html><body><p>Test</p></body></html>";
        HtmlDocument original = new HtmlDocument(html);

        // Serialize without accessing (lazy)
        byte[] serialized1 = serialize(original);

        // Access to trigger parsing
        original.textNode("p");

        // Serialize after parsing
        byte[] serialized2 = serialize(original);

        // Both should work the same after deserialization
        HtmlDocument doc1 = (HtmlDocument) deserialize(serialized1);
        HtmlDocument doc2 = (HtmlDocument) deserialize(serialized2);

        assertEquals("Test", doc1.textNode("p"));
        assertEquals("Test", doc2.textNode("p"));

        // Sizes should be similar (transient field not serialized)
        assertEquals(serialized1.length, serialized2.length, 50);
    }

    // Helper methods

    private byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
        }
        return baos.toByteArray();
    }

    private Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        try (ObjectInputStream ois = new ObjectInputStream(bais)) {
            return ois.readObject();
        }
    }
}
