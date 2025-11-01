package com.example.systemactivitymonitor.service;

import com.example.systemactivitymonitor.iterator.Iterator;
import com.example.systemactivitymonitor.iterator.ReportAggregate;
import com.example.systemactivitymonitor.model.*;
import com.example.systemactivitymonitor.repository.impl.*;
import com.example.systemactivitymonitor.repository.interfaces.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


public class ReportService {

    private final ReportRepository reportRepository = new ReportRepositoryImpl();
    private final StatsRepository statsRepository = new StatsRepositoryImpl();
    private final IdleRepository idleRepository = new IdleRepositoryImpl();

    /** Створення нового звіту */
    public Report generateReport(User user, String reportName, LocalDate startDate, LocalDate endDate) {
        if (user == null || user.getId() == null)
            throw new IllegalArgumentException("Користувач не заданий або не збережений (id == null).");
        if (startDate == null || endDate == null || endDate.isBefore(startDate))
            throw new IllegalArgumentException("Некоректний період для звіту.");

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        List<SystemStats> stats = statsRepository.findByUserIdAndRecordedAtBetween(user.getId(), start, end);
        List<IdleTime> idleTimes = idleRepository.findByUserIdAndStartTimeBetween(user.getId(), start, end);

        BigDecimal cpuAvg = calculateAverage(stats, SystemStats::getCpuLoad);
        BigDecimal ramAvg = calculateAverage(stats, SystemStats::getMemoryUsageMb);
        BigDecimal idleSeconds = calculateTotalIdleTime(idleTimes);
        Map<String, BigDecimal> appUsagePercent = calculateAppUsagePercent(stats);

        List<DaySummary> days = buildDaySummary(stats);

        BigDecimal avgUptime = calculateAverageUptimeByDay(stats);

        Report report = new Report();
        report.setUser(user);
        report.setReportName(reportName);
        report.setPeriodStart(startDate);
        report.setPeriodEnd(endDate);
        report.setCpuAvg(cpuAvg);
        report.setRamAvg(ramAvg);
        report.setIdleTimeTotalSeconds(idleSeconds);
        report.setAppUsagePercent(appUsagePercent);
        report.setDays(days); // 🆕 вкладена структура для ітератора
        report.setFilePath("Середній аптайм: " + avgUptime + " год/день");

        reportRepository.save(report);
        return report;
    }

    /** Отримати всі звіти користувача */
    public List<Report> getReportsByUser(User user) {
        if (user == null || user.getId() == null)
            throw new IllegalArgumentException("Користувач не заданий або не збережений (id == null).");

        LocalDateTime start = LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.now();

        return reportRepository.findByUserIdAndCreatedAtBetween(user.getId(), start, end);
    }

    /** Отримати звіти за період */
    public List<Report> getReportsInPeriod(User user, LocalDate startDate, LocalDate endDate) {
        if (user == null || user.getId() == null)
            throw new IllegalArgumentException("Користувач не заданий або не збережений (id == null).");
        if (startDate == null || endDate == null || endDate.isBefore(startDate))
            throw new IllegalArgumentException("Некоректний період.");

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        return reportRepository.findByUserIdAndCreatedAtBetween(user.getId(), start, end)
                .stream()
                .filter(r -> r.getPeriodStart() != null && r.getPeriodEnd() != null &&
                        !r.getPeriodStart().isBefore(startDate) &&
                        !r.getPeriodEnd().isAfter(endDate))
                .toList();
    }

    /** Детальний звіт CPU/RAM по годинах */
    public String getCpuAndRamReport(User user, Report report) {
        if (report == null) return "Немає звіту.";
        if (user == null || user.getId() == null) return "Користувач не визначений.";

        if (report.getDays() == null || report.getDays().isEmpty()) {
            LocalDate start = report.getPeriodStart();
            LocalDate end   = report.getPeriodEnd();
            if (start == null || end == null) return "Немає періоду у звіті.";

            var stats = statsRepository.findByUserIdAndRecordedAtBetween(
                    user.getId(),
                    start.atStartOfDay(),
                    end.atTime(23, 59, 59)
            );
            report.setDays(buildDaySummary(stats));
        }

        if (report.getDays() == null || report.getDays().isEmpty())
            return "Немає даних для звіту.";

        ReportAggregate aggregate = new ReportAggregate(report);
        Iterator<HourStat> it = aggregate.createIterator();

        StringBuilder sb = new StringBuilder("CPU/RAM по годинах:\n");
        for (it.first(); !it.isDone(); it.next()) {
            HourStat stat = it.currentItem();
            sb.append(String.format("Година %02d → CPU: %.2f%% | RAM: %.2fMB%n",
                    stat.getHour(), stat.getAvgCpu(), stat.getAvgRam()));
        }
        return sb.toString();
    }

