package com.example.systemactivitymonitor.iterator;

import com.example.systemactivitymonitor.model.HourStat;
import com.example.systemactivitymonitor.model.Report;

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
        Iterator<HourStat> it = createIterator();
        int count = 0;
        it.first();
        while (!it.isDone()) {
            if (it.currentItem() != null) count++;
            it.next();
        }
        return count;
    }

    @Override
    public HourStat get(int index) {
        if (index < 0) return null;
        Iterator<HourStat> it = createIterator();
        int i = 0;
        it.first();
        while (!it.isDone()) {
            HourStat cur = it.currentItem();
            if (cur != null) {
                if (i == index) return cur;
                i++;
            }
            it.next();
        }
        return null;
    }
}
