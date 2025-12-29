package os.tool.model.impl;

import os.tool.model.Document;
import os.tool.model.cache.LRUCache;
import os.tool.model.config.DocumentConfig;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class HtmlDocument implements Document {
    private static final long serialVersionUID = 1L;

    private final String html;
    private final DocumentConfig config;
    private transient org.jsoup.nodes.Document document;

    // Shared state for sub-documents to avoid re-parsing
    private transient org.jsoup.nodes.Document sharedRootDocument;
    private transient org.jsoup.nodes.Element element;

    // LRU cache for frequently accessed paths
    private transient LRUCache<String, Object> pathCache;

    // Root document constructor with default config
    public HtmlDocument(String html) {
        this(html, DocumentConfig.DEFAULT);
    }

    // Root document constructor with custom config
    public HtmlDocument(String html, DocumentConfig config) {
        this.html = html;
        this.config = config;
        this.sharedRootDocument = null;
        this.element = null;

        // Initialize cache if enabled
        if (config.isCached()) {
            this.pathCache = new LRUCache<>(config.getCacheSize());
        }

        // Eager parsing if configured
        if (config.isEager()) {
            this.document = Jsoup.parse(html);
        }
    }

    // Sub-document constructor (shares parsed document)
    private HtmlDocument(String html, org.jsoup.nodes.Document sharedRoot, org.jsoup.nodes.Element elem, DocumentConfig config) {
        this.html = html;
        this.config = config;
        this.sharedRootDocument = sharedRoot;
        this.element = elem;
        this.document = null; // Will use element or parse from string if needed

        // Inherit cache settings from parent
        if (config.isCached()) {
            this.pathCache = new LRUCache<>(config.getCacheSize());
        }
    }

    private org.jsoup.nodes.Document getDocument() {
        if (document == null) {
            // If this is a sub-document with an element, create a minimal parsed document
            if (element != null) {
                document = Jsoup.parse(html);
            } else {
                document = Jsoup.parse(html);
            }
        }
        return document;
    }

    private org.jsoup.nodes.Element getElement() {
        if (element != null) {
            return element;
        }
        // Fall back to root element of parsed document
        return getDocument();
    }

    @Override
    public String textNode(String path) {
        // Check cache first
        if (pathCache != null && pathCache.containsKey(path)) {
            return (String) pathCache.get(path);
        }

        String[] parts = path.split("@");
        Elements el = getElement().select(parts[0]);
        String result = parts.length == 2 ? el.attr(parts[1]) : el.text();

        // Cache result if caching enabled
        if (pathCache != null) {
            pathCache.put(path, result);
        }

        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<CharSequence> textNodeList(String path) {
        // Check cache first
        if (pathCache != null && pathCache.containsKey(path)) {
            return (List<CharSequence>) pathCache.get(path);
        }

        String[] parts = path.split("@");
        List<CharSequence> result = getElement().select(parts[0]).stream()
                .map(elem -> parts.length == 2 ? elem.attr(parts[1]) : elem.text())
                .collect(toList());

        // Cache result if caching enabled
        if (pathCache != null) {
            pathCache.put(path, result);
        }

        return result;
    }

    @Override
    public Document node(String path) {
        org.jsoup.nodes.Document doc = getDocument();
        org.jsoup.nodes.Element selected = getElement().selectFirst(path);
        // Share the root document and config to avoid re-parsing
        return new HtmlDocument(selected.html(), doc, selected, config);
    }

    @Override
    public List<Document> nodeList(String path) {
        org.jsoup.nodes.Document doc = getDocument();
        return getElement().select(path).stream()
                .map(el -> new HtmlDocument(el.html(), doc, el, config))
                .collect(Collectors.toList());
    }

    @Override
    public Stream<Document> nodeStream(String path) {
        org.jsoup.nodes.Document doc = getDocument();
        return getElement().select(path).stream()
                .map(el -> new HtmlDocument(el.html(), doc, el, config));
    }

    @Override
    public boolean isEmpty() {
        return html.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HtmlDocument that = (HtmlDocument) o;

        return Objects.equals(html, that.html);
    }

    @Override
    public int hashCode() {
        return html != null ? html.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "HtmlDocument{" +
                "html='" + html + '\'' +
                '}';
    }
}
