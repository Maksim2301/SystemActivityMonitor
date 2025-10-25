package com.example.systemactivitymonitor.service.impl;

import com.example.systemactivitymonitor.model.IdleTime;
import com.example.systemactivitymonitor.model.Report;
import com.example.systemactivitymonitor.model.SystemStats;
import com.example.systemactivitymonitor.model.User;
import com.example.systemactivitymonitor.repository.impl.IdleRepositoryImpl;
import com.example.systemactivitymonitor.repository.impl.ReportRepositoryImpl;
import com.example.systemactivitymonitor.repository.impl.StatsRepositoryImpl;
import com.example.systemactivitymonitor.repository.interfaces.IdleRepository;
import com.example.systemactivitymonitor.repository.interfaces.ReportRepository;
import com.example.systemactivitymonitor.repository.interfaces.StatsRepository;
import com.example.systemactivitymonitor.service.interfaces.ReportService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository = new ReportRepositoryImpl();
    private final StatsRepository  statsRepository  = new StatsRepositoryImpl();
    private final IdleRepository   idleRepository   = new IdleRepositoryImpl();

    @Override
    public Report generateReport(User user, String reportName, LocalDate startDate, LocalDate endDate) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("Користувач не заданий або не збережений (id == null).");
        }
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Некоректний період для звіту.");
        }

        List<SystemStats> stats = statsRepository.findByUserId(user.getId()).stream()
                .filter(s -> s.getRecordedAt() != null
                        && !s.getRecordedAt().toLocalDate().isBefore(startDate)
                        && !s.getRecordedAt().toLocalDate().isAfter(endDate))
                .toList();

        List<IdleTime> idleTimes = idleRepository.findByUserId(user.getId()).stream()
                .filter(i -> i.getStartTime() != null
                        && !i.getStartTime().toLocalDate().isBefore(startDate)
                        && (i.getEndTime() == null || !i.getEndTime().toLocalDate().isAfter(endDate)))
                .toList();

        Report report = Report.generate(user, reportName, startDate, endDate, stats, idleTimes);

        // Зберігаємо
        reportRepository.save(report);
        return report;
    }

    @Override
    public List<Report> getReportsByUser(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("Користувач не заданий або не збережений (id == null).");
        }
        return reportRepository.findByUserId(user.getId());
    }

    @Override
    public List<Report> getReportsInPeriod(User user, LocalDate startDate, LocalDate endDate) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("Користувач не заданий або не збережений (id == null).");
        }
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Некоректний період для фільтрації звітів.");
        }
        return reportRepository.findByUserId(user.getId()).stream()
                .filter(r ->
                        (r.getPeriodStart() != null && r.getPeriodEnd() != null &&
                                !r.getPeriodStart().isAfter(endDate) &&
                                !r.getPeriodEnd().isBefore(startDate))
                                ||
                                (r.getCreatedAt() != null &&
                                        !r.getCreatedAt().toLocalDate().isBefore(startDate) &&
                                        !r.getCreatedAt().toLocalDate().isAfter(endDate))
                )
                .toList();
    }

    @Override
    public void deleteReport(Integer reportId) {
        if (reportId == null) return;
        reportRepository.deleteById(reportId);
    }
}
