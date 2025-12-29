package os.tool.model.cache;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Simple thread-safe LRU (Least Recently Used) cache implementation.
 * Used to cache frequently accessed document paths to avoid repeated DOM traversal.
 *
 * @param <K> Key type (typically String for path)
 * @param <V> Value type (cached result)
 */
public class LRUCache<K, V> {
    private final int maxSize;
    private final LinkedHashMap<K, V> cache;

    public LRUCache(int maxSize) {
        this.maxSize = maxSize;
        // true for access-order (LRU), false for insertion-order
        this.cache = new LinkedHashMap<K, V>(maxSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > LRUCache.this.maxSize;
            }
        };
    }

    public synchronized V get(K key) {
        return cache.get(key);
    }

    public synchronized void put(K key, V value) {
        cache.put(key, value);
    }

    public synchronized void clear() {
        cache.clear();
    }

    public synchronized int size() {
        return cache.size();
    }

    public synchronized boolean containsKey(K key) {
        return cache.containsKey(key);
    }
}
