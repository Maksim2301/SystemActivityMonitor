package com.example.systemactivitymonitor.iterator;

import com.example.systemactivitymonitor.model.*;

import java.util.List;

public class DeepReportIterator implements Iterator<HourStat> {
    private final Report report;
    private List<DaySummary> days;
    private int dayIndex = 0;
    private int hourIndex = 0;

    public DeepReportIterator(Report report) {
        this.report = report;
        this.days = report != null ? report.getDays() : null;
    }

    @Override
    public void first() {
        dayIndex = 0;
        hourIndex = 0;
        advanceToNextValid();
    }

    @Override
    public void next() {
        if (isDone()) return;
        hourIndex++;
        advanceToNextValid();
    }

    @Override
    public boolean isDone() {
        return days == null || dayIndex >= days.size();
    }

    @Override
    public HourStat currentItem() {
        if (isDone()) return null;
        DaySummary day = days.get(dayIndex);
        if (day == null || day.getHourlyStats() == null) return null;
        if (hourIndex < 0 || hourIndex >= day.getHourlyStats().size()) return null;
        return day.getHourlyStats().get(hourIndex);
    }

    private void advanceToNextValid() {
        if (days == null) return;
        while (dayIndex < days.size()) {
            DaySummary day = days.get(dayIndex);
            if (day == null || day.getHourlyStats() == null || day.getHourlyStats().isEmpty()) {
                dayIndex++;
                hourIndex = 0;
                continue;
            }
            if (hourIndex >= day.getHourlyStats().size()) {
                dayIndex++;
                hourIndex = 0;
                continue;
            }
            break;
        }
    }
}
