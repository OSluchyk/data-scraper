package os.tool.loader;

import java.io.IOException;

public interface DataLoader<T> extends AutoCloseable {
    T load(String path) throws IOException;

    default void close() {
    }
}