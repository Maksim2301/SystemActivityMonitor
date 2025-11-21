package com.example.systemactivitymonitor.metrics.impl;

import com.example.systemactivitymonitor.metrics.MetricsProvider;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Linux реалізація MetricsProvider.
 * Працює через /proc та df.
 */
public class LinuxMetricsProvider implements MetricsProvider {

    // ------------------------ CPU ------------------------
    private long lastIdle = 0;
    private long lastTotal = 0;

    // ------------------------ Disk ------------------------
    private BigDecimal diskTotal = BigDecimal.ZERO;
    private BigDecimal diskFree = BigDecimal.ZERO;
    private String diskDetails = "Unknown";

    // ------------------------ RAM ------------------------
    private BigDecimal ramTotal = BigDecimal.ZERO;

    // ------------------------ Input monitoring ------------------------
    private final AtomicInteger keyPressCount = new AtomicInteger(0);
    private final AtomicInteger mouseClickCount = new AtomicInteger(0);
    private final AtomicLong mouseMoveCount = new AtomicLong(0);

    private volatile Instant lastActivity = Instant.now();
    private Thread inputThread;
    private volatile boolean inputActive = false;

    // ========================================================================
    // CPU
    // ========================================================================
    @Override
    public BigDecimal getCpuLoad() {
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/stat"))) {
            String line = reader.readLine();
            if (line == null || !line.startsWith("cpu")) return BigDecimal.ZERO;

            String[] t = line.split("\\s+");
            long user = Long.parseLong(t[1]);
            long nice = Long.parseLong(t[2]);
            long system = Long.parseLong(t[3]);
            long idle = Long.parseLong(t[4]);
            long iowait = t.length > 5 ? Long.parseLong(t[5]) : 0;
            long irq = t.length > 6 ? Long.parseLong(t[6]) : 0;
            long softirq = t.length > 7 ? Long.parseLong(t[7]) : 0;

            long total = user + nice + system + idle + iowait + irq + softirq;

            if (lastTotal == 0) {
                lastTotal = total;
                lastIdle = idle + iowait;
                return BigDecimal.ZERO;
            }

            long totalDiff = total - lastTotal;
            long idleDiff = (idle + iowait) - lastIdle;

            lastIdle = idle + iowait;
            lastTotal = total;

            if (totalDiff == 0) return BigDecimal.ZERO;

            double usage = (double) (totalDiff - idleDiff) / totalDiff * 100.0;
            return BigDecimal.valueOf(usage).setScale(2, RoundingMode.HALF_UP);

        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    // ========================================================================
    // RAM
    // ========================================================================
    @Override
    public BigDecimal getRamUsed() {
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/meminfo"))) {
            long total = 0, available = 0;
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("MemTotal")) {
                    total = Long.parseLong(line.replaceAll("\\D+", ""));
                } else if (line.startsWith("MemAvailable")) {
                    available = Long.parseLong(line.replaceAll("\\D+", ""));
                }
            }

            ramTotal = BigDecimal.valueOf(total / 1024.0).setScale(2, RoundingMode.HALF_UP);
            double usedMb = (total - available) / 1024.0;

            return BigDecimal.valueOf(usedMb).setScale(2, RoundingMode.HALF_UP);

        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    @Override
    public BigDecimal getRamTotal() {
        return ramTotal;
    }

