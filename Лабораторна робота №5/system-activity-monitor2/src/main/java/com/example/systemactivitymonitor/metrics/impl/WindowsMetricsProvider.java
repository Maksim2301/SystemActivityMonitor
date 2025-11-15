package com.example.systemactivitymonitor.metrics.impl;

import com.example.systemactivitymonitor.metrics.MetricsProvider;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.*;
import com.sun.jna.platform.win32.WinBase.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class WindowsMetricsProvider implements MetricsProvider {

    private long lastIdleTime = 0;
    private long lastKernelTime = 0;
    private long lastUserTime = 0;

    private BigDecimal totalDiskGb = BigDecimal.ZERO;
    private BigDecimal freeDiskGb = BigDecimal.ZERO;
    private String disksDetails = "Unknown";

    private final AtomicInteger keyPressCount = new AtomicInteger(0);
    private final AtomicInteger mouseClickCount = new AtomicInteger(0);
    private final AtomicLong mouseMoveCount = new AtomicLong(0);
    private int lastX = -1;
    private int lastY = -1;

    @Override
    public BigDecimal getCpuLoad() {
        Kernel32 kernel = Kernel32.INSTANCE;
        FILETIME idleTime = new FILETIME();
        FILETIME kernelTime = new FILETIME();
        FILETIME userTime = new FILETIME();

        if (!kernel.GetSystemTimes(idleTime, kernelTime, userTime))
            return BigDecimal.ZERO;

        long idle = idleTime.toDWordLong().longValue();
        long kernelT = kernelTime.toDWordLong().longValue();
        long userT = userTime.toDWordLong().longValue();

        if (lastIdleTime == 0) {
            lastIdleTime = idle;
            lastKernelTime = kernelT;
            lastUserTime = userT;
            return BigDecimal.ZERO;
        }

        long idleDiff = idle - lastIdleTime;
        long kernelDiff = kernelT - lastKernelTime;
        long userDiff = userT - lastUserTime;
        long total = kernelDiff + userDiff;

        lastIdleTime = idle;
        lastKernelTime = kernelT;
        lastUserTime = userT;

        if (total == 0) return BigDecimal.ZERO;

        double usage = (double) (total - idleDiff) / total * 100.0;
        return BigDecimal.valueOf(usage).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal getMemoryUsage() {
        Kernel32 kernel = Kernel32.INSTANCE;
        MEMORYSTATUSEX mem = new MEMORYSTATUSEX();
        if (!kernel.GlobalMemoryStatusEx(mem)) return BigDecimal.ZERO;

        long total = mem.ullTotalPhys.longValue();
        long free = mem.ullAvailPhys.longValue();
        double usedMb = (double) (total - free) / 1024 / 1024;

        return BigDecimal.valueOf(usedMb).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public void updateDiskStats() {
        Kernel32 kernel = Kernel32.INSTANCE;
        double total = 0;
        double free = 0;
        StringBuilder sb = new StringBuilder();

        for (char drive = 'C'; drive <= 'Z'; drive++) {
            String root = drive + ":\\";
            int type = kernel.GetDriveType(root);
            if (type == WinBase.DRIVE_FIXED) {
                WinNT.LARGE_INTEGER freeBytesAvail = new WinNT.LARGE_INTEGER();
                WinNT.LARGE_INTEGER totalBytes = new WinNT.LARGE_INTEGER();
                WinNT.LARGE_INTEGER freeBytes = new WinNT.LARGE_INTEGER();

                if (kernel.GetDiskFreeSpaceEx(root, freeBytesAvail, totalBytes, freeBytes)) {
                    double totalGb = (double) totalBytes.getValue() / 1e9;
                    double freeGb = (double) freeBytes.getValue() / 1e9;
                    double usedGb = totalGb - freeGb;
                    total += totalGb;
                    free += freeGb;
                    sb.append(String.format("%s: %.2f / %.2f GB | ", drive, usedGb, totalGb));
                }
            }
        }

        totalDiskGb = BigDecimal.valueOf(total).setScale(2, RoundingMode.HALF_UP);
        freeDiskGb = BigDecimal.valueOf(free).setScale(2, RoundingMode.HALF_UP);
        disksDetails = sb.isEmpty() ? "Unknown" : sb.toString().replaceAll("\\| $", "");
    }

    @Override
    public String getActiveWindowTitle() {
        try {
            char[] buffer = new char[512];
            WinDef.HWND hwnd = User32.INSTANCE.GetForegroundWindow();
            if (hwnd == null) return "Unknown";
            User32.INSTANCE.GetWindowText(hwnd, buffer, 512);
            String title = Native.toString(buffer).trim();
            return title.isEmpty() ? "Unknown" : title.length() > 120 ? title.substring(0, 120) : title;
        } catch (Exception e) {
            return "Unknown";
        }
    }

    @Override
    public String getUptime() {
        long uptimeSec = Kernel32.INSTANCE.GetTickCount64() / 1000;
        long days = uptimeSec / 86400;
        long hours = (uptimeSec % 86400) / 3600;
        long minutes = (uptimeSec % 3600) / 60;
        return String.format("%d d %d h %d m", days, hours, minutes);
    }

    @Override
    public void updateInputActivity() {
        User32 user32 = User32.INSTANCE;

        for (int i = 0x08; i <= 0xFE; i++) {
            short state = user32.GetAsyncKeyState(i);
            if ((state & 0x0001) != 0) keyPressCount.incrementAndGet();
        }

        if ((user32.GetAsyncKeyState(0x01) & 0x0001) != 0) mouseClickCount.incrementAndGet();
        if ((user32.GetAsyncKeyState(0x02) & 0x0001) != 0) mouseClickCount.incrementAndGet();

        WinDef.POINT p = new WinDef.POINT();
        user32.GetCursorPos(p);
        if (lastX != -1 && lastY != -1 && (p.x != lastX || p.y != lastY))
            mouseMoveCount.incrementAndGet();

        lastX = p.x;
        lastY = p.y;
    }

    @Override
    public Map<String, Long> getInputStats() {
        Map<String, Long> input = new HashMap<>();
        input.put("keys", (long) keyPressCount.get());
        input.put("clicks", (long) mouseClickCount.get());
        input.put("moves", mouseMoveCount.get());
        return input;
    }

    @Override
    public Map<String, Object> collectAllMetrics() {
        updateDiskStats();
        Map<String, Object> data = new HashMap<>();
        data.put("cpu", getCpuLoad());
        data.put("ram", getMemoryUsage());
        data.put("window", getActiveWindowTitle());
        data.put("osName", "Windows");
        data.put("uptime", getUptime());
        data.put("diskUsed", totalDiskGb.subtract(freeDiskGb));
        data.put("diskTotal", totalDiskGb);
        data.put("diskDetails", disksDetails);
        return data;
    }
}
