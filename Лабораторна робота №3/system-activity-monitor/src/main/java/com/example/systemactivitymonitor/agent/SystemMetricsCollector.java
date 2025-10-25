package com.example.systemactivitymonitor.agent;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.software.os.OperatingSystem;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class SystemMetricsCollector {

    private final SystemInfo systemInfo;
    private final CentralProcessor processor;
    private final GlobalMemory memory;
    private final OperatingSystem os;

    private long[] prevTicks;

    public SystemMetricsCollector() {
        systemInfo = new SystemInfo();
        processor = systemInfo.getHardware().getProcessor();
        memory = systemInfo.getHardware().getMemory();
        os = systemInfo.getOperatingSystem();
        prevTicks = processor.getSystemCpuLoadTicks(); // перший замір
    }

    public BigDecimal getCpuLoad() {
        long[] newTicks = processor.getSystemCpuLoadTicks();
        double load = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100;
        prevTicks = newTicks; // оновлюємо для наступного виклику
        return BigDecimal.valueOf(load).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getMemoryUsageMb() {
        long used = memory.getTotal() - memory.getAvailable();
        return BigDecimal.valueOf(used / 1024.0 / 1024.0)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public long getUptimeSeconds() {
        return os.getSystemUptime();
    }

    public String getOsName() {
        return os.toString();
    }
}
