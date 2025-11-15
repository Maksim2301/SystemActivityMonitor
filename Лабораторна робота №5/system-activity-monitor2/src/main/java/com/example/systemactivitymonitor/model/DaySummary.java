package com.example.systemactivitymonitor.model;

import java.time.LocalDate;
import java.util.List;

public class DaySummary {
    private LocalDate date;
    private List<HourStat> hourlyStats;

    public DaySummary(LocalDate date, List<HourStat> hourlyStats) {
        this.date = date;
        this.hourlyStats = hourlyStats;
    }

    public LocalDate getDate() { return date; }
    public List<HourStat> getHourlyStats() { return hourlyStats; }
}
