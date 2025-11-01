package com.example.systemactivitymonitor.controller;

import com.example.systemactivitymonitor.agent.AppMonitorManager;
import com.example.systemactivitymonitor.agent.InputActivityChecker;
import com.example.systemactivitymonitor.model.User;
import com.example.systemactivitymonitor.util.Session;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
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
    @FXML private Label movesLabel;
    @FXML private Label uptimeLabel;
    @FXML private Label diskLabel;
    @FXML private Label statusLabel;

    @FXML private Button startButton;
    @FXML private Button stopButton;
//    @FXML private Button retryButton;

    private InputActivityChecker inputChecker;
    private User activeUser;
    private ScheduledExecutorService uiUpdater;
    private boolean isMonitoring = false;

    @FXML
    public void initialize() {
        activeUser = Session.getCurrentUser();
        inputChecker = null; // створюється лише після Start
        stopButton.setDisable(true);
//        retryButton.setDisable(true);
        statusLabel.setText("Готово до запуску моніторингу");
    }

    /** ▶️ Запуск моніторингу */
    @FXML
    private void startMonitoring() {
        if (isMonitoring) {
            showAlert("Моніторинг уже запущено.");
            return;
        }

        try {
            isMonitoring = true;
            startButton.setDisable(true);
            stopButton.setDisable(false);
//            retryButton.setDisable(true);

            // Запуск фонового моніторингу
            AppMonitorManager.start(Session.isGuest() ? null : activeUser);
            inputChecker = AppMonitorManager.getInputChecker();

            startAutoUIUpdate();
            statusLabel.setText("Моніторинг активний (оновлення кожні 5 секунд)");
        } catch (Exception e) {
            handleMonitoringError(e);
        }
    }

    /** Зупинити моніторинг */
    @FXML
    private void stopMonitoring() {
        try {
            if (!isMonitoring) {
                showAlert("Моніторинг уже зупинено.");
                return;
            }

            isMonitoring = false;
            AppMonitorManager.stop();
            stopAutoUIUpdate();

            startButton.setDisable(false);
            stopButton.setDisable(true);
//            retryButton.setDisable(false);
            statusLabel.setText("Моніторинг зупинено повністю");

        } catch (Exception e) {
            handleMonitoringError(e);
        }
    }

    /** Повторити після помилки */
    @FXML
    private void retryMonitoring() {
        statusLabel.setText("Повторний запуск моніторингу...");
//        retryButton.setDisable(true);
        startMonitoring();
    }

    /** Автоматичне оновлення UI */
    private void startAutoUIUpdate() {
        uiUpdater = Executors.newSingleThreadScheduledExecutor();
        uiUpdater.scheduleAtFixedRate(() -> Platform.runLater(() -> {
            try {
                if (isMonitoring) refreshStats();
            } catch (Exception e) {
                handleMonitoringError(e);
            }
        }), 0, 5, TimeUnit.SECONDS);
    }

    /** Зупинити оновлення інтерфейсу */
    private void stopAutoUIUpdate() {
        if (uiUpdater != null && !uiUpdater.isShutdown()) {
            uiUpdater.shutdownNow();
            uiUpdater = null;
        }
    }

    /** Оновлення інтерфейсу у реальному часі */
    private void refreshStats() {
        var collector = AppMonitorManager.getMetricsCollector();
        BigDecimal cpu = collector.getCpuLoad();
        BigDecimal ram = collector.getMemoryUsageMb();
        String window = collector.getActiveWindowTitle();
        String osName = System.getProperty("os.name");
        String uptime = collector.getUptime();
        BigDecimal diskTotal = collector.getTotalDiskGb();
        BigDecimal diskFree = collector.getFreeDiskGb();
        BigDecimal diskUsed = diskTotal.subtract(diskFree);
        String diskDetails = collector.getDisksDetails();

        cpuLabel.setText(cpu + " %");
        ramLabel.setText(ram + " MB");
        osLabel.setText(osName);
        windowLabel.setText(window);
        uptimeLabel.setText("Uptime: " + uptime);
        diskLabel.setText(String.format(" %.2f / %.2f GB | %s", diskUsed, diskTotal, diskDetails));

        if (inputChecker != null) {
            inputChecker.checkInput();
            keysLabel.setText("Keys: " + inputChecker.getKeyPressCount());
            clicksLabel.setText("Clicks: " + inputChecker.getMouseClickCount());
            movesLabel.setText("Moves: " + inputChecker.getMouseMoveCount());
        }
    }

    /** Оновити статистику вручну і зберегти у базу */
    @FXML
    private void updateNow() {
        try {
            var collector = AppMonitorManager.getMetricsCollector();
            if (inputChecker == null) inputChecker = AppMonitorManager.getInputChecker();
            refreshStats();

            if (!Session.isGuest() && activeUser != null) {
                AppMonitorManager.getMonitoringService().recordSystemStats(
                        activeUser,
                        collector.getCpuLoad().doubleValue(),
                        collector.getMemoryUsageMb().doubleValue(),
                        collector.getActiveWindowTitle(),
                        inputChecker.getKeyPressCount(),
                        inputChecker.getMouseClickCount()
                );
                statusLabel.setText(" Статистика вручну оновлена і збережена (" + java.time.LocalTime.now().withNano(0) + ")");
            } else {
                statusLabel.setText(" Гість: дані оновлені лише на екрані (без збереження у базу)");
            }

        } catch (Exception e) {
            handleMonitoringError(e);
        }
    }

    /** Обробка помилок */
    private void handleMonitoringError(Exception e) {
        AppMonitorManager.stop();
        stopAutoUIUpdate();
        isMonitoring = false;

        startButton.setDisable(true);
        stopButton.setDisable(true);
//        retryButton.setDisable(false);
        statusLabel.setText("Помилка моніторингу: " + e.getMessage());
        showAlert("Помилка: " + e.getMessage());
    }

    /** Повернення до головного меню */
    @FXML
    private void goBack() {
        stopAutoUIUpdate();
        switchScene("/fxml/main.fxml", "Main Menu");
    }

    /** Зміна сцени */
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

    /** Повідомлення користувачу */
    private void showAlert(String text) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Повідомлення");
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.showAndWait();
    }
}
