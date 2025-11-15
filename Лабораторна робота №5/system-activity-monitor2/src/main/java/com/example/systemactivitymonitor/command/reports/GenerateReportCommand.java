package com.example.systemactivitymonitor.command.reports;

import com.example.systemactivitymonitor.model.Report;
import com.example.systemactivitymonitor.model.User;
import com.example.systemactivitymonitor.service.ReportService;

import java.time.LocalDate;

/**
 * 🧾 Команда створення звіту.
 * Підтримує Undo — видалення створеного звіту.
 */
public class GenerateReportCommand implements ReportCommand {

    private final ReportService receiver;
    private final User user;
    private final String reportName;
    private final LocalDate start;
    private final LocalDate end;
    private Report createdReport;

    public GenerateReportCommand(ReportService receiver, User user, String reportName, LocalDate start, LocalDate end) {
        this.receiver = receiver;
        this.user = user;
        this.reportName = reportName;
        this.start = start;
        this.end = end;
    }

    @Override
    public void execute() {
        createdReport = receiver.generateReport(user, reportName, start, end);
        System.out.println("✅ [COMMAND] Створено звіт: " + createdReport.getReportName());
    }

    @Override
    public void undo() {
        if (createdReport != null && createdReport.getId() != null) {
            receiver.deleteReport(createdReport.getId());
            System.out.println("↩️ [UNDO] Створення звіту \"" + createdReport.getReportName() + "\" скасовано (видалено).");
        } else {
            System.out.println("⚠️ [UNDO] Немає створеного звіту для скасування.");
        }
    }
}
