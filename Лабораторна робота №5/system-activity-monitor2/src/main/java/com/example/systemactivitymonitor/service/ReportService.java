package com.example.systemactivitymonitor.service;

import com.example.systemactivitymonitor.iterator.Iterator;
import com.example.systemactivitymonitor.iterator.ReportAggregate;
import com.example.systemactivitymonitor.model.*;
import com.example.systemactivitymonitor.repository.impl.*;
import com.example.systemactivitymonitor.repository.interfaces.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.io.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.*;
/**
 * ✅ ReportService — фасад для роботи зі звітами.
 * Використовується як Receiver у шаблоні Command.
 */
public class ReportService {

    private final ReportRepository reportRepository = new ReportRepositoryImpl();
    private final StatsRepository statsRepository = new StatsRepositoryImpl();
    private final IdleRepository idleRepository = new IdleRepositoryImpl();

    private static final String EXPORT_DIR = "exports";

    // ============================================================
    // 🧾 Операції над звітами
    // ============================================================

    public Report generateReport(User user, String reportName, LocalDate startDate, LocalDate endDate) {
        validateUser(user);
        validatePeriod(startDate, endDate);

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        // Отримуємо статистику
        List<SystemStats> stats = statsRepository.findByUserIdAndRecordedAtBetween(user.getId(), start, end);
        List<IdleTime> idleTimes = idleRepository.findByUserIdAndStartTimeBetween(user.getId(), start, end);

        if (stats.isEmpty()) {
            System.out.println("⚠️ Немає даних статистики за цей період.");
        }

        // Обчислення основних показників
        BigDecimal cpuAvg = calculateAverage(stats, SystemStats::getCpuLoad);
        BigDecimal ramAvg = calculateAverage(stats, SystemStats::getMemoryUsageMb);
        BigDecimal idleSeconds = calculateTotalIdleTime(idleTimes);
        BigDecimal avgUptime = calculateAverageUptimeByDay(stats);

        Map<String, BigDecimal> appUsagePercent = calculateAppUsagePercent(stats);
        List<DaySummary> days = buildDaySummary(stats);

        // Формування звіту
        Report report = new Report();
        report.setUser(user);
        report.setReportName(reportName);
        report.setPeriodStart(startDate);
        report.setPeriodEnd(endDate);
        report.setCpuAvg(cpuAvg);
        report.setRamAvg(ramAvg);
        report.setIdleTimeTotalSeconds(idleSeconds);
        report.setAppUsagePercent(appUsagePercent);
        report.setDays(days);

        // ⚙️ середній аптайм записуємо у file_path (для простоти)
        report.setFilePath("Середній аптайм: " + avgUptime + " год/день");

        // Збереження в базі
        reportRepository.save(report);
        System.out.println("✅ Звіт \"" + reportName + "\" створено для користувача " + user.getUsername());

        return report;
    }

    public void deleteReport(Integer reportId) {
        if (reportId == null)
            throw new IllegalArgumentException("ID звіту не може бути null.");
        reportRepository.deleteById(reportId);
        System.out.println("🗑 Звіт з ID=" + reportId + " видалено.");
    }

    public void restoreReport(Report report) {
        if (report != null) {
            reportRepository.save(report);
            System.out.println("↩️ Звіт \"" + report.getReportName() + "\" відновлено.");
        }
    }

    public Report findById(Integer id) {
        return reportRepository.findById(id).orElse(null);
    }

    // ============================================================
    // 📄 Експорт звітів
    // ============================================================

    public Path exportReport(Report report, String format) {
        try {
            Files.createDirectories(Paths.get(EXPORT_DIR));

            return switch (format.toLowerCase()) {
                case "csv" -> exportToCSV(report);
                case "excel" -> exportToExcel(report);
                case "pdf" -> exportToPDF(report);
                default -> throw new IllegalArgumentException("Невідомий формат: " + format);
            };
        } catch (Exception e) {
            System.err.println("❌ Помилка при експорті звіту: " + e.getMessage());
            return null;
        }
    }

