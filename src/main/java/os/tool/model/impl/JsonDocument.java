package os.tool.model.impl;

import os.tool.model.Document;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.PathNotFoundException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.jayway.jsonpath.JsonPath.parse;

public class JsonDocument implements Document {
    private final String json;
    private final DocumentContext document;


    public JsonDocument(String json) {
        this.json = json;
        this.document = parse(json);
        document.configuration().setOptions(Option.DEFAULT_PATH_LEAF_TO_NULL, Option.SUPPRESS_EXCEPTIONS);

    }

    @Override
    public String textNode(String path) {
        try {
            return document.read(path);
        } catch (PathNotFoundException e) {
            return null;
        }
    }

    @Override
    public List<CharSequence> textNodeList(String path) {
        try {
            return document.read(path);
        } catch (PathNotFoundException e) {
            return null;
        }

    }

    @Override
    public Document node(String path) {
        Map propertyMap = document.read(path);
        String json = parse(propertyMap).jsonString();
        return new JsonDocument(json);
    }

    @Override
    public List<Document> nodeList(String path) {
        List<Map> docs = document.read(path);
        return docs.stream().map(m -> new JsonDocument(parse(m).jsonString()))
                .collect(Collectors.toList());
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
}
