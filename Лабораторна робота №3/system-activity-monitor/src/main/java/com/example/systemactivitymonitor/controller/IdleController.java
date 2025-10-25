package com.example.systemactivitymonitor.controller;

import com.example.systemactivitymonitor.model.IdleTime;
import com.example.systemactivitymonitor.model.User;
import com.example.systemactivitymonitor.service.impl.IdleServiceImpl;
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

    private final IdleServiceImpl idleService = new IdleServiceImpl();
    private IdleTime currentIdle = null;
    private User activeUser;

    @FXML
    public void initialize() {
        activeUser = Session.getCurrentUser();
        if (activeUser == null) {
            messageLabel.setText("Спочатку увійдіть у систему!");
        } else {
            messageLabel.setText("Ви зараз онлайн.");
        }
    }

    @FXML
    private void setOnlineMode() {
        if (activeUser == null) {
            messageLabel.setText("Спочатку увійдіть у систему!");
            return;
        }

        if (currentIdle != null) {
            currentIdle.setEndTime(LocalDateTime.now());
            idleService.endIdle(activeUser);

            // Обчислюємо тривалість
            Duration duration = Duration.between(currentIdle.getStartTime(), currentIdle.getEndTime());
            long minutes = duration.toMinutes();
            long seconds = duration.minusMinutes(minutes).toSeconds();

            messageLabel.setText(String.format(
                    "Ви знову онлайн. Тривалість простою: %d хв %d сек.",
                    minutes, seconds
            ));

            currentIdle = null;
        } else {
            messageLabel.setText("Ви вже онлайн.");
        }

        statusLabel.setText("Статус: Онлайн");
        statusLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
    }

    @FXML
    private void setOfflineMode() {
        if (activeUser == null) {
            messageLabel.setText("Спочатку увійдіть у систему!");
            onlineRadio.setSelected(true);
            return;
        }

        if (currentIdle == null) {
            currentIdle = idleService.startIdle(activeUser);
            messageLabel.setText("Початок простою: " + currentIdle.getStartTime());
        } else {
            messageLabel.setText("Простій уже триває з " + currentIdle.getStartTime());
        }

        statusLabel.setText("Статус: Офлайн");
        statusLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
    }

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
