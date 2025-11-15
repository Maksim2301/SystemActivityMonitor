package com.example.systemactivitymonitor.command.reports;

import com.example.systemactivitymonitor.model.Report;
import com.example.systemactivitymonitor.service.ReportService;

import java.nio.file.Path;

/**
 * 📤 Команда експорту звіту.
 * Підтримує Undo — видалення створеного файлу.
 */
public class ExportReportCommand implements ReportCommand {

    private final ReportService receiver;
    private final Report report;
    private final String format;
    private Path exportedPath;

    public ExportReportCommand(ReportService receiver, Report report, String format) {
        this.receiver = receiver;
        this.report = report;
        this.format = format;
    }

    @Override
    public void execute() {
        try {
            exportedPath = receiver.exportReport(report, format);
            if (exportedPath != null) {
                System.out.println("📤 [COMMAND] Звіт \"" + report.getReportName() +
                        "\" експортовано у форматі " + format.toUpperCase());
            } else {
                System.out.println("⚠️ [COMMAND] Не вдалося експортувати звіт.");
            }
        } catch (Exception e) {
            System.err.println("❌ [COMMAND] Помилка при експорті звіту: " + e.getMessage());
        }
    }

    @Override
    public void undo() {
        if (exportedPath != null) {
            receiver.deleteExportedFile(exportedPath);
            System.out.println("🗑️ [UNDO] Видалено експортований файл \"" + exportedPath + "\".");
        } else {
            System.out.println("⚠️ [UNDO] Немає файлу для видалення.");
        }
    }
}
