package com.example.systemactivitymonitor.factory;

import com.example.systemactivitymonitor.metrics.MetricsProvider;
import com.example.systemactivitymonitor.metrics.impl.WindowsMetricsProvider;
import com.example.systemactivitymonitor.service.*;

/**
 * 🪟 Конкретна фабрика для Windows.
 */
public class WindowsSystemFactory implements SystemEnvironmentFactory {

    @Override
    public MetricsProvider createMetricsProvider() {
        return new WindowsMetricsProvider();
    }

    @Override
    public MonitoringService createMonitoringService() {
        return new AdvancedMonitoringService(createMetricsProvider());
    }

    @Override
    public IdleService createIdleService() {
        return new IdleService();
    }

    @Override
    public ReportService createReportService() {
        return new ReportService();
    }
}
