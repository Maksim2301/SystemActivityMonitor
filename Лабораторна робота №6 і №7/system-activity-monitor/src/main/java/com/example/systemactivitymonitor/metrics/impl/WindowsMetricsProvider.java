package com.example.systemactivitymonitor.metrics.impl;

import com.example.systemactivitymonitor.metrics.MetricsProvider;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Windows реалізація MetricsProvider.
 * Працює через JNA.
 * ✔ CPU рахується правильно у фоні (1 раз/сек)
 * ✔ UI більше не отримує 0%
 * ✔ Статично стабільна робота без конфліктів потоків
 */
public class WindowsMetricsProvider implements MetricsProvider {

    private ScheduledExecutorService inputScheduler;
    private static final int VK_LBUTTON = 0x01;
    private static final int VK_RBUTTON = 0x02;
    private int lastX = -1;
    private int lastY = -1;
    private static final int MOVE_THRESHOLD = 3;

    // ------------------------ CPU state ------------------------
    private long lastIdleTime = 0;
    private long lastKernelTime = 0;
    private long lastUserTime = 0;

    private volatile BigDecimal lastCpuLoad = BigDecimal.ZERO;

    private final ScheduledExecutorService cpuScheduler =
            Executors.newSingleThreadScheduledExecutor();

    // ------------------------ Disk state ------------------------
    private BigDecimal diskTotal = BigDecimal.ZERO;
    private BigDecimal diskFree = BigDecimal.ZERO;
    private String diskDetails = "Unknown";

    // ------------------------ RAM total ------------------------
    private BigDecimal ramTotalMb = BigDecimal.ZERO;

    // ------------------------ Input Activity ------------------------
    private final AtomicInteger keyPressCount = new AtomicInteger(0);
    private final AtomicInteger mouseClickCount = new AtomicInteger(0);
    private final AtomicLong mouseMoveCount = new AtomicLong(0);
    private volatile long lastActivityTimestamp = System.currentTimeMillis();

    private volatile boolean inputMonitoringActive = false;

    // ------------------------ Constructor ------------------------
    public WindowsMetricsProvider() {
        // Фоновий розрахунок CPU раз на 1 сек
        cpuScheduler.scheduleAtFixedRate(this::updateCpuLoad,
                0, 1, TimeUnit.SECONDS);
    }

    private long filetimeToLong(WinBase.FILETIME ft) {
        return ((long) ft.dwHighDateTime << 32) | (ft.dwLowDateTime & 0xffffffffL);
    }

