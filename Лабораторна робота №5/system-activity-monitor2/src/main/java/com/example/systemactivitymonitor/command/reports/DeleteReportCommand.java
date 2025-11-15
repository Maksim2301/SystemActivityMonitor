package com.example.systemactivitymonitor.command.reports;

import com.example.systemactivitymonitor.model.Report;
import com.example.systemactivitymonitor.service.ReportService;

/**
 * 🗑 Команда видалення звіту.
 * Підтримує Undo — відновлення видаленого звіту.
 */
public class DeleteReportCommand implements ReportCommand {

    private final ReportService receiver;
    private final Integer reportId;
    private Report deletedReport;

    public DeleteReportCommand(ReportService receiver, Integer reportId) {
        this.receiver = receiver;
        this.reportId = reportId;
    }

    @Override
    public void execute() {
        deletedReport = receiver.findById(reportId);
        if (deletedReport != null) {
            receiver.deleteReport(reportId);
            System.out.println("🗑 [COMMAND] Видалено звіт: " + deletedReport.getReportName());
        } else {
            System.out.println("⚠️ [COMMAND] Звіт не знайдено для видалення (ID=" + reportId + ").");
        }
    }

    @Override
    public void undo() {
        if (deletedReport != null) {
            receiver.restoreReport(deletedReport);
            System.out.println("↩️ [UNDO] Відновлено звіт \"" + deletedReport.getReportName() + "\" після видалення.");
        } else {
            System.out.println("⚠️ [UNDO] Немає збереженого звіту для відновлення.");
        }
    }
}
