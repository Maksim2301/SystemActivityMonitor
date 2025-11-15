package com.example.systemactivitymonitor.service;

import com.example.systemactivitymonitor.model.User;
import com.example.systemactivitymonitor.repository.impl.UserRepositoryImpl;
import com.example.systemactivitymonitor.repository.interfaces.UserRepository;

import java.util.Optional;

/**
 * UserService — об’єднаний фасад для роботи з користувачами.
 * Містить як командну частину (створення, оновлення, видалення),
 * так і запитову (авторизація, читання).
 */
public class UserService {

    private final UserRepository userRepository = new UserRepositoryImpl();

    // ======================================================
    // 🔹 Виконавча частина (Command)
    // ======================================================

    /** 🧾 Реєстрація нового користувача */
    public User registerUser(String username, String plainPassword, String email) {
        if (username == null || username.isBlank())
            throw new IllegalArgumentException("Ім'я користувача не може бути порожнім.");
        if (plainPassword == null || plainPassword.isBlank())
            throw new IllegalArgumentException("Пароль не може бути порожнім.");

        User user = new User(username, plainPassword, email);
        userRepository.save(user);

        System.out.println("✅ Користувача '" + username + "' зареєстровано.");
        return user;
    }

    /** 🔒 Зміна пароля користувача */
    public void changePassword(User user, String oldPassword, String newPassword) {
        validateUser(user);

        if (!user.getPasswordHash().equals(oldPassword))
            throw new SecurityException("Старий пароль неправильний.");

        userRepository.updatePassword(user.getId(), newPassword);
        user.setPasswordHash(newPassword);

        System.out.println("🔐 Пароль користувача " + user.getUsername() + " змінено.");
    }

    /** 🗑 Видалення користувача */
    public void deleteUser(Integer id) {
        if (id == null)
            throw new IllegalArgumentException("ID користувача не може бути null.");
        userRepository.deleteById(id);
        System.out.println("🗑 Користувача з ID=" + id + " видалено.");
    }

    // ======================================================
    // 🔹 Запитова частина (Query)
    // ======================================================

    /** 🔐 Авторизація користувача */
    public Optional<User> login(String username, String plainPassword) {
        if (username == null || username.isBlank() || plainPassword == null || plainPassword.isBlank())
            return Optional.empty();

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent() && plainPassword.equals(userOpt.get().getPasswordHash()))
            return userOpt;

        return Optional.empty();
    }

    // ======================================================
    // 🔹 Приватна перевірка
    // ======================================================

    private void validateUser(User user) {
        if (user == null || user.getId() == null)
            throw new IllegalArgumentException("Користувач не заданий або не збережений (id == null).");
    }
}
