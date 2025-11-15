package com.example.systemactivitymonitor.metrics;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 🔧 MetricsProvider — інтерфейс "реалізації" для шаблону Міст.
 * Дозволяє створювати кросплатформені реалізації збору метрик.
 */
public interface MetricsProvider {
    BigDecimal getCpuLoad();
    BigDecimal getMemoryUsage();
    void updateDiskStats();
    String getActiveWindowTitle();
    String getUptime();
    void updateInputActivity();
    Map<String, Long> getInputStats();
    Map<String, Object> collectAllMetrics();
}
