package com.example.systemactivitymonitor.controller;

import com.example.systemactivitymonitor.model.IdleTime;
import com.example.systemactivitymonitor.model.User;
import com.example.systemactivitymonitor.service.IdleService;
import com.example.systemactivitymonitor.util.Session;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.Duration;

public class IdleController {

    @FXML private Label statusLabel;
    @FXML private Label messageLabel;
    @FXML private RadioButton onlineRadio;
    @FXML private RadioButton offlineRadio;
    @FXML private ToggleGroup modeGroup;

    private User activeUser;
    private IdleService idleService;

    @FXML
    public void initialize() {

        this.activeUser = Session.getCurrentUser();
        this.idleService = new IdleService();

        // Гостьовий режим – лише UI
        if (Session.isGuest()) {
            messageLabel.setText("Гостьовий режим: стан не зберігається.");
        } else {
            messageLabel.setText("Ви онлайн.");
        }

        updateOnlineStatus();
        onlineRadio.setSelected(true);
    }

    // =====================================================================
    // ONLINE
    // =====================================================================
    @FXML
    private void setOnlineMode() {

        if (Session.isGuest()) {
            updateOnlineStatus();
            messageLabel.setText("Гостьовий режим. Простію не зберігається.");
            return;
        }

        try {
            IdleTime ended = idleService.endIdle(activeUser);

            if (ended != null && ended.getDurationSeconds() != null) {

                Duration d = Duration.ofSeconds(ended.getDurationSeconds());
                messageLabel.setText(String.format(
                        "Простій завершено. Тривав: %d хв %d сек.",
                        d.toMinutes(), d.minusMinutes(d.toMinutes()).toSeconds()
                ));

            } else {
                messageLabel.setText("Простій не був активним.");
            }

            updateOnlineStatus();

        } catch (Exception e) {
            messageLabel.setText("Помилка: " + e.getMessage());
        }
    }

    // =====================================================================
    // OFFLINE
    // =====================================================================
    @FXML
    private void setOfflineMode() {

        if (Session.isGuest()) {
            updateOfflineStatus();
            messageLabel.setText("Гостьовий режим: простій лише візуальний.");
            return;
        }

        try {
            IdleTime started = idleService.startIdle(activeUser);
            messageLabel.setText("Режим OFFLINE. Початок простою: " + started.getStartTime());
            updateOfflineStatus();

        } catch (IllegalStateException e) {
            messageLabel.setText("Простій вже триває.");
            offlineRadio.setSelected(true);

        } catch (Exception e) {
            messageLabel.setText("Помилка: " + e.getMessage());
            onlineRadio.setSelected(true);
        }
    }

    // =====================================================================
    // UI helpers
    // =====================================================================
    private void updateOnlineStatus() {
        statusLabel.setText("Статус: Онлайн");
        statusLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
    }

    private void updateOfflineStatus() {
        statusLabel.setText("Статус: Офлайн");
        statusLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
    }

    // =====================================================================
    // Back to main
    // =====================================================================
    @FXML
    private void goBack() {
        switchScene("/fxml/main.fxml", "Main Menu");
    }

    private void switchScene(String fxml, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) statusLabel.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle(title);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
