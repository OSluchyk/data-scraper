package os.tool.model;

import java.util.List;

public interface Document {
    String textNode(String path);

    List<CharSequence> textNodeList(String path);

    Document node(String path);

    List<Document> nodeList(String path);
}