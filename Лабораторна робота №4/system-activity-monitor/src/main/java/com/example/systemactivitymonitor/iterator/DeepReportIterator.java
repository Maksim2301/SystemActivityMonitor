package com.example.systemactivitymonitor.iterator;

import com.example.systemactivitymonitor.model.*;

public class DeepReportIterator implements Iterator<HourStat> {
    private final Report report;
    private int dayIndex = 0;
    private int hourIndex = 0;

    public DeepReportIterator(Report report) {
        this.report = report;
    }

    @Override
    public void first() {
        dayIndex = 0;
        hourIndex = 0;
    }

    @Override
    public void next() {
        hourIndex++;
        if (dayIndex < report.getDays().size()) {
            DaySummary day = report.getDays().get(dayIndex);
            if (hourIndex >= day.getHourlyStats().size()) {
                dayIndex++;
                hourIndex = 0;
            }
        }
    }

    @Override
    public boolean isDone() {
        return dayIndex >= report.getDays().size();
    }

    @Override
    public HourStat currentItem() {
        if (isDone()) return null;
        return report.getDays().get(dayIndex).getHourlyStats().get(hourIndex);
    }
}