    private Path exportToCSV(Report report) throws IOException {
        Path path = Paths.get(EXPORT_DIR, report.getReportName() + ".csv");
        try (FileWriter writer = new FileWriter(path.toFile())) {
            writer.write("Report Name," + report.getReportName() + "\n");
            writer.write("Period," + report.getPeriodStart() + " - " + report.getPeriodEnd() + "\n");
            writer.write("CPU Avg (%)," + report.getCpuAvg() + "\n");
            writer.write("RAM Avg (MB)," + report.getRamAvg() + "\n");
            writer.write("Idle Time (sec)," + report.getIdleTimeTotalSeconds() + "\n");
            writer.write(report.getFilePath() + "\n\n");

            writer.write("Application,Usage (%)\n");
            for (var e : report.getAppUsagePercent().entrySet()) {
                writer.write(e.getKey() + "," + e.getValue() + "\n");
            }
        }
        System.out.println("📤 CSV звіт збережено: " + path);
        return path;
    }

    private Path exportToExcel(Report report) throws IOException {
        Path path = Paths.get(EXPORT_DIR, report.getReportName() + ".xlsx");
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Report");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Показник");
            header.createCell(1).setCellValue("Значення");

            sheet.createRow(1).createCell(0).setCellValue("CPU Avg (%)");
            sheet.getRow(1).createCell(1).setCellValue(report.getCpuAvg().doubleValue());

            sheet.createRow(2).createCell(0).setCellValue("RAM Avg (MB)");
            sheet.getRow(2).createCell(1).setCellValue(report.getRamAvg().doubleValue());

            sheet.createRow(3).createCell(0).setCellValue("Idle Time (sec)");
            sheet.getRow(3).createCell(1).setCellValue(report.getIdleTimeTotalSeconds().doubleValue());

            sheet.createRow(4).createCell(0).setCellValue("Avg Uptime");
            sheet.getRow(4).createCell(1).setCellValue(report.getFilePath());

            int row = 6;
            sheet.createRow(row++).createCell(0).setCellValue("Application Usage:");
            for (var entry : report.getAppUsagePercent().entrySet()) {
                Row r = sheet.createRow(row++);
                r.createCell(0).setCellValue(entry.getKey());
                r.createCell(1).setCellValue(entry.getValue().doubleValue());
            }

