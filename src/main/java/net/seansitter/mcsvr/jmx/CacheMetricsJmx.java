package net.seansitter.mcsvr.jmx;

import net.seansitter.mcsvr.cache.CacheMetrics;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * This is a mbean cache metrics wrapper to enable jmx support
 */
public class CacheMetricsJmx implements CacheMetricsJmxMBean {
    private final CacheMetrics cacheMetrics;

    @Inject
    public CacheMetricsJmx(@Named("cacheMetrics") CacheMetrics cacheMetrics) {
        this.cacheMetrics = cacheMetrics;
    }

    @Override
    public long getHits() {
        return cacheMetrics.getHits();
    }

    @Override
    public long getMisses() {
        return cacheMetrics.getMisses();
    }

    @Override
    public long getItems() {
        return cacheMetrics.getItems();
    }

    @Override
    public long getSize() {
        return cacheMetrics.getSize();
    }
}
