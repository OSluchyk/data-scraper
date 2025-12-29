package os.tool.model;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Stream;

public interface Document extends Serializable {
    String textNode(String path);

    List<CharSequence> textNodeList(String path);

    Document node(String path);

    List<Document> nodeList(String path);

    /**
     * Returns a stream of sub-documents for memory-efficient processing of large result sets.
     * Prefer this over nodeList() for large documents to avoid loading all results into memory.
     * This enables processing of huge documents without holding all nodes in memory at once.
     *
     * @param path the path expression to select nodes
     * @return a stream of Document instances matching the path
     */
    default Stream<Document> nodeStream(String path) {
        return nodeList(path).stream();
    }

    boolean isEmpty();
}