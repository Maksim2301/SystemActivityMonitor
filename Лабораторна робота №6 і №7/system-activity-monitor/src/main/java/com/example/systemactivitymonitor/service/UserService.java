package com.example.systemactivitymonitor.service;

import com.example.systemactivitymonitor.model.User;
import com.example.systemactivitymonitor.repository.interfaces.UserRepository;

import java.util.Optional;

/**
 * UserService — фасад для роботи з користувачами.
 * Як просили: без хешування паролів, без email-валідації.
 */
public class UserService {

    private final UserRepository userRepository;

    // ======================================================
    // CONSTRUCTOR (Dependency Injection)
    // ======================================================
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // ======================================================
    // REGISTER
    // ======================================================
    public User registerUser(String username, String plainPassword, String email) {

        if (username == null || username.isBlank())
            throw new IllegalArgumentException("Ім’я користувача не може бути порожнім.");

        if (plainPassword == null || plainPassword.isBlank())
            throw new IllegalArgumentException("Пароль не може бути порожнім.");

        User user = new User(username, plainPassword, email);
        userRepository.save(user);

        System.out.println("✅ Користувача '" + username + "' зареєстровано.");
        return user;
    }

    // ======================================================
    // LOGIN
    // ======================================================
    public Optional<User> login(String username, String password) {

        if (username == null || username.isBlank())
            return Optional.empty();

        if (password == null || password.isBlank())
            return Optional.empty();

        Optional<User> found = userRepository.findByUsername(username);

        if (found.isPresent()) {
            User user = found.get();

            // Паролі без хешування — пряме порівняння
            if (password.equals(user.getPasswordHash())) {
                return Optional.of(user);
            }
        }

        return Optional.empty();
    }

    // ======================================================
    // CHANGE PASSWORD
    // ======================================================
    public void changePassword(User user, String oldPassword, String newPassword) {

        validateUser(user);

        if (!user.getPasswordHash().equals(oldPassword))
            throw new SecurityException("Неправильний старий пароль.");

        // Оновлюємо пароль у БД
        userRepository.updatePassword(user.getId(), newPassword);

        // Оновлюємо об'єкт в пам’яті
        user.setPasswordHash(newPassword);

        System.out.println("🔐 Пароль оновлено.");
    }

    // ======================================================
    // DELETE USER
    // ======================================================
    public void deleteUser(Integer id) {
        if (id == null)
            throw new IllegalArgumentException("ID не може бути null.");

        userRepository.deleteById(id);

        System.out.println("🗑 Користувача видалено: ID=" + id);
    }

    // ======================================================
    // Helper
    // ======================================================
    private void validateUser(User user) {
        if (user == null || user.getId() == null)
            throw new IllegalArgumentException("Користувач не збережений або null.");
    }
}
