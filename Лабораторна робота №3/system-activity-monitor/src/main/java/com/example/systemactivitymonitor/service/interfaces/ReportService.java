package com.example.systemactivitymonitor.service.interfaces;

import com.example.systemactivitymonitor.model.Report;
import com.example.systemactivitymonitor.model.User;
import java.time.LocalDate;
import java.util.List;

public interface ReportService {

    Report generateReport(User user, String reportName, LocalDate startDate, LocalDate endDate);

    List<Report> getReportsByUser(User user);

    List<Report> getReportsInPeriod(User user, LocalDate startDate, LocalDate endDate);

    void deleteReport(Integer reportId);
}
