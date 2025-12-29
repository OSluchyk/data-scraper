package os.tool.model.config;

import java.io.Serializable;

/**
 * Configuration for document parsing and caching behavior.
 * Allows fine-tuning performance characteristics based on use case.
 */
public class DocumentConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    private final ParsingStrategy parsingStrategy;
    private final boolean enableCache;
    private final int cacheSize;

    public enum ParsingStrategy {
        /**
         * Parse on first access (default).
         * Best for: One-time document processing, distributed frameworks
         * Memory: Lowest when serialized
         */
        LAZY,

        /**
         * Parse immediately on construction.
         * Best for: Repeated access to same document, local processing
         * Memory: Higher, but faster access
         */
        EAGER,

        /**
         * Parse lazily + cache frequently accessed paths.
         * Best for: Repeated queries on same paths
         * Memory: Medium, cache overhead
         */
        CACHED
    }

    // Default configuration
    public static final DocumentConfig DEFAULT = new DocumentConfig(
            ParsingStrategy.LAZY,
            false,
            0
    );

    // Eager parsing configuration
    public static final DocumentConfig EAGER = new DocumentConfig(
            ParsingStrategy.EAGER,
            false,
            0
    );

    // Cached configuration with default cache size
    public static final DocumentConfig CACHED = new DocumentConfig(
            ParsingStrategy.CACHED,
            true,
            100
    );

    public DocumentConfig(ParsingStrategy parsingStrategy, boolean enableCache, int cacheSize) {
        this.parsingStrategy = parsingStrategy;
        this.enableCache = enableCache;
        this.cacheSize = cacheSize;
    }

    public ParsingStrategy getParsingStrategy() {
        return parsingStrategy;
    }

    public boolean isEnableCache() {
        return enableCache;
    }

    public int getCacheSize() {
        return cacheSize;
    }

    public boolean isLazy() {
        return parsingStrategy == ParsingStrategy.LAZY;
    }

    public boolean isEager() {
        return parsingStrategy == ParsingStrategy.EAGER;
    }

    public boolean isCached() {
        return parsingStrategy == ParsingStrategy.CACHED || enableCache;
    }

    @Override
    public String toString() {
        return "DocumentConfig{" +
                "parsingStrategy=" + parsingStrategy +
                ", enableCache=" + enableCache +
                ", cacheSize=" + cacheSize +
                '}';
    }

    /**
     * Create a custom configuration.
     */
    public static DocumentConfig custom(ParsingStrategy strategy, boolean enableCache, int cacheSize) {
        return new DocumentConfig(strategy, enableCache, cacheSize);
    }
}
