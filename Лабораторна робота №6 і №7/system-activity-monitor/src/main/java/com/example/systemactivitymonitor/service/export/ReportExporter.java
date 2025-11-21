package com.example.systemactivitymonitor.service.export;

import com.example.systemactivitymonitor.model.Report;
import java.nio.file.Path;

public interface ReportExporter {
    Path export(Report report) throws Exception;
    String getExtension();
}
