package com.example.systemactivitymonitor.agent;

import com.example.systemactivitymonitor.model.User;
import com.example.systemactivitymonitor.service.MonitoringService;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Керує запуском і зупинкою глобального моніторингу (SystemMetricsCollector + InputActivityChecker).
 * Відповідає за оновлення даних і збереження їх у базу лише для зареєстрованих користувачів.
 */
public class AppMonitorManager {

    private static InputActivityChecker inputChecker;
    private static SystemMetricsCollector metricsCollector;
    private static MonitoringService monitoringService;

    private static ScheduledExecutorService backgroundSaver;
    private static boolean active = false;

    public static InputActivityChecker getInputChecker() {
        if (inputChecker == null) inputChecker = new InputActivityChecker();
        return inputChecker;
    }

    public static SystemMetricsCollector getMetricsCollector() {
        if (metricsCollector == null) metricsCollector = new SystemMetricsCollector();
        return metricsCollector;
    }

    public static MonitoringService getMonitoringService() {
        if (monitoringService == null) monitoringService = new MonitoringService();
        return monitoringService;
    }

    /** Запуск моніторингу у фоні (оновлення даних кожні 5 секунд) */
    public static void start(User user) {
        if (active) return;
        active = true;

        if (inputChecker == null) inputChecker = new InputActivityChecker();
        if (metricsCollector == null) metricsCollector = new SystemMetricsCollector();
        if (monitoringService == null) monitoringService = new MonitoringService();

        metricsCollector.start();

        backgroundSaver = Executors.newSingleThreadScheduledExecutor();
        backgroundSaver.scheduleAtFixedRate(() -> {
            try {
                inputChecker.checkInput();

                if (user != null) {
                    monitoringService.recordSystemStats(
                            user,
                            metricsCollector.getCpuLoad().doubleValue(),
                            metricsCollector.getMemoryUsageMb().doubleValue(),
                            metricsCollector.getActiveWindowTitle(),
                            inputChecker.getKeyPressCount(),
                            inputChecker.getMouseClickCount()
                    );
                }
            } catch (Exception e) {
                System.err.println("Background monitor error: " + e.getMessage());
            }
        }, 0, 5, TimeUnit.SECONDS);

        System.out.println("Глобальний моніторинг у фоні запущено.");
    }

    /** ⏹ Зупинити моніторинг */
    public static void stop() {
        active = false;

        if (backgroundSaver != null) {
            backgroundSaver.shutdownNow();
            backgroundSaver = null;
        }

        if (metricsCollector != null) {
            metricsCollector.stop();
            metricsCollector = null;
        }

        inputChecker = null;
        monitoringService = null;

        System.out.println("Глобальний моніторинг зупинено.");
    }

    public static boolean isActive() { return active; }
}
