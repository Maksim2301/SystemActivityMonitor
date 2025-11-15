package com.example.systemactivitymonitor.controller;

import com.example.systemactivitymonitor.util.Session;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.stage.Window;

public class MainController {

    @FXML private Label guestLabel;

    @FXML
    public void initialize() {
        if (Session.isGuest()) {
            guestLabel.setText("Guest Mode — дані не зберігаються у базу");
        } else {
            guestLabel.setText("");
        }
    }

    @FXML
    private void openUserPanel(ActionEvent event) {
        switchScene("/fxml/user.fxml", "User Management");
    }

    @FXML
    private void openMonitoringPanel(ActionEvent event) {
        switchScene("/fxml/monitoring.fxml", "System Monitoring");
    }

    @FXML
    private void openReportsPanel(ActionEvent event) {
        if (Session.isGuest()) {
            showAlert("У гостьовому режимі звіти недоступні.");
            return;
        }
        if (!Session.isLoggedIn()) {
            showAlert("Спочатку увійдіть у систему!");
            return;
        }
        switchScene("/fxml/reports.fxml", "Reports");
    }

    @FXML
    private void openIdlePanel(ActionEvent event) {
        switchScene("/fxml/idle.fxml", "Idle Tracker");
    }

    @FXML
    private void exitApp() {
        Session.logout();
        System.exit(0);
    }

    private void switchScene(String fxml, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) Stage.getWindows().filtered(Window::isShowing).get(0);
            stage.setScene(scene);
            stage.setTitle(title);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String text) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Повідомлення");
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.showAndWait();
    }
}
