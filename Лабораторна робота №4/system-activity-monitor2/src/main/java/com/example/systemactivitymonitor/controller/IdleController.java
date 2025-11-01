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
import java.time.LocalDateTime;

public class IdleController {

    @FXML private Label statusLabel;
    @FXML private Label messageLabel;
    @FXML private RadioButton onlineRadio;
    @FXML private RadioButton offlineRadio;
    @FXML private ToggleGroup modeGroup;

    private final IdleService idleService = new IdleService();
    private User activeUser;

    @FXML
    public void initialize() {
        activeUser = Session.getCurrentUser();

        messageLabel.setText(Session.isGuest()
                ? "Гостьовий режим активний. Можете перемикати стани, але вони не зберігаються."
                : "Ви зараз онлайн.");

        statusLabel.setText("Статус: Онлайн");
        statusLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
    }

    @FXML
    private void setOnlineMode() {
        if (Session.isGuest()) {
            messageLabel.setText("Гостьовий режим: дані не зберігаються.");
            updateOnlineStatus();
            return;
        }

        try {
            // Завершуємо простій через сервіс
            IdleTime ended = idleService.endIdle(activeUser);
            if (ended != null && ended.getDurationSeconds() != null) {
                Duration d = Duration.ofSeconds(ended.getDurationSeconds());
                messageLabel.setText(String.format(
                        "Ви знову онлайн. Простій тривав %d хв %d сек.",
                        d.toMinutes(), d.minusMinutes(d.toMinutes()).toSeconds()
                ));
            } else {
                messageLabel.setText("Ви вже онлайн або простій не був активним.");
            }
            updateOnlineStatus();
        } catch (Exception e) {
            messageLabel.setText("Помилка: " + e.getMessage());
            onlineRadio.setSelected(true);
        }
    }

    @FXML
    private void setOfflineMode() {
        if (Session.isGuest()) {
            messageLabel.setText("Гостьовий режим: простій лише візуальний.");
            updateOfflineStatus();
            return;
        }

        try {
            IdleTime idle = idleService.startIdle(activeUser);
            messageLabel.setText("Початок простою: " + idle.getStartTime());
            updateOfflineStatus();
        } catch (IllegalStateException e) {
            messageLabel.setText("Простій уже триває: завершіть поточний перед новим.");
            offlineRadio.setSelected(true);
        } catch (Exception e) {
            messageLabel.setText("Помилка: " + e.getMessage());
            onlineRadio.setSelected(true);
        }
    }

    @FXML
    private void goBack() {
        switchScene("/fxml/main.fxml", "Main Menu");
    }

    // === Додаткові методи для UI ===

    private void updateOnlineStatus() {
        statusLabel.setText("Статус: Онлайн");
        statusLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
    }

    private void updateOfflineStatus() {
        statusLabel.setText("Статус: Офлайн");
        statusLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
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
