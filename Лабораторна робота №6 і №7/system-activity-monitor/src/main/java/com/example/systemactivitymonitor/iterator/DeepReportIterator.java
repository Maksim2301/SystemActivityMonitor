package com.example.systemactivitymonitor.iterator;

import com.example.systemactivitymonitor.model.DaySummary;
import com.example.systemactivitymonitor.model.HourStat;
import com.example.systemactivitymonitor.model.Report;

import java.util.List;

/**
 * Глибокий ітератор по звіту:
 * Перебирає всі HourStat у всіх днях.
 */
public class DeepReportIterator implements Iterator<HourStat> {

    private final List<DaySummary> days;

    private int dayIndex = 0;
    private int hourIndex = 0;

    public DeepReportIterator(Report report) {
        this.days = report != null ? report.getDays() : List.of();
    }

    @Override
    public void first() {
        dayIndex = 0;
        hourIndex = 0;
        moveToNextValid();
    }

    @Override
    public void next() {
        if (isDone()) return;

        hourIndex++;
        moveToNextValid();
    }

    @Override
    public boolean isDone() {
        return dayIndex >= days.size();
    }

    @Override
    public HourStat currentItem() {
        if (isDone()) return null;

        DaySummary day = days.get(dayIndex);
        List<HourStat> hours = day.getHourlyStats();

        if (hours == null || hourIndex >= hours.size()) return null;

        return hours.get(hourIndex);
    }

    /**
     * Переходимо до наступної валідної години або дня.
     */
    private void moveToNextValid() {
        while (dayIndex < days.size()) {
            DaySummary day = days.get(dayIndex);
            List<HourStat> hours = (day != null) ? day.getHourlyStats() : null;

            // Немає годин → рухаємося до наступного дня
            if (hours == null || hours.isEmpty()) {
                dayIndex++;
                hourIndex = 0;
                continue;
            }

            // Години закінчились → наступний день
            if (hourIndex >= hours.size()) {
                dayIndex++;
                hourIndex = 0;
                continue;
            }

            // Є валідний елемент
            return;
        }
    }
}
