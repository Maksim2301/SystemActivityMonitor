package com.example.systemactivitymonitor.metrics.impl;

import com.example.systemactivitymonitor.metrics.MetricsProvider;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 🐧 LinuxMetricsProvider — легка, безпечна реалізація MetricsProvider для Linux.
 * Використовує /proc для системних метрик і xprop для активного вікна.
 */
public class LinuxMetricsProvider implements MetricsProvider {

    // ===== CPU state =====
    private long lastIdle = 0;
    private long lastTotal = 0;

    // ===== Disk state =====
    private BigDecimal totalDiskGb = BigDecimal.ZERO;
    private BigDecimal freeDiskGb = BigDecimal.ZERO;
    private String disksDetails = "Unknown";

    // ===== Input counters =====
    private final AtomicInteger keyPressCount = new AtomicInteger(0);
    private final AtomicInteger mouseClickCount = new AtomicInteger(0);
    private final AtomicLong mouseMoveCount = new AtomicLong(0);
    private volatile Instant lastActivity = Instant.now();
    private Thread inputMonitorThread;
    private volatile boolean monitoringInput = false;

    @Override
    public BigDecimal getCpuLoad() {
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/stat"))) {
            String line = reader.readLine();
            if (line == null || !line.startsWith("cpu")) return BigDecimal.ZERO;

            String[] tokens = line.trim().split("\\s+");
            long user = Long.parseLong(tokens[1]);
            long nice = Long.parseLong(tokens[2]);
            long system = Long.parseLong(tokens[3]);
            long idle = Long.parseLong(tokens[4]);
            long iowait = tokens.length > 5 ? Long.parseLong(tokens[5]) : 0;
            long irq = tokens.length > 6 ? Long.parseLong(tokens[6]) : 0;
            long softirq = tokens.length > 7 ? Long.parseLong(tokens[7]) : 0;

            long total = user + nice + system + idle + iowait + irq + softirq;

            if (lastTotal == 0) {
                lastTotal = total;
                lastIdle = idle + iowait;
                return BigDecimal.ZERO;
            }

            long totalDiff = total - lastTotal;
            long idleDiff = (idle + iowait) - lastIdle;

            lastTotal = total;
            lastIdle = idle + iowait;

            if (totalDiff == 0) return BigDecimal.ZERO;
            double usage = (double) (totalDiff - idleDiff) / totalDiff * 100.0;
            return BigDecimal.valueOf(usage).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    @Override
    public BigDecimal getMemoryUsage() {
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/meminfo"))) {
            long total = 0, available = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("MemTotal")) {
                    total = Long.parseLong(line.replaceAll("\\D+", ""));
                } else if (line.startsWith("MemAvailable")) {
                    available = Long.parseLong(line.replaceAll("\\D+", ""));
                    break;
                }
            }
            if (total == 0) return BigDecimal.ZERO;

