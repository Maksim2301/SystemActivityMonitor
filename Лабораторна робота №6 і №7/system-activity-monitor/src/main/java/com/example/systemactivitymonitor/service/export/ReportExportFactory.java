package com.example.systemactivitymonitor.service.export;

public class ReportExportFactory {

    public static ReportExporter getExporter(String format) {
        if (format == null)
            throw new IllegalArgumentException("Format cannot be null");

        return switch (format.toLowerCase()) {
            case "csv" -> new CsvReportExporter();
            case "excel", "xlsx" -> new ExcelReportExporter();
            case "pdf" -> new PdfReportExporter();
            default -> throw new IllegalArgumentException("Unknown report format: " + format);
        };
    }
}
