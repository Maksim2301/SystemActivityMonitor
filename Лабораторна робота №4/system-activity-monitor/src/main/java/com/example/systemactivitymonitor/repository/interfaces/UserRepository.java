package com.example.systemactivitymonitor.repository.interfaces;

import com.example.systemactivitymonitor.model.User;
import java.util.Optional;

/**
 * Репозиторій для роботи з таблицею користувачів (users).
 * Виконує CRUD-операції: створення, пошук, оновлення, видалення.
 */
public interface UserRepository {

    /** Створює або оновлює користувача */
    void save(User user);

    /** Знаходить користувача за ім'ям */
    Optional<User> findByUsername(String username);

    /** Оновлює лише пароль користувача */
    void updatePassword(Integer userId, String newPasswordHash);

    /** Видаляє користувача за ID */
    void deleteById(Integer id);
}
