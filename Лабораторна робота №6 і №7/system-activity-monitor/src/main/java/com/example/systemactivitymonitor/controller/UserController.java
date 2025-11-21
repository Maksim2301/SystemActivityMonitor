package com.example.systemactivitymonitor.controller;

import com.example.systemactivitymonitor.factory.RepositoryFactory;
import com.example.systemactivitymonitor.model.User;
import com.example.systemactivitymonitor.service.UserService;
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

    private final UserService userService;

    public UserController() {
        // 🔥 ВИКОРИСТОВУЄМО ФАБРИКУ
        this.userService = new UserService(
                RepositoryFactory.getUserRepository()
        );
    }

    // ==========================================================================
    // REGISTER
    // ==========================================================================
    @FXML
    private void registerUser() {
        try {
            userService.registerUser(
                    registerUsername.getText(),
                    registerPassword.getText(),
                    registerEmail.getText()
            );

            messageLabel.setText("✅ Користувач успішно створений!");

        } catch (Exception e) {
            messageLabel.setText("❌ " + e.getMessage());
        }
    }

    // ==========================================================================
    // LOGIN
    // ==========================================================================
    @FXML
    private void loginUser() {
        Optional<User> userOpt = userService.login(
                loginUsername.getText(),
                loginPassword.getText()
        );

        if (userOpt.isPresent()) {
            User loggedUser = userOpt.get();
            Session.setCurrentUser(loggedUser);

            messageLabel.setText("Вітаю, " + loggedUser.getUsername() + "!");
            switchScene("/fxml/main.fxml");

        } else {
            messageLabel.setText("❌ Невірне ім’я або пароль.");
        }
    }

    // ==========================================================================
    // CHANGE PASSWORD
    // ==========================================================================
    @FXML
    private void changePassword() {
        User user = Session.getCurrentUser();

        if (user == null) {
            messageLabel.setText("❌ Спочатку увійдіть у систему.");
            return;
        }

        String oldPass = oldPassword.getText();
        String newPass = newPassword.getText();

        if (oldPass.isBlank() || newPass.isBlank()) {
            messageLabel.setText("⚠ Обидва поля мають бути заповнені.");
            return;
        }

        try {
            userService.changePassword(user, oldPass, newPass);

            // 🔥 Оновлюємо користувача в сесії
            Session.setCurrentUser(user);

            messageLabel.setText("🔐 Пароль успішно змінено!");

        } catch (SecurityException e) {
            messageLabel.setText("❌ Старий пароль неправильний.");
        } catch (Exception e) {
            messageLabel.setText("❌ Помилка: " + e.getMessage());
        }
    }

    // ==========================================================================
    // DELETE ACCOUNT
    // ==========================================================================
    @FXML
    private void deleteMyAccount() {

        User user = Session.getCurrentUser();
        if (user == null) {
            messageLabel.setText("❌ Ви не ввійшли у систему.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Підтвердження");
        confirm.setHeaderText("Видалити акаунт?");
        confirm.setContentText("Цю дію не можна буде скасувати.");

        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                userService.deleteUser(user.getId());
                Session.logout();

                messageLabel.setText("🗑 Акаунт видалено.");
                switchScene("/fxml/user.fxml");

            } catch (Exception e) {
                messageLabel.setText("❌ Помилка: " + e.getMessage());
            }
        } else {
            messageLabel.setText("Скасовано.");
        }
    }

    // ==========================================================================
    // GUEST LOGIN
    // ==========================================================================
    @FXML
    private void loginAsGuest() {
        Session.setGuestMode();
        messageLabel.setText("🔓 Увійдено як гість.");
        switchScene("/fxml/main.fxml");
    }

    // ==========================================================================
    // BACK
    // ==========================================================================
    @FXML
    private void goBack() {
        switchScene("/fxml/main.fxml");
    }

    // ==========================================================================
    // Scene switching
    // ==========================================================================
    private void switchScene(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Scene scene = new Scene(loader.load());

            Stage stage = (Stage) messageLabel.getScene().getWindow();
            stage.setScene(scene);

        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setText("❌ Помилка переходу між сценами.");
        }
    }
}
