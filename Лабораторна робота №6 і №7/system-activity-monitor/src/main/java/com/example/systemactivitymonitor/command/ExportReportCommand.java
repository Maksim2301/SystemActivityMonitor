package com.example.systemactivitymonitor.command;

import com.example.systemactivitymonitor.model.Report;
import com.example.systemactivitymonitor.service.ReportService;

import java.nio.file.Path;

public class ExportReportCommand extends ReportCommand {

    private final ReportService receiver;
    private final Report report;
    private final String format;
    private Path exportedFile;

    public ExportReportCommand(ReportService receiver, Report report, String format) {
        this.receiver = receiver;
        this.report = report;
        this.format = format;
    }

    @Override
    public boolean execute() {
        try {
            exportedFile = receiver.export(report, format);
            return exportedFile != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void undo() {
        if (exportedFile != null) {
            receiver.deleteExportedFile(exportedFile);
        }
    }
}
