package os.tool.model.impl;

import os.tool.model.Document;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class HtmlDocument implements Document {
    private final String html;
    private String url;

    private final org.jsoup.nodes.Document document;

    public HtmlDocument(String html) {
        this.html = html;
        this.document = Jsoup.parse(html);
    }

    @Override
    public String textNode(String path) {
        String[] parts = path.split("@");
        Elements el = document.select(parts[0]);
        return parts.length == 2 ? el.attr(parts[1]) : el.text();
    }

    @Override
    public List<CharSequence> textNodeList(String path) {
        String[] parts = path.split("@");
        return document.select(parts[0]).stream()
                .map(element -> parts.length == 2 ? element.attr(parts[1]) : element.text())
                .collect(toList());
    }

    @Override
    public Document node(String path) {
        return new HtmlDocument(document.selectFirst(path).html());
    }

    @Override
    public List<Document> nodeList(String path) {
        return document.select(path).stream().map(el -> new HtmlDocument(el.html()))
                .collect(Collectors.toList());
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
