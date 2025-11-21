package com.example.systemactivitymonitor.service.export;

import com.example.systemactivitymonitor.model.Report;
import com.example.systemactivitymonitor.util.FontResolver;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

import java.io.FileOutputStream;
import java.nio.file.*;

public class PdfReportExporter implements ReportExporter {

    private static final Path DOWNLOAD_DIR =
            Paths.get(System.getProperty("user.home"), "Downloads");

    @Override
    public Path export(Report report) throws Exception {
        Files.createDirectories(DOWNLOAD_DIR);

        Path path = DOWNLOAD_DIR.resolve(report.getReportName() + ".pdf");

        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(path.toFile()));
        document.open();

        BaseFont bf = FontResolver.resolveCyrillicFont();
        Font font = new Font(bf, 12);
        Font bold = new Font(bf, 16, Font.BOLD);

        document.add(new Paragraph("Звіт: " + report.getReportName(), bold));
        document.add(new Paragraph("Період: " + report.getPeriodStart() + " - " + report.getPeriodEnd(), font));
        document.add(new Paragraph("CPU Avg: " + report.getCpuAvg() + "%", font));
        document.add(new Paragraph("RAM Avg: " + report.getRamAvg() + " MB", font));
        document.add(new Paragraph("Idle Time: " + report.getIdleTimeTotalSeconds() + " сек.", font));
        document.add(new Paragraph("Avg Uptime: " + report.getAvgUptimeHours() + " год/день", font));

        document.add(new Paragraph("\n", font));

        PdfPTable table = new PdfPTable(2);
        table.addCell(new Paragraph("Програма", font));
        table.addCell(new Paragraph("Відсоток часу", font));

        report.getAppUsagePercent().forEach((app, value) -> {
            table.addCell(new Paragraph(app, font));
            table.addCell(new Paragraph(value + "%", font));
        });

        document.add(table);
        document.close();

        return path;
    }

    @Override
    public String getExtension() {
        return "pdf";
    }
}
