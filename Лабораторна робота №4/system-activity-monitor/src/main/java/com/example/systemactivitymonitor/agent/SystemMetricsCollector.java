package com.example.systemactivitymonitor.agent;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Клас для збору системних метрик у реальному часі (CPU, RAM, диски, активне вікно тощо).
 * Працює у власному потоці та може бути зупинений і перезапущений вручну.
 */
public class SystemMetricsCollector {

    private volatile BigDecimal cpuLoad = BigDecimal.ZERO;
    private volatile BigDecimal memoryUsageMb = BigDecimal.ZERO;
    private volatile String activeWindowTitle = "Unknown";

    private volatile BigDecimal totalDiskGb = BigDecimal.ZERO;
    private volatile BigDecimal freeDiskGb = BigDecimal.ZERO;
    private volatile String disksDetails = "Unknown";

    private final PowerShellSession psSession;
    private ScheduledExecutorService scheduler;
    private boolean active = false;

    public SystemMetricsCollector() {
        psSession = new PowerShellSession();
        System.out.println("SystemMetricsCollector створено (очікує запуск).");
    }

    public void start() {
        if (active) return;
        active = true;

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::updateMetrics, 0, 5, TimeUnit.SECONDS);
        System.out.println("SystemMetricsCollector: моніторинг запущено (оновлення кожні 5 секунд).");
    }

    public void stop() {
        active = false;
        if (scheduler != null && !scheduler.isShutdown()) scheduler.shutdownNow();
        psSession.close();
        System.out.println("SystemMetricsCollector: моніторинг зупинено.");
    }

    private void updateMetrics() {
        if (!active) return;
        try {
            cpuLoad = psSession.executeNumericWithDelay(
                    "(Get-CimInstance Win32_Processor | Measure-Object -Property LoadPercentage -Average).Average", 250);

            memoryUsageMb = psSession.executeNumeric(
                    "(Get-CimInstance Win32_OperatingSystem | " +
                            "Select-Object @{n='Used';e={[math]::Round(($_.TotalVisibleMemorySize - $_.FreePhysicalMemory)/1024,2)}}).Used");

            totalDiskGb = psSession.executeNumeric(
                    "(Get-CimInstance Win32_LogicalDisk -Filter \"DriveType=3\" | " +
                            "Measure-Object -Property Size -Sum | " +
                            "Select-Object @{n='Total';e={[math]::Round(($_.Sum)/1GB,2)}}).Total");

            freeDiskGb = psSession.executeNumeric(
                    "(Get-CimInstance Win32_LogicalDisk -Filter \"DriveType=3\" | " +
                            "Measure-Object -Property FreeSpace -Sum | " +
                            "Select-Object @{n='Free';e={[math]::Round(($_.Sum)/1GB,2)}}).Free");

            disksDetails = psSession.executeString(
                    "Get-CimInstance Win32_LogicalDisk -Filter \"DriveType=3\" | " +
                            "Select-Object DeviceID,FreeSpace,Size | " +
                            "ForEach-Object {" +
                            " $used=[math]::Round(($_.Size - $_.FreeSpace)/1GB,2);" +
                            " $total=[math]::Round($_.Size/1GB,2);" +
                            " \"$($_.DeviceID): $used / $total GB\"" +
                            "}"
            ).replace("\r", "").replace("\n", " | ");

            activeWindowTitle = readActiveWindow();

            System.out.printf("CPU: %s%% | RAM: %s MB | Disk: %.2f/%.2f GB | Window: %s%n",
                    cpuLoad, memoryUsageMb, totalDiskGb.subtract(freeDiskGb), totalDiskGb, activeWindowTitle);

        } catch (Exception e) {
            System.err.println("Помилка оновлення метрик: " + e.getMessage());
        }
    }

    private String readActiveWindow() {
        try {
            char[] buffer = new char[512];
            HWND hwnd = User32.INSTANCE.GetForegroundWindow();
            if (hwnd == null) return "Unknown";
            User32.INSTANCE.GetWindowText(hwnd, buffer, 512);
            String title = Native.toString(buffer).trim();
            if (title.isEmpty()) title = "Unknown";
            if (title.length() > 120) title = title.substring(0, 120);
            return title;
        } catch (Exception e) {
            return "Unknown";
        }
    }

    public String getUptime() {
        try {
            String cmd =
                    "$os = Get-CimInstance Win32_OperatingSystem;" +
                            "$uptime = (Get-Date) - $os.LastBootUpTime;" +
                            "$d = [math]::Floor($uptime.TotalDays);" +
                            "$h = $uptime.Hours;" +
                            "$m = $uptime.Minutes;" +
                            "Write-Output (\"$d d $h h $m m\")";
            String output = psSession.executeString(cmd).trim();
            return output.isBlank() ? "Unknown" : output;
        } catch (Exception e) {
            return "Unknown";
        }
    }

    public BigDecimal getCpuLoad() { return cpuLoad; }
    public BigDecimal getMemoryUsageMb() { return memoryUsageMb; }
    public BigDecimal getTotalDiskGb() { return totalDiskGb; }
    public BigDecimal getFreeDiskGb() { return freeDiskGb; }
    public String getDisksDetails() { return disksDetails; }
    public String getActiveWindowTitle() { return activeWindowTitle; }


    private static class PowerShellSession implements AutoCloseable {
        private final Process process;
        private final BufferedWriter writer;
        private final BufferedReader reader;
        private final Pattern numberPattern = Pattern.compile("(\\d+(?:\\.\\d+)?)");

        public PowerShellSession() {
            try {
                process = new ProcessBuilder("powershell.exe", "-NoLogo", "-NoProfile", "-Command", "-")
                        .redirectErrorStream(true)
                        .start();
                writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
                reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
                sendCommand("[Console]::OutputEncoding = [System.Text.Encoding]::UTF8");
                System.out.println("💡 PowerShellSession: постійний процес PowerShell запущено.");
            } catch (IOException e) {
                throw new RuntimeException("Не вдалося запустити PowerShell: " + e.getMessage(), e);
            }
        }

        private synchronized String sendCommand(String command) throws IOException {
            writer.write(command + "\n");
            writer.write("Write-Host '<END>'\n");
            writer.flush();
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equals("<END>")) break;
                if (!line.isBlank()) output.append(line).append("\n");
            }
            return output.toString().trim();
        }

        public BigDecimal executeNumeric(String command) { return executeNumericInternal(command, false, 0); }
        public BigDecimal executeNumericWithDelay(String command, int delayMs) { return executeNumericInternal(command, true, delayMs); }

        private BigDecimal executeNumericInternal(String command, boolean withDelay, int delayMs) {
            try {
                if (withDelay) Thread.sleep(delayMs);
                String output = sendCommand(command);
                Matcher m = numberPattern.matcher(output);
                if (m.find()) return new BigDecimal(m.group(1)).setScale(2, RoundingMode.HALF_UP);
            } catch (Exception ignored) {}
            return BigDecimal.ZERO;
        }

        public String executeString(String command) {
            try {
                String output = sendCommand(command);
                return output.isBlank() ? "Unknown" : output;
            } catch (Exception e) {
                return "Unknown";
            }
        }

        @Override
        public void close() {
            try {
                sendCommand("exit");
                process.destroyForcibly();
                writer.close();
                reader.close();
                System.out.println("PowerShellSession: завершено.");
            } catch (IOException ignored) {}
        }
    }
}
