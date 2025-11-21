package com.example.systemactivitymonitor.factory;

import com.example.systemactivitymonitor.metrics.MetricsProvider;
import com.example.systemactivitymonitor.metrics.impl.LinuxMetricsProvider;
import com.example.systemactivitymonitor.service.*;

/**
 * 🐧 Конкретна фабрика для Linux.
 */
public class LinuxSystemFactory implements SystemEnvironmentFactory {

    private final MetricsProvider provider = new LinuxMetricsProvider();

    @Override
    public MetricsProvider createMetricsProvider() {
        return provider;
    }

    @Override
    public MonitoringService createMonitoringService() {
        return new AdvancedMonitoringService(provider);
    }

}