    // ========================================================================
    // CPU — фонове оновлення
    // ========================================================================
    private void updateCpuLoad() {

        Kernel32 kernel = Kernel32.INSTANCE;

        WinBase.FILETIME idle = new WinBase.FILETIME();
        WinBase.FILETIME kernelTime = new WinBase.FILETIME();
        WinBase.FILETIME userTime = new WinBase.FILETIME();

        if (!kernel.GetSystemTimes(idle, kernelTime, userTime))
            return;

        long idleNow = filetimeToLong(idle);
        long kernelNow = filetimeToLong(kernelTime);
        long userNow = filetimeToLong(userTime);

        if (lastIdleTime == 0) {
            lastIdleTime = idleNow;
            lastKernelTime = kernelNow;
            lastUserTime = userNow;
            return;
        }

        long idleDiff = idleNow - lastIdleTime;
        long kernelDiff = kernelNow - lastKernelTime;
        long userDiff = userNow - lastUserTime;

        lastIdleTime = idleNow;
        lastKernelTime = kernelNow;
        lastUserTime = userNow;

        long total = kernelDiff + userDiff;
        if (total <= 0) return;

        double usage = (double) (total - idleDiff) / total * 100.0;

        if (usage < 0) usage = 0;
        if (usage > 100) usage = 100;

        lastCpuLoad = BigDecimal.valueOf(usage).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal getCpuLoad() {
        return lastCpuLoad;
    }

    // ========================================================================
    // RAM
    // ========================================================================
    @Override
    public BigDecimal getRamUsed() {
        Kernel32 kernel32 = Kernel32.INSTANCE;

        WinBase.MEMORYSTATUSEX mem = new WinBase.MEMORYSTATUSEX();
        if (!kernel32.GlobalMemoryStatusEx(mem))
            return BigDecimal.ZERO;

        long total = mem.ullTotalPhys.longValue();
        long free = mem.ullAvailPhys.longValue();

        ramTotalMb = BigDecimal.valueOf(total / 1024.0 / 1024.0)
                .setScale(2, RoundingMode.HALF_UP);

        double usedMb = (total - free) / 1024.0 / 1024.0;
        return BigDecimal.valueOf(usedMb).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal getRamTotal() {
        return ramTotalMb;
    }

    // ========================================================================
    // DISK
    // ========================================================================
    @Override
    public void updateDiskStats() {
        Kernel32 kernel = Kernel32.INSTANCE;

        double total = 0;
        double free = 0;
        StringBuilder sb = new StringBuilder();

        for (char drive = 'C'; drive <= 'Z'; drive++) {
            String root = drive + ":\\";

            int type = kernel.GetDriveType(root);
            if (type != WinBase.DRIVE_FIXED)
                continue;

            WinNT.LARGE_INTEGER freeAvail = new WinNT.LARGE_INTEGER();
            WinNT.LARGE_INTEGER totalBytes = new WinNT.LARGE_INTEGER();
            WinNT.LARGE_INTEGER freeBytes = new WinNT.LARGE_INTEGER();

            if (kernel.GetDiskFreeSpaceEx(root, freeAvail, totalBytes, freeBytes)) {
                double totalGb = totalBytes.getValue() / 1e9;
                double freeGb = freeBytes.getValue() / 1e9;
                double usedGb = totalGb - freeGb;

                total += totalGb;
                free += freeGb;

                sb.append(String.format("%s: %.2f / %.2f GB | ", drive, usedGb, totalGb));
            }
        }

        diskTotal = BigDecimal.valueOf(total).setScale(2, RoundingMode.HALF_UP);
        diskFree = BigDecimal.valueOf(free).setScale(2, RoundingMode.HALF_UP);
        diskDetails = sb.length() == 0 ? "Unknown" : sb.toString().replaceAll("\\| $", "");
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
            char[] buffer = new char[512];

            WinDef.HWND hwnd = User32.INSTANCE.GetForegroundWindow();
            if (hwnd == null) return "Unknown";

            User32.INSTANCE.GetWindowText(hwnd, buffer, 512);
            String title = Native.toString(buffer).trim();

            if (title.isEmpty()) return "Unknown";
            return title.length() > 120 ? title.substring(0, 120) : title;

        } catch (Exception e) {
            return "Unknown";
        }
    }

    // ========================================================================
    // UPTIME
    // ========================================================================
    @Override
    public String getUptime() {

        long uptimeSec = Kernel32.INSTANCE.GetTickCount64() / 1000;

        long days = uptimeSec / 86400;
        long hours = (uptimeSec % 86400) / 3600;
        long minutes = (uptimeSec % 3600) / 60;

        return String.format("%d d %d h %d m", days, hours, minutes);
    }

    // ========================================================================
    // INPUT MONITORING
    // ========================================================================
    @Override
    public void startInputMonitoring() {
        if (inputMonitoringActive) return;

        inputMonitoringActive = true;

        inputScheduler = Executors.newSingleThreadScheduledExecutor();

        inputScheduler.scheduleAtFixedRate(() -> {
            try {
                checkInput();
            } catch (Exception ignored) {}
        }, 0, 80, TimeUnit.MILLISECONDS); // 12.5 раз/сек
    }

    private void checkInput() {

        User32 user32 = User32.INSTANCE;

        // ------------------ KEYBOARD ------------------
        for (int i = 0x08; i <= 0xFE; i++) {
            short state = user32.GetAsyncKeyState(i);
            if ((state & 0x0001) != 0) { // нове натискання
                keyPressCount.incrementAndGet();
                lastActivityTimestamp = System.currentTimeMillis();
            }
        }

        // ------------------ MOUSE CLICKS ------------------
        if ((user32.GetAsyncKeyState(VK_LBUTTON) & 0x0001) != 0) {
            mouseClickCount.incrementAndGet();
            lastActivityTimestamp = System.currentTimeMillis();
        }

        if ((user32.GetAsyncKeyState(VK_RBUTTON) & 0x0001) != 0) {
            mouseClickCount.incrementAndGet();
            lastActivityTimestamp = System.currentTimeMillis();
        }

        // ------------------ MOUSE MOVES ------------------
        WinDef.POINT p = new WinDef.POINT();
        user32.GetCursorPos(p);

        if (lastX != -1 && lastY != -1) {
            int dx = Math.abs(p.x - lastX);
            int dy = Math.abs(p.y - lastY);
            if (dx >= MOVE_THRESHOLD || dy >= MOVE_THRESHOLD) {
                mouseMoveCount.incrementAndGet();
                lastActivityTimestamp = System.currentTimeMillis();
            }
        }

        lastX = p.x;
        lastY = p.y;
    }

    @Override
    public void stopInputMonitoring() {
        inputMonitoringActive = false;

        if (inputScheduler != null && !inputScheduler.isShutdown()) {
            inputScheduler.shutdownNow();
            inputScheduler = null;
        }
    }


    @Override
    public Map<String, Long> getInputStats() {
        Map<String, Long> map = new HashMap<>();
        map.put("keys", (long) keyPressCount.get());
        map.put("clicks", (long) mouseClickCount.get());
        map.put("moves", mouseMoveCount.get());
        map.put("lastActivitySecAgo", getLastActivitySeconds());
        return map;
    }

    @Override
    public long getLastActivitySeconds() {
        return (System.currentTimeMillis() - lastActivityTimestamp) / 1000;
    }

    // ========================================================================
    // COLLECT ALL METRICS
    // ========================================================================
    @Override
    public Map<String, Object> collectAllMetrics() {

        updateDiskStats();

        Map<String, Object> data = new HashMap<>();

        data.put("cpuLoad", getCpuLoad());
        data.put("ramUsed", getRamUsed());
        data.put("ramTotal", getRamTotal());

        data.put("diskTotal", getDiskTotal());
        data.put("diskFree", getDiskFree());
        data.put("diskUsed", getDiskUsed());
        data.put("diskDetails", diskDetails);

        data.put("activeWindow", getActiveWindowTitle());
        data.put("uptime", getUptime());
        data.put("osName", "Windows");

        if (inputMonitoringActive)
            data.putAll(getInputStats());

        return data;
    }
}
