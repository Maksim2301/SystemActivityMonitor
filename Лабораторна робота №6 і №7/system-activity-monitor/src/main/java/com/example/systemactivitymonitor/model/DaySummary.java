package com.example.systemactivitymonitor.model;

import java.time.LocalDate;
import java.util.List;

public class DaySummary {
    private LocalDate date;
    private List<HourStat> hourlyStats;

    public DaySummary() {}

    public DaySummary(LocalDate date, List<HourStat> hourlyStats) {
        this.date = date;
        this.hourlyStats = hourlyStats;
    }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public List<HourStat> getHourlyStats() { return hourlyStats; }
    public void setHourlyStats(List<HourStat> hourlyStats) { this.hourlyStats = hourlyStats; }
}