            double usedMb = (total - available) / 1024.0; // MB
            return BigDecimal.valueOf(usedMb).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    @Override
    public void updateDiskStats() {
        try {
            ProcessBuilder pb = new ProcessBuilder("df", "-B1", "/");
            Process p = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                reader.readLine(); // skip header
                String line = reader.readLine();
                if (line == null) return;

                String[] parts = line.trim().split("\\s+");
                if (parts.length < 4) return;

                double totalGb = Double.parseDouble(parts[1]) / 1e9;
                double usedGb = Double.parseDouble(parts[2]) / 1e9;
                double freeGb = Double.parseDouble(parts[3]) / 1e9;

                totalDiskGb = BigDecimal.valueOf(totalGb).setScale(2, RoundingMode.HALF_UP);
                freeDiskGb = BigDecimal.valueOf(freeGb).setScale(2, RoundingMode.HALF_UP);
                disksDetails = String.format("/root: %.2f / %.2f GB", usedGb, totalGb);
            }
        } catch (Exception e) {
            totalDiskGb = BigDecimal.ZERO;
            freeDiskGb = BigDecimal.ZERO;
            disksDetails = "Unknown";
        }
    }

    @Override
    public String getActiveWindowTitle() {
        try {
            ProcessBuilder pb = new ProcessBuilder("bash", "-c",
                    "xprop -id $(xprop -root _NET_ACTIVE_WINDOW | awk '{print $5}') WM_NAME | cut -d '\"' -f2");
            Process p = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line = reader.readLine();
                if (line == null || line.isBlank()) return "Unknown";
                return line.length() > 120 ? line.substring(0, 120) : line;
            }
        } catch (Exception e) {
            return "Unknown";
        }
    }

    @Override
    public String getUptime() {
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/uptime"))) {
            String[] parts = reader.readLine().split(" ");
            long uptimeSec = (long) Double.parseDouble(parts[0]);
            long days = uptimeSec / 86400;
            long hours = (uptimeSec % 86400) / 3600;
            long minutes = (uptimeSec % 3600) / 60;
            return String.format("%d d %d h %d m", days, hours, minutes);
        } catch (Exception e) {
            return "Unknown";
        }
    }

    @Override
    public void updateInputActivity() {
        if (monitoringInput) return; // вже запущено

        monitoringInput = true;
        inputMonitorThread = new Thread(() -> {
            try {
                java.io.File inputDir = new java.io.File("/dev/input");
                if (!inputDir.exists() || !inputDir.isDirectory()) {
                    System.err.println("⚠️ /dev/input не знайдено — активність не відстежується.");
                    monitoringInput = false;
                    return;
                }

                java.io.File[] devices = inputDir.listFiles((dir, name) -> name.startsWith("event"));
                if (devices == null || devices.length == 0) {
                    System.err.println("⚠️ Не знайдено жодного /dev/input/event* пристрою.");
                    monitoringInput = false;
                    return;
                }

                System.out.println("✅ Моніторинг пристроїв введення запущено (" + devices.length + " пристроїв)");

                // Відкриваємо всі пристрої у фоновому режимі
                java.util.List<java.io.RandomAccessFile> openDevices = new java.util.ArrayList<>();
                for (java.io.File dev : devices) {
                    try {
                        java.io.RandomAccessFile file = new java.io.RandomAccessFile(dev, "r");
                        openDevices.add(file);
                    } catch (Exception e) {
                        // Деякі /dev/input/event можуть бути недоступні — ігноруємо
                    }
                }

                // Основний цикл моніторингу
                byte[] buffer = new byte[24];
                while (monitoringInput) {
                    for (java.io.RandomAccessFile file : openDevices) {
                        try {
                            if (file.getChannel().size() > 0) { // читаємо, якщо є дані
                                int read = file.read(buffer);
                                if (read == 24) {
                                    int type = ((buffer[17] & 0xFF) << 8) | (buffer[16] & 0xFF);
                                    int code = ((buffer[19] & 0xFF) << 8) | (buffer[18] & 0xFF);
                                    int value = (buffer[20] & 0xFF)
                                            | ((buffer[21] & 0xFF) << 8)
                                            | ((buffer[22] & 0xFF) << 16)
                                            | ((buffer[23] & 0xFF) << 24);

                                    // Аналізуємо тип події
                                    if (type == 1 && value == 1) { // EV_KEY (натискання)
                                        keyPressCount.incrementAndGet();
                                        lastActivity = java.time.Instant.now();
                                    } else if (type == 2) { // EV_REL (рух миші)
                                        mouseMoveCount.incrementAndGet();
                                        lastActivity = java.time.Instant.now();
                                    } else if (type == 1 && code >= 0x110 && code <= 0x11F) { // кнопки миші
                                        mouseClickCount.incrementAndGet();
                                        lastActivity = java.time.Instant.now();
                                    }
                                }
                            }
                        } catch (Exception ignore) {
                            // якщо пристрій тимчасово недоступний — пропускаємо
                        }
                    }

                    Thread.sleep(50); // невелика пауза для економії CPU
                }

                // Закриття файлів
                for (java.io.RandomAccessFile f : openDevices) {
                    try { f.close(); } catch (Exception ignored) {}
                }

                System.out.println("🧹 Моніторинг /dev/input зупинено");

            } catch (Exception e) {
                System.err.println("⚠️ Помилка моніторингу введення: " + e.getMessage());
                monitoringInput = false;
            }
        }, "LinuxInputMonitor");

        inputMonitorThread.setDaemon(true);
        inputMonitorThread.start();
    }


    @Override
    public Map<String, Long> getInputStats() {
        Map<String, Long> input = new HashMap<>();
        input.put("keys", (long) keyPressCount.get());
        input.put("clicks", (long) mouseClickCount.get());
        input.put("moves", mouseMoveCount.get());
        input.put("lastActivitySecAgo", Instant.now().getEpochSecond() - lastActivity.getEpochSecond());
        return input;
    }

    @Override
    public Map<String, Object> collectAllMetrics() {
        updateDiskStats();
        Map<String, Object> data = new HashMap<>();
        data.put("cpu", getCpuLoad());
        data.put("ram", getMemoryUsage());
        data.put("window", getActiveWindowTitle());
        data.put("osName", "Linux");
        data.put("uptime", getUptime());
        data.put("diskUsed", totalDiskGb.subtract(freeDiskGb));
        data.put("diskTotal", totalDiskGb);
        data.put("diskDetails", disksDetails);
        data.putAll(getInputStats());
        return data;
    }
}

