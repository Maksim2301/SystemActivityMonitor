package com.example.systemactivitymonitor.command;

import com.example.systemactivitymonitor.model.Report;
import com.example.systemactivitymonitor.service.ReportService;

public class DeleteReportCommand extends ReportCommand {

    private final ReportService receiver;
    private final Integer reportId;
    private Report deletedReport;

    public DeleteReportCommand(ReportService receiver, Integer reportId) {
        this.receiver = receiver;
        this.reportId = reportId;
    }

    @Override
    public boolean execute() {
        deletedReport = receiver.findById(reportId);
        if (deletedReport == null) return false;

        receiver.deleteReport(reportId);
        return true;
    }

    @Override
    public void undo() {
        if (deletedReport != null) {
            receiver.restoreReport(deletedReport);
        }
    }
}
