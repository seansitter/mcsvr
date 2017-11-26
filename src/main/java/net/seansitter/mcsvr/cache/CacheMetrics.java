package net.seansitter.mcsvr.cache;

/**
 * Interface for cache metrics providers
 */
public interface CacheMetrics {
    long getHits();
    long getMisses();
    long getItems();
    long getSize();
}
