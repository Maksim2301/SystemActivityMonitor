package com.example.systemactivitymonitor.model;

import java.math.BigDecimal;

public class HourStat {
    private int hour;
    private BigDecimal avgCpu;
    private BigDecimal avgRam;

    public HourStat(int hour, BigDecimal avgCpu, BigDecimal avgRam) {
        this.hour = hour;
        this.avgCpu = avgCpu;
        this.avgRam = avgRam;
    }

    public int getHour() { return hour; }
    public BigDecimal getAvgCpu() { return avgCpu; }
    public BigDecimal getAvgRam() { return avgRam; }
}
