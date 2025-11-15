package com.example.systemactivitymonitor.factory;

import com.example.systemactivitymonitor.metrics.MetricsProvider;
import com.example.systemactivitymonitor.service.IdleService;
import com.example.systemactivitymonitor.service.MonitoringService;
import com.example.systemactivitymonitor.service.ReportService;

/**
 * 🏭 Abstract Factory — створює пов’язані між собою об’єкти:
 * MetricsProvider, MonitoringService, IdleService, ReportService
 * без залежності від конкретної операційної системи.
 */
public interface SystemEnvironmentFactory {

    MetricsProvider createMetricsProvider();

    MonitoringService createMonitoringService();

    IdleService createIdleService();

    ReportService createReportService();
}
