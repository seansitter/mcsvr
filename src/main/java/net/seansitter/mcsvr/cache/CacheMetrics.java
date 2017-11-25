package net.seansitter.mcsvr.cache;

public interface CacheMetrics {
    long getHits();
    long getMisses();
    long getItems();
    long getSize();
}
