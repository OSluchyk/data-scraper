package os.tool.model.impl;

import os.tool.model.Document;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.PathNotFoundException;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.jayway.jsonpath.JsonPath.parse;

public class JsonDocument implements Document {
    private static final long serialVersionUID = 1L;

    private final String json;
    private transient DocumentContext document;

    // Shared state for sub-documents
    private transient DocumentContext sharedRootContext;
    private transient Object subObject; // The sub-object this document represents

    // Root document constructor
    public JsonDocument(String json) {
        this.json = json;
        this.sharedRootContext = null;
        this.subObject = null;
        // Lazy parsing - don't parse until needed
    }

    // Sub-document constructor (shares parsed context)
    private JsonDocument(String json, DocumentContext sharedContext, Object obj) {
        this.json = json;
        this.sharedRootContext = sharedContext;
        this.subObject = obj;
        this.document = null; // Will parse from string only if needed
    }

    private DocumentContext getDocument() {
        if (document == null) {
            document = parse(json);
            document.configuration().setOptions(Option.DEFAULT_PATH_LEAF_TO_NULL, Option.SUPPRESS_EXCEPTIONS);
        }
        return document;
    }

    @Override
    public String textNode(String path) {
        try {
            return getDocument().read(path);
        } catch (PathNotFoundException e) {
            return null;
        }
    }

    @Override
    public List<CharSequence> textNodeList(String path) {
        try {
            return getDocument().read(path);
        } catch (PathNotFoundException e) {
            return Collections.emptyList();
        }
    }

    @Override
    public Document node(String path) {
        DocumentContext ctx = getDocument();
        Map propertyMap = ctx.read(path);
        String jsonString = parse(propertyMap).jsonString();
        // Share the root context to avoid re-parsing in sub-documents
        return new JsonDocument(jsonString, ctx, propertyMap);
    }

    @Override
    public List<Document> nodeList(String path) {
        DocumentContext ctx = getDocument();
        List<Map> docs = ctx.read(path);
        return docs.stream()
                .map(m -> {
                    String jsonString = parse(m).jsonString();
                    return new JsonDocument(jsonString, ctx, m);
                })
                .collect(Collectors.toList());
    }

    @Override
    public Stream<Document> nodeStream(String path) {
        DocumentContext ctx = getDocument();
        List<Map> docs = ctx.read(path);
        return docs.stream()
                .map(m -> {
                    String jsonString = parse(m).jsonString();
                    return new JsonDocument(jsonString, ctx, m);
                });
    }

    @Override
    public boolean isEmpty() {
        return json.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JsonDocument that = (JsonDocument) o;

        return json != null ? json.equals(that.json) : that.json == null;
    }

    @Override
    public int hashCode() {
        return json != null ? json.hashCode() : 0;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("JsonDocument{");
        sb.append("json='").append(json).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
