package com.example.systemactivitymonitor.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class Report {

    private Integer id;
    private User user;
    private String reportName;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal cpuAvg;
    private BigDecimal ramAvg;
    private BigDecimal idleTimeTotalSeconds;
    private BigDecimal browserUsagePercent;
    private String filePath;
    private LocalDateTime createdAt;

    public Report() {
        this.createdAt = LocalDateTime.now();
    }

    public static Report generate(User user, String reportName, LocalDate start, LocalDate end,
                                  List<SystemStats> stats, List<IdleTime> idleTimes) {

        BigDecimal cpuAvg = calculateAverage(stats, SystemStats::getCpuLoad);
        BigDecimal ramAvg = calculateAverage(stats, SystemStats::getMemoryUsageMb);
        BigDecimal idleSeconds = calculateTotalIdleTime(idleTimes);
        BigDecimal browserPercent = calculateBrowserUsage(stats);

        Report report = new Report();
        report.setUser(user);
        report.setReportName(reportName);
        report.setPeriodStart(start);
        report.setPeriodEnd(end);
        report.setCpuAvg(cpuAvg);
        report.setRamAvg(ramAvg);
        report.setIdleTimeTotalSeconds(idleSeconds);
        report.setBrowserUsagePercent(browserPercent);

        return report;
    }

    private static BigDecimal calculateAverage(List<SystemStats> stats, Function<SystemStats, BigDecimal> mapper) {
        if (stats == null || stats.isEmpty()) return BigDecimal.ZERO;
        return stats.stream()
                .map(mapper)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(stats.size()), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal calculateTotalIdleTime(List<IdleTime> idleTimes) {
        if (idleTimes == null || idleTimes.isEmpty()) return BigDecimal.ZERO;
        long totalSeconds = idleTimes.stream()
                .map(IdleTime::getDurationSeconds)
                .filter(Objects::nonNull)
                .mapToLong(Integer::longValue)
                .sum();
        return new BigDecimal(totalSeconds);
    }

    private static BigDecimal calculateBrowserUsage(List<SystemStats> stats) {
        if (stats == null || stats.isEmpty()) return BigDecimal.ZERO;
        long total = stats.size();
        long browser = stats.stream()
                .map(SystemStats::getActiveWindow)
                .filter(Objects::nonNull)
                .filter(w -> w.toLowerCase().matches(".*(chrome|firefox|edge|opera).*"))
                .count();
        return total == 0 ? BigDecimal.ZERO :
                BigDecimal.valueOf(browser).multiply(new BigDecimal("100"))
                        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getReportName() { return reportName; }
    public void setReportName(String reportName) { this.reportName = reportName; }
    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }
    public BigDecimal getCpuAvg() { return cpuAvg; }
    public void setCpuAvg(BigDecimal cpuAvg) { this.cpuAvg = cpuAvg; }
    public BigDecimal getRamAvg() { return ramAvg; }
    public void setRamAvg(BigDecimal ramAvg) { this.ramAvg = ramAvg; }
    public BigDecimal getIdleTimeTotalSeconds() { return idleTimeTotalSeconds; }
    public void setIdleTimeTotalSeconds(BigDecimal idleTimeTotalSeconds) { this.idleTimeTotalSeconds = idleTimeTotalSeconds; }
    public BigDecimal getBrowserUsagePercent() { return browserUsagePercent; }
    public void setBrowserUsagePercent(BigDecimal browserUsagePercent) { this.browserUsagePercent = browserUsagePercent; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
