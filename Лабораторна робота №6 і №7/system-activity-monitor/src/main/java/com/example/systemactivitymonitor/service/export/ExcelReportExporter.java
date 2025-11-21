package com.example.systemactivitymonitor.service.export;

import com.example.systemactivitymonitor.model.Report;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.nio.file.*;

public class ExcelReportExporter implements ReportExporter {

    private static final Path EXPORT_DIR = Paths.get("exports");

    @Override
    public Path export(Report report) throws Exception {
        Files.createDirectories(EXPORT_DIR);

        Path path = EXPORT_DIR.resolve(report.getReportName() + ".xlsx");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Report");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Metric");
            header.createCell(1).setCellValue("Value");

            sheet.createRow(1).createCell(0).setCellValue("CPU Avg (%)");
            sheet.getRow(1).createCell(1).setCellValue(report.getCpuAvg().doubleValue());

            sheet.createRow(2).createCell(0).setCellValue("RAM Avg (MB)");
            sheet.getRow(2).createCell(1).setCellValue(report.getRamAvg().doubleValue());

            sheet.createRow(3).createCell(0).setCellValue("Idle Time (sec)");
            sheet.getRow(3).createCell(1).setCellValue(report.getIdleTimeTotalSeconds().doubleValue());

            sheet.createRow(4).createCell(0).setCellValue("Average uptime (hours/day)");
            sheet.getRow(4).createCell(1).setCellValue(report.getAvgUptimeHours().doubleValue());

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

        return path;
    }

    @Override
    public String getExtension() {
        return "xlsx";
    }
}
