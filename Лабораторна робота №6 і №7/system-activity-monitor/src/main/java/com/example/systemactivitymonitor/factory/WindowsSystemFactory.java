package com.example.systemactivitymonitor.factory;

import com.example.systemactivitymonitor.metrics.MetricsProvider;
import com.example.systemactivitymonitor.metrics.impl.WindowsMetricsProvider;
import com.example.systemactivitymonitor.service.*;

/**
 * 🪟 Конкретна фабрика для Windows.
 */
public class WindowsSystemFactory implements SystemEnvironmentFactory {

    private final MetricsProvider provider = new WindowsMetricsProvider();

    @Override
    public MetricsProvider createMetricsProvider() {
        return provider;
    }

    @Override
    public MonitoringService createMonitoringService() {
        return new AdvancedMonitoringService(provider);
    }
}

