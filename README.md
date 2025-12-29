# Scraping Tools to Extract Online Data

data-scraper is an ultra-fast, memory-efficient tool to scrape and process data from various sources (HTML, JSON, XML). Optimized for high-performance batch processing and distributed computing frameworks like Apache Spark and Apache Beam.

## Features

- **Multiple Format Support**: Parse HTML, JSON, and XML documents with a unified interface
- **Ultra-Fast**: Lazy parsing with instant document creation (~1μs)
- **Memory Efficient**: 75-85% memory reduction through transient field serialization
- **Distributed-Ready**: Full serialization support for Apache Spark/Beam
- **Streaming API**: Process large documents without loading everything into memory
- **High Performance**: Cached TransformerFactory for 100-500x faster XML operations

## Performance Benchmarks

Based on real performance tests (see `PerformanceBenchmark.java` and `Phase2And3Benchmark.java`):

```
=== Phase 1: Core Optimizations ===
Lazy Parsing:
  Document creation: <1 ms (instant, parsing deferred)
  First access:      19-47 ms (parsing happens on demand)

Memory Efficiency:
  Serialized size:   1.0x original (only ~0.1x Java overhead)
  Parsed DOM excluded from serialization ✓

XmlDocument Performance:
  Processing 100 nodes: 2 ms with cached TransformerFactory
  Without caching:      100-5000 ms (100-500x slower)

Streaming API:
  nodeList (5000 items):   80 ms
  nodeStream + limit(100):  2 ms (40x faster for partial results)

Distributed Processing:
  Serialize + transfer + deserialize + process: 13 ms
  Network overhead: ~1.0x original document size

=== Phase 2 & 3: Advanced Optimizations ===
Shared State for Sub-Documents:
  Creating 1000 sub-documents: 2 ms
  Sub-document access:         <1 µs (shares parsed parent)
  Deep navigation (3 levels):  28 ms total

LRU Caching for Repeated Queries:
  100 iterations without cache: 369 ms
  100 iterations with cache:     10 ms
  Speedup: 36x faster ✓

Parsing Strategies:
  LAZY   - Creation: <1 µs,  First access: 2 ms
  EAGER  - Creation: 1 ms,   First access: 185 µs
  CACHED - Creation: <1 µs,  Cached access: near-instant

JSON Sub-Document Sharing:
  Creating 1000 sub-documents: 23 ms
  Average access time:          9 µs per document
```

## Quick Start

### Basic Usage

```java
// HTML parsing (default LAZY strategy)
HtmlDocument htmlDoc = new HtmlDocument(htmlString);
String title = htmlDoc.textNode("title");
List<Document> items = htmlDoc.nodeList("div.product");

// JSON parsing with JSONPath
JsonDocument jsonDoc = new JsonDocument(jsonString);
String name = jsonDoc.textNode("$.user.name");
List<CharSequence> prices = jsonDoc.textNodeList("$.products[*].price");

// XML parsing with XPath
XmlDocument xmlDoc = new XmlDocument(xmlString);
String author = xmlDoc.textNode("//book[@id='123']/author");
List<Document> books = xmlDoc.nodeList("//book");
```

### Configurable Parsing Strategies

```java
import os.tool.model.config.DocumentConfig;

// LAZY (default) - Parse on first access, best for one-time processing
HtmlDocument lazyDoc = new HtmlDocument(html, DocumentConfig.DEFAULT);

// EAGER - Parse immediately, best for repeated access
HtmlDocument eagerDoc = new HtmlDocument(html, DocumentConfig.EAGER);

// CACHED - Cache frequently accessed paths, best for repeated queries
HtmlDocument cachedDoc = new HtmlDocument(html, DocumentConfig.CACHED);
String title1 = cachedDoc.textNode("title"); // Parsed
String title2 = cachedDoc.textNode("title"); // Cached (36x faster!)

// Custom configuration
DocumentConfig custom = DocumentConfig.custom(
    DocumentConfig.ParsingStrategy.LAZY,
    true,  // Enable cache
    200    // Cache size
);
HtmlDocument customDoc = new HtmlDocument(html, custom);
```

### Streaming API for Large Documents

```java
// Process large result sets efficiently
htmlDoc.nodeStream("div.item")
    .filter(item -> !item.isEmpty())
    .limit(100)
    .forEach(item -> process(item));

// Memory-efficient counting
long count = xmlDoc.nodeStream("//item").count();
```

### Web Scraping

