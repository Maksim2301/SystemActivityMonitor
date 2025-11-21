package com.example.systemactivitymonitor.metrics;

/**
 * Уніфіковані ключі метрик.
 * Використовуються провайдерами, MonitoringService та AdvancedMonitoringService.
 */
public final class MetricKeys {

    private MetricKeys() {}

    public static final String CPU = "cpuLoad";

    public static final String RAM_USED = "ramUsed";
    public static final String RAM_TOTAL = "ramTotal";

    public static final String DISK_TOTAL = "diskTotal";
    public static final String DISK_FREE = "diskFree";
    public static final String DISK_USED = "diskUsed";
    public static final String DISK_DETAILS = "diskDetails";

    public static final String WINDOW = "activeWindow";
    public static final String UPTIME = "uptime";

    public static final String OS = "osName";

    // Input
    public static final String KEYS = "keys";
    public static final String CLICKS = "clicks";
    public static final String MOVES = "moves";
    public static final String LAST_ACTIVITY = "lastActivitySecAgo";
}
