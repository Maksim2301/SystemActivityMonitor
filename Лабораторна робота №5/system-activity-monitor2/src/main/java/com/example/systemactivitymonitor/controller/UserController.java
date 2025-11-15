package com.example.systemactivitymonitor.controller;

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

    private final UserService userService = new UserService();

    /** Реєстрація нового користувача */
    @FXML
    private void registerUser() {
        try {
            userService.registerUser(
                    registerUsername.getText(),
                    registerPassword.getText(),
                    registerEmail.getText()
            );
            messageLabel.setText("Користувач створений!");
        } catch (Exception e) {
            messageLabel.setText("Помилка: " + e.getMessage());
        }
    }

    /** Вхід у систему */
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
            messageLabel.setText("Невірні дані входу.");
        }
    }

    /** Зміна пароля через */
    @FXML
    private void changePassword() {
        User user = Session.getCurrentUser();
        if (user == null) {
            messageLabel.setText("Спочатку увійдіть у систему.");
            return;
        }

        String oldPass = oldPassword.getText();
        String newPass = newPassword.getText();

        if (oldPass.isBlank() || newPass.isBlank()) {
            messageLabel.setText("Обидва поля повинні бути заповнені.");
            return;
        }

        if (oldPass.equals(newPass)) {
            messageLabel.setText("Новий пароль не може збігатися зі старим.");
            return;
        }

        try {
            userService.changePassword(user, oldPass, newPass);
            messageLabel.setText("Пароль успішно оновлено!");
        } catch (SecurityException e) {
            messageLabel.setText("Старий пароль неправильний.");
        } catch (Exception e) {
            messageLabel.setText("Помилка при зміні пароля: " + e.getMessage());
        }
    }

    /** Видалення акаунта користувача */
    @FXML
    private void deleteMyAccount() {
        User user = Session.getCurrentUser();
        if (user == null) {
            messageLabel.setText("Ви не ввійшли у систему.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Підтвердження видалення");
        confirm.setHeaderText("Видалення акаунта");
        confirm.setContentText("Ви впевнені, що хочете видалити свій акаунт? Цю дію не можна скасувати.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                userService.deleteUser(user.getId());
                Session.logout();
                messageLabel.setText("🗑 Акаунт видалено. Ви вийшли із системи.");
                switchScene("/fxml/user.fxml");
            } catch (Exception e) {
                messageLabel.setText(" Помилка при видаленні: " + e.getMessage());
            }
        } else {
            messageLabel.setText("Видалення скасовано.");
        }
    }

    /** Вхід як гість */
    @FXML
    private void loginAsGuest() {
        Session.setGuestMode();
        messageLabel.setText("Увійдено як гість. Дані не зберігаються у базу.");
        switchScene("/fxml/main.fxml");
    }

    /** Повернення до головного меню */
    @FXML
    private void goBack() {
        switchScene("/fxml/main.fxml");
    }

    /** Перехід між сценами */
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