    // ========================================================================
    // DISK
    // ========================================================================
    @Override
    public void updateDiskStats() {
        try {
            Process p = new ProcessBuilder("df", "-B1", "/").start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                reader.readLine(); // header
                String line = reader.readLine();
                if (line == null) return;

                String[] t = line.split("\\s+");
                double totalGb = Double.parseDouble(t[1]) / 1e9;
                double usedGb = Double.parseDouble(t[2]) / 1e9;
                double freeGb = Double.parseDouble(t[3]) / 1e9;

                diskTotal = BigDecimal.valueOf(totalGb).setScale(2, RoundingMode.HALF_UP);
                diskFree = BigDecimal.valueOf(freeGb).setScale(2, RoundingMode.HALF_UP);
                diskDetails = String.format("/root: %.2f / %.2f GB", usedGb, totalGb);
            }
        } catch (Exception e) {
            diskTotal = BigDecimal.ZERO;
            diskFree = BigDecimal.ZERO;
        }
    }

    @Override
    public BigDecimal getDiskTotal() {
        return diskTotal;
    }

    @Override
    public BigDecimal getDiskFree() {
        return diskFree;
    }

    @Override
    public BigDecimal getDiskUsed() {
        return diskTotal.subtract(diskFree);
    }

    // ========================================================================
    // ACTIVE WINDOW
    // ========================================================================
    @Override
    public String getActiveWindowTitle() {
        try {
            Process p = new ProcessBuilder(
                    "bash", "-c",
                    "xprop -id $(xprop -root _NET_ACTIVE_WINDOW | awk '{print $5}') WM_NAME | cut -d '\"' -f2"
            ).start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String name = br.readLine();
                if (name == null || name.isBlank()) return "Unknown";
                return name.length() > 120 ? name.substring(0, 120) : name;
            }
        } catch (Exception e) {
            return "Unknown";
        }
    }

    // ========================================================================
    // UPTIME
    // ========================================================================
    @Override
    public String getUptime() {
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/uptime"))) {
            long uptimeSec = (long) Double.parseDouble(reader.readLine().split(" ")[0]);

            long days = uptimeSec / 86400;
            long hours = (uptimeSec % 86400) / 3600;
            long minutes = (uptimeSec % 3600) / 60;

            return String.format("%d d %d h %d m", days, hours, minutes);
        } catch (Exception e) {
            return "Unknown";
        }
    }

    // ========================================================================
    // INPUT MONITORING
    // ========================================================================
    @Override
    public void startInputMonitoring() {
        if (inputActive) return;

        inputActive = true;

        inputThread = new Thread(() -> {
            try {
                java.io.File inputDir = new java.io.File("/dev/input");

                if (!inputDir.exists()) {
                    inputActive = false;
                    return;
                }

                java.io.File[] devices = inputDir.listFiles(
                        (d, name) -> name.startsWith("event"));
                if (devices == null || devices.length == 0) {
                    inputActive = false;
                    return;
                }

                byte[] buffer = new byte[24];
                List<java.io.RandomAccessFile> files = new ArrayList<>();

                for (java.io.File dev : devices) {
                    try {
                        files.add(new java.io.RandomAccessFile(dev, "r"));
                    } catch (Exception ignored) {}
                }

                while (inputActive) {
                    for (java.io.RandomAccessFile f : files) {
                        try {
                            if (f.getChannel().size() > 0) {
                                int read = f.read(buffer);
                                if (read == 24) {
                                    int type = (buffer[17] & 0xFF) << 8 | (buffer[16] & 0xFF);
                                    int code = (buffer[19] & 0xFF) << 8 | (buffer[18] & 0xFF);
                                    int value = buffer[20] & 0xFF;

                                    if (type == 1 && value == 1)
                                        keyPressCount.incrementAndGet();
                                    if (type == 2)
                                        mouseMoveCount.incrementAndGet();
                                    if (type == 1 && code >= 0x110 && code <= 0x11F)
                                        mouseClickCount.incrementAndGet();

                                    lastActivity = Instant.now();
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                    Thread.sleep(30);
                }

                for (java.io.RandomAccessFile f : files)
                    try { f.close(); } catch (Exception ignored) {}

            } catch (Exception e) {
                inputActive = false;
            }
        }, "LinuxInputMonitor");

        inputThread.setDaemon(true);
        inputThread.start();
    }

    @Override
    public void stopInputMonitoring() {
        inputActive = false;
        if (inputThread != null) {
            try { inputThread.join(200); } catch (InterruptedException ignored) {}
        }
    }

    @Override
    public Map<String, Long> getInputStats() {
        Map<String, Long> map = new HashMap<>();
        map.put("keys", (long) keyPressCount.get());
        map.put("clicks", (long) mouseClickCount.get());
        map.put("moves", mouseMoveCount.get());
        map.put("lastActivitySecAgo",
                Instant.now().getEpochSecond() - lastActivity.getEpochSecond());
        return map;
    }

    @Override
    public long getLastActivitySeconds() {
        return Instant.now().getEpochSecond() - lastActivity.getEpochSecond();
    }

    @Override
    public Map<String, Object> collectAllMetrics() {
        updateDiskStats();

        Map<String, Object> m = new HashMap<>();
        m.put("cpuLoad", getCpuLoad());
        m.put("ramUsed", getRamUsed());
        m.put("ramTotal", getRamTotal());

        m.put("diskTotal", getDiskTotal());
        m.put("diskFree", getDiskFree());
        m.put("diskUsed", getDiskUsed());
        m.put("diskDetails", diskDetails);

        m.put("activeWindow", getActiveWindowTitle());
        m.put("uptime", getUptime());
        m.put("osName", "Linux");

        if (inputActive)
            m.putAll(getInputStats());

        return m;
    }
}
