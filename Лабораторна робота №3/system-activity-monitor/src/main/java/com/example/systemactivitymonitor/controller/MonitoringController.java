package com.example.systemactivitymonitor.controller;

import com.example.systemactivitymonitor.agent.SystemMetricsCollector;
import com.example.systemactivitymonitor.agent.ActiveWindowTracker;
import com.example.systemactivitymonitor.agent.KeyboardMouseListener;
import com.example.systemactivitymonitor.model.User;
import com.example.systemactivitymonitor.service.impl.MonitoringServiceImpl;
import com.example.systemactivitymonitor.util.Session;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MonitoringController {

    @FXML private Label cpuLabel;
    @FXML private Label ramLabel;
    @FXML private Label osLabel;
    @FXML private Label windowLabel;
    @FXML private Label keysLabel;
    @FXML private Label clicksLabel;

    private final MonitoringServiceImpl monitoringService = new MonitoringServiceImpl();
    private final SystemMetricsCollector systemCollector = new SystemMetricsCollector();
    private KeyboardMouseListener inputListener;

    private User activeUser;
    private ScheduledExecutorService scheduler;
    private boolean monitoringActive = false;

    @FXML
    public void initialize() {
        try {
            inputListener = new KeyboardMouseListener();
        } catch (Exception e) {
            System.err.println("Неможливо ініціалізувати KeyboardMouseListener: " + e.getMessage());
            inputListener = null;
        }
    }

    @FXML
    private void updateStats() {
        if (Session.getCurrentUser() == null) {
            cpuLabel.setText("Спочатку увійдіть у систему!");
            return;
        }

        activeUser = Session.getCurrentUser();

        if (!monitoringActive) {
            monitoringActive = true;
            startAutoMonitoring();
        } else {
            cpuLabel.setText("Моніторинг уже запущено...");
        }
    }

    private void startAutoMonitoring() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> Platform.runLater(() -> {
            try {
                monitoringService.recordSystemStats(activeUser);

                BigDecimal cpu = systemCollector.getCpuLoad();
                BigDecimal ram = systemCollector.getMemoryUsageMb();

                cpuLabel.setText("CPU: " + cpu + "%");
                ramLabel.setText("RAM: " + ram + " MB");
                osLabel.setText("OS: " + systemCollector.getOsName());
                windowLabel.setText("Window: " + ActiveWindowTracker.getActiveWindowTitle());

                // Якщо inputListener не ініціалізувався — показуємо 0
                int keys = (inputListener != null) ? inputListener.getKeyPressCount() : 0;
                int clicks = (inputListener != null) ? inputListener.getMouseClickCount() : 0;

                keysLabel.setText("Keys: " + keys);
                clicksLabel.setText("Clicks: " + clicks);
            } catch (Exception e) {
                cpuLabel.setText("Помилка моніторингу");
                e.printStackTrace();
            }
        }), 0, 5, TimeUnit.SECONDS);

        cpuLabel.setText("Моніторинг запущено (оновлення кожні 5 сек)");
    }

    @FXML
    private void goBack() {
        stopMonitoring();
        switchScene("/fxml/main.fxml", "Main Menu");
    }

    private void stopMonitoring() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            monitoringActive = false;
            System.out.println("Моніторинг зупинено");
        }
    }

    private void switchScene(String fxml, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) cpuLabel.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle(title);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
