package com.example.systemactivitymonitor.service;

import com.example.systemactivitymonitor.metrics.MetricsProvider;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * AdvancedMonitoringService — розширена аналітична абстракція (Refined Abstraction).
 * ✔ Працює з новим форматом метрик (cpu, ramUsed, ramTotal, diskTotal, diskFree)
 * ✔ Має захист від поділу на нуль
 * ✔ Має безпечні конвертери через MonitoringService
 * ✔ Має покращений вивід попереджень
 * ✔ Не блокує моніторинг при помилці
 */
public class AdvancedMonitoringService extends MonitoringService {

    public AdvancedMonitoringService(MetricsProvider provider) {
        super(provider);
    }

    @Override
    protected void collectMetrics() {
        if (!active) return;

        Map<String, Object> data = metricsProvider.collectAllMetrics();

        // ----------------------- SAFE EXTRACTION -----------------------
        BigDecimal cpu = toDecimal(data.get("cpu"));
        BigDecimal ramUsed = toDecimal(data.get("ramUsed"));
        BigDecimal ramTotal = toDecimal(data.get("ramTotal"));
        BigDecimal diskTotal = toDecimal(data.get("diskTotal"));
        BigDecimal diskFree = toDecimal(data.get("diskFree"));

        // ----------------------- ANALYTICS ------------------------------

        // ⚠️ CPU WARNING
        if (cpu.compareTo(BigDecimal.valueOf(90)) > 0) {
            System.out.printf("🔥 Високе навантаження CPU — %.2f%%%n", cpu);
        }

        // ⚠️ RAM WARNING
        BigDecimal ramPercent = BigDecimal.ZERO;
        if (ramTotal.compareTo(BigDecimal.ZERO) > 0) {
            ramPercent = ramUsed
                    .divide(ramTotal, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        if (ramPercent.compareTo(BigDecimal.valueOf(85)) > 0) {
            System.out.printf("🧠 Високе використання RAM — %.2f%% (%.2f MB)%n",
                    ramPercent, ramUsed);
        }

        // ⚠️ DISK WARNING
        BigDecimal freePercent = BigDecimal.ZERO;
        if (diskTotal.compareTo(BigDecimal.ZERO) > 0) {
            freePercent = diskFree
                    .divide(diskTotal, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        if (freePercent.compareTo(BigDecimal.valueOf(10)) < 0) {
            System.out.printf("💾 Мало вільного місця на диску — %.2f%% залишилось%n",
                    freePercent);
        }

        // ----------------------- SAVE TO DB ----------------------------
        if (activeUser != null) {
            try {
                recordSystemStats(data, activeUser);
            } catch (Exception e) {
                System.err.println("[AdvancedMonitoringService] Помилка при збереженні метрик: " + e.getMessage());
            }
        }
    }
}
