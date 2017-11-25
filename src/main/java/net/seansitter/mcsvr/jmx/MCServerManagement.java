package net.seansitter.mcsvr.jmx;

import javax.inject.Inject;
import javax.management.*;
import java.lang.management.ManagementFactory;

public class MCServerManagement {
    private final CacheMetricsJmxMBean cacheMetricsMBean;

    @Inject
    public MCServerManagement(CacheMetricsJmxMBean cacheMetricsBean) {
        this.cacheMetricsMBean = cacheMetricsBean;
    }

    public void start() throws MalformedObjectNameException, NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException {
        //Get the MBean server
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        //register the MBean
        ObjectName name = null;
        name = new ObjectName("net.seansitter.mcserver.jmx:type=CacheMetrics");
        mbs.registerMBean(cacheMetricsMBean, name);
    }
}
