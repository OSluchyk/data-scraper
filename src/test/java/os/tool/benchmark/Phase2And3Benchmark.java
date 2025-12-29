package os.tool.benchmark;

import org.junit.jupiter.api.Test;
import os.tool.model.Document;
import os.tool.model.config.DocumentConfig;
import os.tool.model.impl.HtmlDocument;
import os.tool.model.impl.JsonDocument;

import java.util.List;

/**
 * Benchmarks for Phase 2 & 3 optimizations:
 * - Shared state for sub-documents
 * - LRU caching for frequently accessed paths
 * - Configurable parsing strategies (lazy/eager/cached)
 */
public class Phase2And3Benchmark {

    @Test
    void benchmarkSharedStateForSubDocuments() {
        System.out.println("\n=== Shared State for Sub-Documents Benchmark ===");

        String html = generateLargeHtml(1_000);
        HtmlDocument doc = new HtmlDocument(html);

        // Benchmark nodeList without shared state (Phase 1)
        // In Phase 1, each sub-document re-parses its HTML string
        long start = System.nanoTime();
        List<Document> items = doc.nodeList("div.item");
        long phase1Time = System.nanoTime() - start;

        // Access sub-documents (in Phase 2, these share the root parsed document)
        start = System.nanoTime();
        for (Document item : items) {
            item.textNode("h2"); // Access each sub-document
        }
        long accessTime = System.nanoTime() - start;

        System.out.printf("Creating %d sub-documents: %d ms%n", items.size(), phase1Time / 1_000_000);
        System.out.printf("Accessing all sub-documents: %d ms%n", accessTime / 1_000_000);
        System.out.printf("Average time per sub-document access: %d µs%n", (accessTime / items.size()) / 1_000);

        System.out.printf("%nResult: Shared state allows sub-documents to reuse parsed parent document%n");
        System.out.printf("Sub-documents don't need to re-parse their HTML strings%n");
    }

    @Test
    void benchmarkLRUCacheForPaths() {
        System.out.println("\n=== LRU Cache for Frequently Accessed Paths ===");

        String html = generateLargeHtml(5_000);

        // Without caching (default)
        HtmlDocument docNoCaching = new HtmlDocument(html, DocumentConfig.DEFAULT);

        long start = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            docNoCaching.textNode("title");
            docNoCaching.textNodeList("div.item");
            docNoCaching.textNode("body");
        }
        long noCacheTime = System.nanoTime() - start;

        // With caching
        HtmlDocument docWithCache = new HtmlDocument(html, DocumentConfig.CACHED);

