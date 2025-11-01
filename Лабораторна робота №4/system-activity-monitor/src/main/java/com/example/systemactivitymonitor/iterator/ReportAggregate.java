package com.example.systemactivitymonitor.iterator;

import com.example.systemactivitymonitor.model.Report;
import com.example.systemactivitymonitor.model.HourStat;

public class ReportAggregate implements Aggregate<HourStat> {

    private final Report report;

    public ReportAggregate(Report report) {
        this.report = report;
    }

    @Override
    public Iterator<HourStat> createIterator() {
        return new DeepReportIterator(report);
    }

    @Override
    public int size() {
        if (report.getDays() == null) return 0;
        return report.getDays().stream()
                .mapToInt(d -> d.getHourlyStats().size())
                .sum();
    }

    @Override
    public HourStat get(int index) {
        if (report.getDays() == null) return null;
        int count = 0;
        for (var day : report.getDays()) {
            for (var stat : day.getHourlyStats()) {
                if (count == index) return stat;
                count++;
            }
        }
        return null;
    }
}
