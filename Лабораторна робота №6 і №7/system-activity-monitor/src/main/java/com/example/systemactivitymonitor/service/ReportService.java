package com.example.systemactivitymonitor.service;

import com.example.systemactivitymonitor.iterator.Iterator;
import com.example.systemactivitymonitor.iterator.ReportAggregate;
import com.example.systemactivitymonitor.model.*;
import com.example.systemactivitymonitor.repository.interfaces.*;
import com.example.systemactivitymonitor.service.calculations.ReportCalculator;
import com.example.systemactivitymonitor.service.export.ReportExportFactory;
import com.example.systemactivitymonitor.service.export.ReportExporter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ReportService {

    private final ReportRepository reportRepo;
    private final StatsRepository statsRepo;
    private final IdleRepository idleRepo;
    private final ReportCalculator calculator = new ReportCalculator();

    public ReportService(ReportRepository rr, StatsRepository sr, IdleRepository ir) {
        this.reportRepo = rr;
        this.statsRepo = sr;
        this.idleRepo = ir;
    }

    // -----------------------------------------------------------------
    // CREATE
    // -----------------------------------------------------------------
    public Report generateReport(User user, String name, LocalDate start, LocalDate end) {
        validateUser(user);
        validatePeriod(start, end);

        LocalDateTime from = start.atStartOfDay();
        LocalDateTime to = end.atTime(23, 59, 59);

        List<SystemStats> stats = statsRepo.findByUserIdAndRecordedAtBetween(user.getId(), from, to);
        List<IdleTime> idle = idleRepo.findByUserIdAndStartTimeBetween(user.getId(), from, to);

        Report r = new Report();
        r.setUser(user);
        r.setReportName(name);
        r.setPeriodStart(start);
        r.setPeriodEnd(end);
        r.setCpuAvg(calculator.average(stats, SystemStats::getCpuLoad));
        r.setRamAvg(calculator.average(stats, SystemStats::getRamUsedMb));
        r.setIdleTimeTotalSeconds(calculator.totalIdle(idle));
        r.setAppUsagePercent(calculator.appUsagePercent(stats));
        r.setDays(calculator.buildDaySummary(stats));
        r.setAvgUptimeHours(calculator.averageUptime(stats));

        reportRepo.save(r);
        return r;
    }

    // -----------------------------------------------------------------
    // READ
    // -----------------------------------------------------------------
    public Report findById(Integer id) {
        return reportRepo.findById(id).orElse(null);
    }

    public List<Report> getReportsByUser(User user) {
        validateUser(user);
        return reportRepo.findByUserIdAndCreatedAtBetween(
                user.getId(),
                LocalDateTime.MIN,
                LocalDateTime.MAX
        );
    }

    public String getCpuAndRamReport(User user, Report report) {
        validateUser(user);

        if (report == null || report.getDays() == null) {
            return "Даних по годинах немає.";
        }

        ReportAggregate aggregate = new ReportAggregate(report);
        Iterator<HourStat> it = aggregate.createIterator();
        it.first();

        StringBuilder sb = new StringBuilder();

        while (!it.isDone()) {
            HourStat h = it.currentItem();
            if (h != null) {
                sb.append(String.format(
                        "%02d:00 — CPU: %.2f%% | RAM: %.2f MB%n",
                        h.getHour(),
                        h.getAvgCpu(),
                        h.getAvgRam()
                ));
            }
            it.next();
        }

        return sb.length() == 0 ? "Даних по годинах немає." : sb.toString();
    }

    public List<Report> getReportsInPeriod(User user, LocalDate start, LocalDate end) {
        validateUser(user);
        validatePeriod(start, end);

        return reportRepo.findByUserIdAndCreatedAtBetween(
                user.getId(),
                start.atStartOfDay(),
                end.atTime(23, 59, 59)
        );
    }

    // -----------------------------------------------------------------
    // DELETE
    // -----------------------------------------------------------------
    public void deleteReport(Integer id) {
        reportRepo.deleteById(id);
    }

    // -----------------------------------------------------------------
    // RESTORE
    // -----------------------------------------------------------------
    public void restoreReport(Report report) {
        if (report == null) return;
        reportRepo.save(report); // повертаємо назад
    }

    // -----------------------------------------------------------------
    // EXPORT
    // -----------------------------------------------------------------
    public Path export(Report report, String format) throws Exception {
        ReportExporter exporter = ReportExportFactory.getExporter(format);
        return exporter.export(report);
    }

    public void deleteExportedFile(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (Exception ignored) {}
    }

    // -----------------------------------------------------------------
    // INTERNAL
    // -----------------------------------------------------------------
    private void validateUser(User user) {
        if (user == null || user.getId() == null)
            throw new IllegalArgumentException("User is not defined.");
    }

    private void validatePeriod(LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start))
            throw new IllegalArgumentException("Invalid date range.");
    }
}