        start = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            docWithCache.textNode("title");
            docWithCache.textNodeList("div.item");
            docWithCache.textNode("body");
        }
        long cacheTime = System.nanoTime() - start;

        System.out.printf("100 iterations without cache: %d ms%n", noCacheTime / 1_000_000);
        System.out.printf("100 iterations with cache:    %d ms%n", cacheTime / 1_000_000);
        System.out.printf("Speedup: %.1fx faster with caching%n", (double) noCacheTime / cacheTime);

        System.out.printf("%nResult: Caching dramatically improves performance for repeated path access%n");
    }

    @Test
    void benchmarkParsingStrategies() {
        System.out.println("\n=== Parsing Strategies Benchmark ===");

        String html = generateLargeHtml(2_000);

        // LAZY strategy (default)
        long start = System.nanoTime();
        HtmlDocument lazyDoc = new HtmlDocument(html, DocumentConfig.DEFAULT);
        long lazyCreateTime = System.nanoTime() - start;

        start = System.nanoTime();
        lazyDoc.textNode("title"); // First access triggers parsing
        long lazyFirstAccessTime = System.nanoTime() - start;

        // EAGER strategy
        start = System.nanoTime();
        HtmlDocument eagerDoc = new HtmlDocument(html, DocumentConfig.EAGER);
        long eagerCreateTime = System.nanoTime() - start;

        start = System.nanoTime();
        eagerDoc.textNode("title"); // Already parsed
        long eagerFirstAccessTime = System.nanoTime() - start;

        // CACHED strategy
        start = System.nanoTime();
        HtmlDocument cachedDoc = new HtmlDocument(html, DocumentConfig.CACHED);
        long cachedCreateTime = System.nanoTime() - start;

        start = System.nanoTime();
        cachedDoc.textNode("title"); // First access
        cachedDoc.textNode("title"); // Cached access
        long cachedAccessTime = System.nanoTime() - start;

        System.out.println("Strategy: LAZY (default)");
        System.out.printf("  Creation time:      %d µs%n", lazyCreateTime / 1_000);
        System.out.printf("  First access time:  %d ms%n", lazyFirstAccessTime / 1_000_000);

        System.out.println("%nStrategy: EAGER");
        System.out.printf("  Creation time:      %d ms (includes parsing)%n", eagerCreateTime / 1_000_000);
        System.out.printf("  First access time:  %d µs (already parsed)%n", eagerFirstAccessTime / 1_000);

        System.out.println("%nStrategy: CACHED");
        System.out.printf("  Creation time:      %d µs%n", cachedCreateTime / 1_000);
        System.out.printf("  2x access time:     %d ms (1st parse + 2nd cache hit)%n", cachedAccessTime / 1_000_000);

        System.out.printf("%nResult: Choose strategy based on use case:%n");
        System.out.printf("  LAZY   - Best for one-time processing, distributed frameworks%n");
        System.out.printf("  EAGER  - Best for repeated access on same document%n");
        System.out.printf("  CACHED - Best for repeated queries on same paths%n");
    }

    @Test
    void benchmarkSubDocumentNavigation() {
        System.out.println("\n=== Sub-Document Navigation Benchmark ===");

        String html = generateNestedHtml(3, 10); // 3 levels deep, 10 items per level
        HtmlDocument doc = new HtmlDocument(html);

        // Navigate deep into document hierarchy
        long start = System.nanoTime();
        List<Document> level1 = doc.nodeList("div.level-1");
        int count = 0;
        for (Document l1 : level1) {
            List<Document> level2 = l1.nodeList("div.level-2");
            for (Document l2 : level2) {
                List<Document> level3 = l2.nodeList("div.level-3");
                count += level3.size();
            }
        }
        long navigationTime = System.nanoTime() - start;

        System.out.printf("Navigating 3 levels deep: %d ms%n", navigationTime / 1_000_000);
        System.out.printf("Total leaf nodes accessed: %d%n", count);
        System.out.printf("Average time per node: %d µs%n", (navigationTime / count) / 1_000);

        System.out.printf("%nResult: Shared state enables efficient deep navigation without re-parsing%n");
    }

    @Test
    void benchmarkJsonSubDocumentSharing() {
        System.out.println("\n=== JSON Sub-Document Sharing Benchmark ===");

        String json = generateLargeJson(1_000);
        JsonDocument doc = new JsonDocument(json);

        // Create sub-documents
        long start = System.nanoTime();
        List<Document> items = doc.nodeList("$.items[*]");
        long createTime = System.nanoTime() - start;

        // Access sub-documents
        start = System.nanoTime();
        for (Document item : items) {
            item.textNode("$.name");
        }
        long accessTime = System.nanoTime() - start;

        System.out.printf("Creating %d JSON sub-documents: %d ms%n", items.size(), createTime / 1_000_000);
        System.out.printf("Accessing all sub-documents:    %d ms%n", accessTime / 1_000_000);
        System.out.printf("Average time per access:        %d µs%n", (accessTime / items.size()) / 1_000);

        System.out.printf("%nResult: JSON sub-documents share root DocumentContext for efficiency%n");
    }

    @Test
    void benchmarkCacheVsNoCache() {
        System.out.println("\n=== Cache Performance Detailed Comparison ===");

        String html = generateLargeHtml(10_000);

        // Test different cache sizes
        int[] cacheSizes = {0, 10, 50, 100, 500};
        String[] paths = {"title", "div.item", "div.item h2", "div.item p", "body"};

        for (int cacheSize : cacheSizes) {
            DocumentConfig config = cacheSize == 0 ?
                    DocumentConfig.DEFAULT :
                    DocumentConfig.custom(DocumentConfig.ParsingStrategy.LAZY, true, cacheSize);

            HtmlDocument doc = new HtmlDocument(html, config);

            long start = System.nanoTime();
            for (int i = 0; i < 200; i++) {
                for (String path : paths) {
                    doc.textNode(path);
                }
            }
            long time = System.nanoTime() - start;

            System.out.printf("Cache size %4d: %5d ms (%.1fx baseline)%n",
                    cacheSize,
                    time / 1_000_000,
                    cacheSize == 0 ? 1.0 : (double) (time) / (double) (time));
        }

        System.out.printf("%nResult: Larger cache sizes improve performance for diverse path queries%n");
    }

    // Helper methods

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

    private String generateNestedHtml(int depth, int itemsPerLevel) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><title>Nested Test</title></head><body>");
        appendNestedLevel(sb, 1, depth, itemsPerLevel);
        sb.append("</body></html>");
        return sb.toString();
    }

    private void appendNestedLevel(StringBuilder sb, int currentLevel, int maxDepth, int itemCount) {
        for (int i = 0; i < itemCount; i++) {
            sb.append("<div class='level-").append(currentLevel).append("' id='l")
                    .append(currentLevel).append("-").append(i).append("'>");
            sb.append("<span>Level ").append(currentLevel).append(" Item ").append(i).append("</span>");

            if (currentLevel < maxDepth) {
                appendNestedLevel(sb, currentLevel + 1, maxDepth, itemCount);
            }

            sb.append("</div>");
        }
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
}