    /** Видалення звіту */
    public void deleteReport(Integer reportId) {
        if (reportId == null)
            throw new IllegalArgumentException("ID звіту не може бути null.");
        reportRepository.deleteById(reportId);
    }

    // ===================================================================
    //  Допоміжні методи
    // ===================================================================

    /**  Середнє значення для будь-якої метрики */
    private static BigDecimal calculateAverage(List<SystemStats> stats, Function<SystemStats, BigDecimal> mapper) {
        if (stats == null || stats.isEmpty()) return BigDecimal.ZERO;
        return stats.stream()
                .map(mapper)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(stats.size()), 2, RoundingMode.HALF_UP);
    }

    /**  Загальний час простою */
    private static BigDecimal calculateTotalIdleTime(List<IdleTime> idleTimes) {
        if (idleTimes == null || idleTimes.isEmpty()) return BigDecimal.ZERO;
        long totalSeconds = idleTimes.stream()
                .map(IdleTime::getDurationSeconds)
                .filter(Objects::nonNull)
                .mapToLong(Integer::longValue)
                .sum();
        return BigDecimal.valueOf(totalSeconds);
    }

    /**  Розподіл використання програм */
    private static Map<String, BigDecimal> calculateAppUsagePercent(List<SystemStats> stats) {
        if (stats == null || stats.isEmpty()) return Map.of();

        long total = stats.size();
        Map<String, Long> counts = stats.stream()
                .map(SystemStats::getActiveWindow)
                .filter(Objects::nonNull)
                .map(ReportService::normalizeAppName)
                .collect(Collectors.groupingBy(name -> name, LinkedHashMap::new, Collectors.counting()));

        return counts.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> BigDecimal.valueOf(e.getValue() * 100.0 / total)
                                .setScale(2, RoundingMode.HALF_UP),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    /**  Побудова вкладеної структури днів і годин */
    private List<DaySummary> buildDaySummary(List<SystemStats> stats) {
        if (stats == null || stats.isEmpty()) return List.of();

        Map<LocalDate, Map<Integer, List<SystemStats>>> grouped =
                stats.stream()
                        .filter(s -> s.getRecordedAt() != null)
                        .collect(Collectors.groupingBy(
                                s -> s.getRecordedAt().toLocalDate(),
                                Collectors.groupingBy(s -> s.getRecordedAt().getHour())
                        ));

        return grouped.entrySet().stream()
                .map(dayEntry -> {
                    LocalDate date = dayEntry.getKey();
                    List<HourStat> hourly = dayEntry.getValue().entrySet().stream()
                            .map(hourEntry -> {
                                int hour = hourEntry.getKey();
                                List<SystemStats> list = hourEntry.getValue();
                                BigDecimal avgCpu = calculateAverage(list, SystemStats::getCpuLoad);
                                BigDecimal avgRam = calculateAverage(list, SystemStats::getMemoryUsageMb);
                                return new HourStat(hour, avgCpu, avgRam);
                            })
                            .toList();
                    return new DaySummary(date, hourly);
                })
                .toList();
    }

    /** Нормалізація назв активних вікон (заміна на відомі назви) */
    private static String normalizeAppName(String title) {
        if (title == null) return "Unknown";
        String lower = title.toLowerCase();
        if (lower.contains("chrome")) return "Google Chrome";
        if (lower.contains("firefox")) return "Mozilla Firefox";
        if (lower.contains("edge")) return "Microsoft Edge";
        if (lower.contains("opera")) return "Opera Browser";
        if (lower.contains("word")) return "MS Word";
        if (lower.contains("excel")) return "MS Excel";
        if (lower.contains("telegram")) return "Telegram";
        if (lower.contains("viber")) return "Viber";
        if (lower.contains("explorer") || lower.contains("file")) return "File Explorer";
        if (lower.contains("idea")) return "IntelliJ IDEA";
        if (lower.contains("studio")) return "Android Studio";
        return title.length() > 40 ? title.substring(0, 40) + "..." : title;
    }

    /**  Середній аптайм по днях */
    public BigDecimal calculateAverageUptimeByDay(List<SystemStats> stats) {
        if (stats == null || stats.isEmpty()) return BigDecimal.ZERO;
        Map<LocalDate, List<SystemStats>> grouped = stats.stream()
                .filter(s -> s.getRecordedAt() != null)
                .collect(Collectors.groupingBy(s -> s.getRecordedAt().toLocalDate()));
        List<BigDecimal> durations = grouped.values().stream()
                .map(day -> day.stream()
                        .mapToLong(s -> Optional.ofNullable(s.getSystemUptimeSeconds()).orElse(0L))
                        .max().orElse(0L))
                .map(v -> BigDecimal.valueOf(v / 3600.0).setScale(2, RoundingMode.HALF_UP))
                .toList();
        if (durations.isEmpty()) return BigDecimal.ZERO;
        return durations.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(durations.size()), 2, RoundingMode.HALF_UP);
    }
}
