package com.example.systemactivitymonitor.service.export;

import com.example.systemactivitymonitor.model.Report;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;

public class CsvReportExporter implements ReportExporter {

    private static final Path EXPORT_DIR = Paths.get("exports");

    @Override
    public Path export(Report report) throws IOException {
        Files.createDirectories(EXPORT_DIR);

        Path path = EXPORT_DIR.resolve(report.getReportName() + ".csv");

        try (FileWriter writer = new FileWriter(path.toFile())) {
            writer.write("Report Name," + report.getReportName() + "\n");
            writer.write("Period," + report.getPeriodStart() + " - " + report.getPeriodEnd() + "\n");
            writer.write("CPU Avg (%)," + report.getCpuAvg() + "\n");
            writer.write("RAM Avg (MB)," + report.getRamAvg() + "\n");
            writer.write("Idle Time (sec)," + report.getIdleTimeTotalSeconds() + "\n");
            writer.write("Average Uptime," + report.getAvgUptimeHours() + " hours/day\n\n");

            writer.write("Application,Usage (%)\n");

            report.getAppUsagePercent().forEach((app, value) -> {
                try {
                    writer.write(app + "," + value + "\n");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        return path;
    }

    @Override
    public String getExtension() {
        return "csv";
    }
}
