package com.example.systemactivitymonitor.controller;

import com.example.systemactivitymonitor.model.User;
import com.example.systemactivitymonitor.service.MonitoringService;
import com.example.systemactivitymonitor.util.Session;
import com.example.systemactivitymonitor.factory.EnvironmentFactoryProducer;
import com.example.systemactivitymonitor.factory.SystemEnvironmentFactory;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.Map;
import java.util.concurrent.*;

public class MonitoringController {

    @FXML private Label cpuLabel, ramLabel, osLabel, windowLabel;
    @FXML private Label keysLabel, clicksLabel, movesLabel, uptimeLabel;
    @FXML private Label diskLabel, statusLabel;

    @FXML private Button startButton, stopButton;

    private User activeUser;
    private MonitoringService monitoringService;
    private ScheduledExecutorService uiUpdater;
    private boolean isMonitoring = false;

    @FXML
    public void initialize() {
        activeUser = Session.getCurrentUser();
        stopButton.setDisable(true);
        statusLabel.setText("Готово до запуску моніторингу");
    }

    // ============================================================================
    // ▶️ START MONITORING
    // ============================================================================
    @FXML
    private void startMonitoring() {
        if (isMonitoring) {
            showAlert("Моніторинг уже запущено.");
            return;
        }

        try {
            SystemEnvironmentFactory factory = EnvironmentFactoryProducer.getFactory();
            monitoringService = factory.createMonitoringService();

            monitoringService.start(Session.isGuest() ? null : activeUser);

            isMonitoring = true;
            startButton.setDisable(true);
            stopButton.setDisable(false);

            startAutoUIUpdate();

            statusLabel.setText("Моніторинг активний (оновлення кожні 5 секунд)");
            System.out.println("✅ Моніторинг запущено (через Abstract Factory)");

        } catch (Exception e) {
            handleMonitoringError(e);
        }
    }

    // ============================================================================
    // ⏹ STOP MONITORING
    // ============================================================================
    @FXML
    private void stopMonitoring() {
        try {
            if (!isMonitoring) return;

            monitoringService.stop();
            stopAutoUIUpdate();

            isMonitoring = false;
            startButton.setDisable(false);
            stopButton.setDisable(true);

            statusLabel.setText("Моніторинг зупинено.");

        } catch (Exception e) {
            handleMonitoringError(e);
        }
    }

    // ============================================================================
    // 🔄 SCHEDULED UI UPDATE
    // ============================================================================
    private void startAutoUIUpdate() {
        uiUpdater = Executors.newSingleThreadScheduledExecutor();
        uiUpdater.scheduleAtFixedRate(() ->
                        Platform.runLater(this::refreshStats),
                0, 5, TimeUnit.SECONDS);
    }

    private void stopAutoUIUpdate() {
        if (uiUpdater != null && !uiUpdater.isShutdown()) {
            uiUpdater.shutdownNow();
            uiUpdater = null;
        }
    }

    // ============================================================================
    // 🔹 REFRESH UI STATS
    // ============================================================================
    private void refreshStats() {
        try {
            Map<String, Object> data = monitoringService.collectFormattedStats();

            if (data == null || data.isEmpty()) return;

            cpuLabel.setText(data.get("cpuLoad") + " %");

            // RAM — показуємо як "used / total"
            ramLabel.setText(
                    String.format("%s / %s MB",
                            data.get("ramUsed"),
                            data.get("ramTotal"))
            );

            osLabel.setText((String) data.get("osName"));
            windowLabel.setText((String) data.get("activeWindow"));
            uptimeLabel.setText("Uptime: " + data.get("uptime"));

            diskLabel.setText(
                    String.format("📀 %.2f / %.2f GB  (%s)",
                            data.get("diskUsed"),
                            data.get("diskTotal"),
                            data.get("diskDetails"))
            );

            keysLabel.setText("Keys: " + data.get("keys"));
            clicksLabel.setText("Clicks: " + data.get("clicks"));
            movesLabel.setText("Moves: " + data.get("moves"));

        } catch (Exception e) {
            handleMonitoringError(e);
        }
    }

    // ============================================================================
    // 🧾 MANUAL RECORD
    // ============================================================================
    @FXML
    private void updateNow() {
        try {
            if (!Session.isGuest() && activeUser != null) {
                monitoringService.saveNow(activeUser);
                statusLabel.setText(monitoringService.formatStatusSaved());
            } else {
                statusLabel.setText("⚠ Гість: дані не збережено.");
            }

            refreshStats();

        } catch (Exception e) {
            handleMonitoringError(e);
        }
    }

    // ============================================================================
    // ⚠ ERROR HANDLING
    // ============================================================================
    private void handleMonitoringError(Exception e) {
        if (monitoringService != null) monitoringService.stop();
        stopAutoUIUpdate();

        isMonitoring = false;
        startButton.setDisable(false);
        stopButton.setDisable(true);

        statusLabel.setText("Помилка: " + e.getMessage());
        showAlert("Помилка: " + e.getMessage());
        e.printStackTrace();
    }

    // ============================================================================
    // ⏪ GO BACK
    // ============================================================================
    @FXML
    private void goBack() {
        stopAutoUIUpdate();
        if (monitoringService != null) monitoringService.stop();
        switchScene("/fxml/main.fxml", "Main Menu");
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

    // ============================================================================
    // 🔔 ALERT
    // ============================================================================
    private void showAlert(String text) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Повідомлення");
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.showAndWait();
    }
}
