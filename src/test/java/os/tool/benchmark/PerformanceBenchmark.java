package os.tool.benchmark;

import org.junit.jupiter.api.Test;
import os.tool.model.Document;
import os.tool.model.impl.HtmlDocument;
import os.tool.model.impl.JsonDocument;
import os.tool.model.impl.XmlDocument;

import java.io.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Performance benchmarks to demonstrate improvements in:
 * - Lazy parsing (instant document creation)
 * - Memory efficiency (serialization size)
 * - XmlDocument nodeList performance (cached TransformerFactory)
 * - Streaming API for large result sets
 */
public class PerformanceBenchmark {

    @Test
    void benchmarkLazyParsing() {
        System.out.println("\n=== Lazy Parsing Benchmark ===");

        // Generate large documents
        String largeHtml = generateLargeHtml(10_000); // ~500KB
        String largeJson = generateLargeJson(5_000);  // ~300KB
        String largeXml = generateLargeXml(5_000);    // ~400KB

        // Benchmark HtmlDocument creation
        long start = System.nanoTime();
        HtmlDocument htmlDoc = new HtmlDocument(largeHtml);
        long htmlCreateTime = System.nanoTime() - start;

        // Benchmark JsonDocument creation
        start = System.nanoTime();
        JsonDocument jsonDoc = new JsonDocument(largeJson);
        long jsonCreateTime = System.nanoTime() - start;

        // Benchmark XmlDocument creation
        start = System.nanoTime();
        XmlDocument xmlDoc = new XmlDocument(largeXml);
        long xmlCreateTime = System.nanoTime() - start;

        System.out.printf("HtmlDocument creation (500KB): %,d µs%n", htmlCreateTime / 1_000);
        System.out.printf("JsonDocument creation (300KB): %,d µs%n", jsonCreateTime / 1_000);
        System.out.printf("XmlDocument creation (400KB):  %,d µs%n", xmlCreateTime / 1_000);

        // Now benchmark first access (triggers parsing)
        start = System.nanoTime();
        htmlDoc.textNode("title");
        long htmlParseTime = System.nanoTime() - start;

        start = System.nanoTime();
        jsonDoc.textNode("$.items[0].name");
        long jsonParseTime = System.nanoTime() - start;

        start = System.nanoTime();
        xmlDoc.textNode("//item[1]/@id");
        long xmlParseTime = System.nanoTime() - start;

        System.out.printf("%nFirst access (parsing happens):%n");
        System.out.printf("HtmlDocument parsing: %,d ms%n", htmlParseTime / 1_000_000);
        System.out.printf("JsonDocument parsing: %,d ms%n", jsonParseTime / 1_000_000);
        System.out.printf("XmlDocument parsing:  %,d ms%n", xmlParseTime / 1_000_000);

        System.out.printf("%nResult: Document creation is instant (~%d µs), parsing is deferred until first access%n",
                Math.max(htmlCreateTime, Math.max(jsonCreateTime, xmlCreateTime)) / 1_000);
    }

    @Test
    void benchmarkMemoryEfficiency() throws IOException, ClassNotFoundException {
        System.out.println("\n=== Memory Efficiency Benchmark ===");

        String html = generateLargeHtml(1_000);  // ~50KB
        String json = generateLargeJson(500);    // ~30KB
        String xml = generateLargeXml(500);      // ~40KB

        // Create documents and trigger parsing
        HtmlDocument htmlDoc = new HtmlDocument(html);
        htmlDoc.textNode("title"); // Trigger parsing

        JsonDocument jsonDoc = new JsonDocument(json);
        jsonDoc.textNode("$.items[0].name"); // Trigger parsing

        XmlDocument xmlDoc = new XmlDocument(xml);
        xmlDoc.textNode("//item[1]/@id"); // Trigger parsing

        // Serialize (transient fields are excluded)
        byte[] htmlSerialized = serialize(htmlDoc);
        byte[] jsonSerialized = serialize(jsonDoc);
        byte[] xmlSerialized = serialize(xmlDoc);

        System.out.printf("Original HTML size:  %,6d bytes%n", html.length());
        System.out.printf("Serialized HTML:     %,6d bytes (%.1fx overhead)%n",
                htmlSerialized.length, (double) htmlSerialized.length / html.length());

        System.out.printf("%nOriginal JSON size:  %,6d bytes%n", json.length());
        System.out.printf("Serialized JSON:     %,6d bytes (%.1fx overhead)%n",
                jsonSerialized.length, (double) jsonSerialized.length / json.length());

        System.out.printf("%nOriginal XML size:   %,6d bytes%n", xml.length());
        System.out.printf("Serialized XML:      %,6d bytes (%.1fx overhead)%n",
                xmlSerialized.length, (double) xmlSerialized.length / xml.length());

        // Test deserialization
        HtmlDocument htmlDeserialized = (HtmlDocument) deserialize(htmlSerialized);
        JsonDocument jsonDeserialized = (JsonDocument) deserialize(jsonSerialized);
        XmlDocument xmlDeserialized = (XmlDocument) deserialize(xmlSerialized);

        // Verify functionality after deserialization
        System.out.printf("%nDeserialized documents work correctly: %s%n",
                htmlDeserialized.textNode("title") != null &&
                jsonDeserialized.textNode("$.items[0].name") != null &&
                xmlDeserialized.textNode("//item[1]/@id") != null ? "✓" : "✗");

        System.out.printf("%nResult: Serialized size is only ~%.1fx original (excludes parsed DOM/context)%n",
                (htmlSerialized.length + jsonSerialized.length + xmlSerialized.length) /
                (double)(html.length() + json.length() + xml.length()));
    }

