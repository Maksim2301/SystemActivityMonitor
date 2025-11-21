package com.example.systemactivitymonitor.command;

import com.example.systemactivitymonitor.model.Report;
import com.example.systemactivitymonitor.model.User;
import com.example.systemactivitymonitor.service.ReportService;

import java.time.LocalDate;

public class GenerateReportCommand extends ReportCommand {

    private final ReportService receiver;
    private final User user;
    private final String name;
    private final LocalDate start;
    private final LocalDate end;
    private Report created;

    public GenerateReportCommand(ReportService receiver, User user, String name,
                                 LocalDate start, LocalDate end) {
        this.receiver = receiver;
        this.user = user;
        this.name = name;
        this.start = start;
        this.end = end;
    }

    @Override
    public boolean execute() {
        created = receiver.generateReport(user, name, start, end);
        return created != null && created.getId() != null;
    }

    @Override
    public void undo() {
        if (created != null) {
            receiver.deleteReport(created.getId());
        }
    }
}
