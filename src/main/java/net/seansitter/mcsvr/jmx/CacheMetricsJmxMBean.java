package net.seansitter.mcsvr.jmx;

public interface CacheMetricsJmxMBean {
    long getHits();
    long getMisses();
    long getItems();
    long getSize();
}