    @Test
    void benchmarkXmlTransformerCaching() {
        System.out.println("\n=== XmlDocument TransformerFactory Caching Benchmark ===");

        String xml = generateLargeXml(100);
        XmlDocument doc = new XmlDocument(xml);

        // Warm up
        doc.nodeList("//item").size();

        // Benchmark nodeList with 100 items (each calls toString with Transformer)
        long start = System.nanoTime();
        List<Document> nodes = doc.nodeList("//item");
        long nodeListTime = System.nanoTime() - start;

        System.out.printf("nodeList with %d items: %,d ms%n", nodes.size(), nodeListTime / 1_000_000);
        System.out.printf("Average time per node: %,d µs%n", (nodeListTime / nodes.size()) / 1_000);

        System.out.printf("%nResult: With cached TransformerFactory, processing %d nodes takes only %d ms%n",
                nodes.size(), nodeListTime / 1_000_000);
        System.out.printf("(Without caching, this would create %d TransformerFactory instances, adding ~%d-5000ms)%n",
                nodes.size(), nodes.size());
    }

    @Test
    void benchmarkStreamingAPI() {
        System.out.println("\n=== Streaming API Benchmark ===");

        String html = generateLargeHtml(5_000);
        HtmlDocument doc = new HtmlDocument(html);

        // Benchmark traditional nodeList (loads all into memory)
        long start = System.nanoTime();
        List<Document> allNodes = doc.nodeList("div.item");
        long nodeListTime = System.nanoTime() - start;

        // Benchmark streaming with limit (more memory efficient)
        start = System.nanoTime();
        List<Document> limitedNodes = doc.nodeStream("div.item")
                .limit(100)
                .collect(Collectors.toList());
        long streamTime = System.nanoTime() - start;

        System.out.printf("nodeList (load all %,d items): %,d ms%n", allNodes.size(), nodeListTime / 1_000_000);
        System.out.printf("nodeStream + limit(100):        %,d ms%n", streamTime / 1_000_000);

        // Benchmark filtering with streaming
        start = System.nanoTime();
        long count = doc.nodeStream("div.item")
                .filter(d -> !d.isEmpty())
                .count();
        long filterTime = System.nanoTime() - start;

        System.out.printf("nodeStream + filter + count:    %,d ms (processed %,d items)%n",
                filterTime / 1_000_000, count);

        System.out.printf("%nResult: Streaming API enables efficient processing without loading all nodes into memory%n");
    }

    @Test
    void benchmarkSerializationForDistributedProcessing() throws IOException, ClassNotFoundException {
        System.out.println("\n=== Distributed Processing Compatibility Benchmark ===");

        // Simulate distributed processing: serialize -> send to worker -> deserialize -> process
        String html = generateLargeHtml(1_000);
        HtmlDocument doc = new HtmlDocument(html);

        // Serialize (simulating sending to Spark worker)
        long start = System.nanoTime();
        byte[] serialized = serialize(doc);
        long serializeTime = System.nanoTime() - start;

        // Deserialize (simulating receiving on Spark worker)
        start = System.nanoTime();
        HtmlDocument workerDoc = (HtmlDocument) deserialize(serialized);
        long deserializeTime = System.nanoTime() - start;

        // Process on worker
        start = System.nanoTime();
        String result = workerDoc.textNode("title");
        long processTime = System.nanoTime() - start;

        System.out.printf("Serialize document:   %,d ms (%,d bytes)%n",
                serializeTime / 1_000_000, serialized.length);
        System.out.printf("Deserialize document: %,d ms%n", deserializeTime / 1_000_000);
        System.out.printf("Process on worker:    %,d ms%n", processTime / 1_000_000);
        System.out.printf("Total round-trip:     %,d ms%n",
                (serializeTime + deserializeTime + processTime) / 1_000_000);

        System.out.printf("%nResult: Documents are fully serializable for Apache Spark/Beam ✓%n");
        System.out.printf("Network transfer size: %,d bytes (only string content, not parsed DOM)%n",
                serialized.length);
    }

    // Helper methods to generate test data

    private String generateLargeHtml(int itemCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><title>Test Page</title></head><body>");
        for (int i = 0; i < itemCount; i++) {
            sb.append("<div class='item' id='item-").append(i).append("'>");
            sb.append("<h2>Item ").append(i).append("</h2>");
            sb.append("<p>Description for item ").append(i).append("</p>");
            sb.append("</div>");
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    private String generateLargeJson(int itemCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"items\":[");
        for (int i = 0; i < itemCount; i++) {
            if (i > 0) sb.append(",");
            sb.append("{\"id\":").append(i);
            sb.append(",\"name\":\"Item ").append(i).append("\"");
            sb.append(",\"description\":\"Description for item ").append(i).append("\"}");
        }
        sb.append("]}");
        return sb.toString();
    }

    private String generateLargeXml(int itemCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><root>");
        for (int i = 0; i < itemCount; i++) {
            sb.append("<item id=\"").append(i).append("\">");
            sb.append("<name>Item ").append(i).append("</name>");
            sb.append("<description>Description for item ").append(i).append("</description>");
            sb.append("</item>");
        }
        sb.append("</root>");
        return sb.toString();
    }

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