            try (FileOutputStream out = new FileOutputStream(path.toFile())) {
                workbook.write(out);
            }
        }
        System.out.println("📘 Excel звіт збережено: " + path);
        return path;
    }

    private Path exportToPDF(Report report) throws Exception {
        Path path = Paths.get(EXPORT_DIR, report.getReportName() + ".pdf");
        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(path.toFile()));
        document.open();

        document.add(new Paragraph("Звіт: " + report.getReportName(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
        document.add(new Paragraph("Період: " + report.getPeriodStart() + " - " + report.getPeriodEnd()));
        document.add(new Paragraph("\nCPU Avg: " + report.getCpuAvg() + "%"));
        document.add(new Paragraph("RAM Avg: " + report.getRamAvg() + " MB"));
        document.add(new Paragraph("Idle Time: " + report.getIdleTimeTotalSeconds() + " сек."));
        document.add(new Paragraph(report.getFilePath()));
        document.add(new Paragraph("\n"));

        PdfPTable table = new PdfPTable(2);
        table.addCell("Програма");
        table.addCell("Відсоток часу");
        for (var e : report.getAppUsagePercent().entrySet()) {
            table.addCell(e.getKey());
            table.addCell(e.getValue() + "%");
        }
        document.add(table);
        document.close();

        System.out.println("📄 PDF звіт збережено: " + path);
        return path;
    }

    public void deleteExportedFile(Path path) {
        try {
            Files.deleteIfExists(path);
            System.out.println("🗑️ Видалено файл експорту: " + path);
        } catch (IOException e) {
            System.err.println("⚠️ Не вдалося видалити файл: " + e.getMessage());
        }
    }

    // ============================================================
    // 📊 Обчислення
    // ============================================================

    private static BigDecimal calculateAverage(List<SystemStats> stats, Function<SystemStats, BigDecimal> mapper) {
        if (stats == null || stats.isEmpty()) return BigDecimal.ZERO;
        return stats.stream()
                .map(mapper)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(stats.size()), 2, java.math.RoundingMode.HALF_UP);
    }

    private static BigDecimal calculateTotalIdleTime(List<IdleTime> idleTimes) {
        if (idleTimes == null || idleTimes.isEmpty()) return BigDecimal.ZERO;
        return BigDecimal.valueOf(idleTimes.stream()
                .map(IdleTime::getDurationSeconds)
                .filter(Objects::nonNull)
                .mapToLong(Integer::longValue)
                .sum());
    }

    private static Map<String, BigDecimal> calculateAppUsagePercent(List<SystemStats> stats) {
        if (stats == null || stats.isEmpty()) return Map.of();
        long total = stats.size();
        Map<String, Long> counts = stats.stream()
                .map(SystemStats::getActiveWindow)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(ReportService::normalizeAppName, LinkedHashMap::new, Collectors.counting()));
        return counts.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> BigDecimal.valueOf(e.getValue() * 100.0 / total).setScale(2, java.math.RoundingMode.HALF_UP),
                (a, b) -> a, LinkedHashMap::new));
    }

    private List<DaySummary> buildDaySummary(List<SystemStats> stats) {
        if (stats == null || stats.isEmpty()) return List.of();
        Map<LocalDate, Map<Integer, List<SystemStats>>> grouped = stats.stream()
                .filter(s -> s.getRecordedAt() != null)
                .collect(Collectors.groupingBy(
                        s -> s.getRecordedAt().toLocalDate(),
                        Collectors.groupingBy(s -> s.getRecordedAt().getHour())
                ));
        return grouped.entrySet().stream()
                .map(e -> new DaySummary(e.getKey(),
                        e.getValue().entrySet().stream()
                                .map(hour -> new HourStat(hour.getKey(),
                                        calculateAverage(hour.getValue(), SystemStats::getCpuLoad),
                                        calculateAverage(hour.getValue(), SystemStats::getMemoryUsageMb)))
                                .toList()))
                .toList();
    }

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
        if (lower.contains("idea")) return "IntelliJ IDEA";
        if (lower.contains("studio")) return "Android Studio";
        return title.length() > 40 ? title.substring(0, 40) + "..." : title;
    }

    private BigDecimal calculateAverageUptimeByDay(List<SystemStats> stats) {
        if (stats == null || stats.isEmpty()) return BigDecimal.ZERO;
        Map<LocalDate, List<SystemStats>> grouped = stats.stream()
                .filter(s -> s.getRecordedAt() != null)
                .collect(Collectors.groupingBy(s -> s.getRecordedAt().toLocalDate()));
        return grouped.values().stream()
                .map(day -> BigDecimal.valueOf(day.stream()
                        .mapToLong(s -> Optional.ofNullable(s.getSystemUptimeSeconds()).orElse(0L))
                        .max().orElse(0L) / 3600.0))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(grouped.size()), 2, java.math.RoundingMode.HALF_UP);
    }

    public List<Report> getReportsByUser(User user) {
        validateUser(user);
        LocalDateTime start = LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.now();
        return reportRepository.findByUserIdAndCreatedAtBetween(user.getId(), start, end);
    }

    /**
     * Повертає звіти користувача лише за вибраний період.
     */
    public List<Report> getReportsInPeriod(User user, LocalDate startDate, LocalDate endDate) {
        validateUser(user);
        validatePeriod(startDate, endDate);

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        return reportRepository.findByUserIdAndCreatedAtBetween(user.getId(), start, end)
                .stream()
                .filter(r -> r.getPeriodStart() != null && r.getPeriodEnd() != null &&
                        !r.getPeriodStart().isBefore(startDate) &&
                        !r.getPeriodEnd().isAfter(endDate))
                .toList();
    }

    /**
     * Генерує текстовий CPU/RAM звіт по годинах для візуалізації.
     */
    public String getCpuAndRamReport(User user, Report report) {
        if (report == null) return "Немає звіту.";
        if (user == null || user.getId() == null) return "Користувач не визначений.";
        if (report.getDays() == null || report.getDays().isEmpty()) return "Немає даних для звіту.";

        ReportAggregate aggregate = new ReportAggregate(report);
        Iterator<HourStat> it = aggregate.createIterator();

        StringBuilder sb = new StringBuilder("CPU/RAM по годинах:\n");
        for (it.first(); !it.isDone(); it.next()) {
            HourStat stat = it.currentItem();
            sb.append(String.format("Година %02d → CPU: %.2f%% | RAM: %.2f MB%n",
                    stat.getHour(),
                    stat.getAvgCpu(),
                    stat.getAvgRam()));
        }
        return sb.toString();
    }

    // ============================================================
    // 🧩 Перевірки
    // ============================================================

    private void validateUser(User user) {
        if (user == null || user.getId() == null)
            throw new IllegalArgumentException("Користувач не заданий або не збережений (id == null).");
    }

    private void validatePeriod(LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start))
            throw new IllegalArgumentException("Некоректний період.");
    }
}
