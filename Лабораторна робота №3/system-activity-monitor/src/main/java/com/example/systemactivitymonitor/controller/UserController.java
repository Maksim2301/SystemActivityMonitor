package com.example.systemactivitymonitor.controller;

import com.example.systemactivitymonitor.model.User;
import com.example.systemactivitymonitor.service.impl.UserServiceImpl;
import com.example.systemactivitymonitor.util.Session;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.Optional;

public class UserController {

    @FXML private TextField registerUsername;
    @FXML private PasswordField registerPassword;
    @FXML private TextField registerEmail;
    @FXML private TextField loginUsername;
    @FXML private PasswordField loginPassword;
    @FXML private PasswordField oldPassword;
    @FXML private PasswordField newPassword;
    @FXML private Label messageLabel;

    private final UserServiceImpl userService = new UserServiceImpl();
    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    private void registerUser() {
        try {
            userService.registerUser(registerUsername.getText(), registerPassword.getText(), registerEmail.getText());
            messageLabel.setText("Користувач створений!");
        } catch (Exception e) {
            messageLabel.setText("Помилка: " + e.getMessage());
        }
    }

    @FXML
    private void loginUser() {
        Optional<User> userOpt = userService.login(loginUsername.getText(), loginPassword.getText());
        if (userOpt.isPresent()) {
            User loggedUser = userOpt.get();
            Session.setCurrentUser(loggedUser);
            messageLabel.setText("Вітаю, " + loggedUser.getUsername() + "!");
            switchScene("/fxml/main.fxml");
        } else {
            messageLabel.setText("Невірні дані входу.");
        }
    }

    @FXML
    private void changePassword() {
        User user = Session.getCurrentUser();
        if (user == null) {
            messageLabel.setText("Спочатку увійдіть у систему.");
            return;
        }
        try {
            userService.changePassword(user, oldPassword.getText(), newPassword.getText());
            messageLabel.setText("Пароль змінено.");
        } catch (Exception e) {
            messageLabel.setText("Помилка: " + e.getMessage());
        }
    }

    @FXML
    private void goBack() {
        switchScene("/fxml/main.fxml");
    }

    private void switchScene(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) messageLabel.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Main Menu");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