```java
WebDocumentLoader loader = new WebDocumentLoader();
Document doc = loader.load("https://example.com");

// Automatically detects HTML or JSON based on Content-Type
String data = doc.textNode("selector or path");
```

## Distributed Processing

### Apache Spark Example

```java
JavaSparkContext sc = new JavaSparkContext(sparkConf);

// Load and parse HTML pages in parallel
JavaRDD<String> htmlPages = sc.textFile("s3://bucket/*.html");
JavaRDD<HtmlDocument> docs = htmlPages.map(HtmlDocument::new);

// Extract data across cluster
JavaRDD<String> titles = docs.map(doc -> doc.textNode("title"));

// Process with streaming for large documents
JavaRDD<Document> products = docs.flatMap(doc ->
    doc.nodeStream("div.product").iterator()
);

// Cache serialized documents for reuse
docs.cache(); // Only ~1x original size in memory
```

### Apache Beam / Dataflow Example

```java
Pipeline pipeline = Pipeline.create(options);

PCollection<String> jsonData = pipeline
    .apply(TextIO.read().from("gs://bucket/*.json"));

PCollection<JsonDocument> docs = jsonData
    .apply(MapElements.into(TypeDescriptor.of(JsonDocument.class))
        .via(JsonDocument::new));

PCollection<String> results = docs
    .apply(MapElements.into(TypeDescriptors.strings())
        .via(doc -> doc.textNode("$.field")));

pipeline.run();
```

## Architecture

### Document Interface

All document types implement the `Document` interface:

```java
public interface Document extends Serializable {
    String textNode(String path);
    List<CharSequence> textNodeList(String path);
    Document node(String path);
    List<Document> nodeList(String path);
    Stream<Document> nodeStream(String path);  // Memory-efficient streaming
    boolean isEmpty();
}
```

### Implementation Details

- **HtmlDocument**: Uses JSoup for CSS selector support
- **JsonDocument**: Uses JSONPath for flexible JSON querying
- **XmlDocument**: Uses XPath with cached TransformerFactory for performance

### Performance Optimizations

#### Phase 1: Core Optimizations
1. **Lazy Parsing**: Documents parse on first access, not on creation (<1µs creation time)
2. **Transient Fields**: Parsed representations excluded from serialization (75-85% memory reduction)
3. **Cached Factories**: Static ThreadLocal caching for thread-safe reuse (100-500x faster XML)
4. **Streaming API**: Process large result sets without full materialization

#### Phase 2 & 3: Advanced Optimizations
5. **Shared State**: Sub-documents share parent's parsed document (no re-parsing overhead)
6. **LRU Caching**: Frequently accessed paths cached for instant retrieval (36x faster)
7. **Configurable Strategies**: Choose LAZY, EAGER, or CACHED based on use case
8. **Smart Sub-Document Creation**: HTML/JSON sub-documents avoid string re-parsing

## Building and Testing

```bash
# Build project
mvn clean install

# Run all tests
mvn test

# Run performance benchmarks
mvn test -Dtest=PerformanceBenchmark

# Run serialization tests
mvn test -Dtest=SerializationTest
```

## Dependencies

- **jsoup** 1.15.3 - HTML parsing
- **json-path** 2.4.0 - JSON querying
- **httpclient** 4.5.13 - HTTP requests
- **JUnit Jupiter** 5.6.1 - Testing

## Performance Improvements (Dec 2025)

### Phase 1: Core Optimizations (Completed)
- **Instant initialization**: <1µs document creation (lazy parsing)
- **75-85% memory reduction**: Serialized size ~1.0x original (vs 4-7x in-memory)
- **100-500x faster XML**: Cached TransformerFactory for nodeList operations
- **40x faster partial results**: Streaming API with limit()
- **Full Spark/Beam support**: Serializable documents with minimal overhead

### Phase 2 & 3: Advanced Optimizations (Completed)
- **Shared state for sub-documents**: Near-instant sub-document access (<1µs)
- **36x faster with LRU caching**: Repeated path queries cached automatically
- **Configurable parsing strategies**: LAZY, EAGER, or CACHED modes
- **Zero re-parsing overhead**: Sub-documents share parent's parsed state
- **Deep navigation optimized**: 3-level deep navigation in 28ms (1000 leaf nodes)

See `PERFORMANCE_IMPROVEMENTS.md` for detailed Phase 1 analysis.
See `Phase2And3Benchmark.java` for Phase 2 & 3 benchmarks.

## License

This project is available under standard open source terms.

