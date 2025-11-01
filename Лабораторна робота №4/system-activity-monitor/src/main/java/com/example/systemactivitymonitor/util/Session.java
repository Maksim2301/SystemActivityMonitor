package com.example.systemactivitymonitor.util;

import com.example.systemactivitymonitor.model.User;

/**
 * Клас для управління поточною сесією користувача.
 * Тепер підтримує також гостьовий режим (guest mode).
 */
public class Session {
    private static User currentUser;
    private static boolean guestMode = false; // 🆕 прапорець гостьового режиму

    /** Встановлення поточного користувача (звичайний вхід) */
    public static void setCurrentUser(User user) {
        currentUser = user;
        guestMode = false;
    }

    /** Увімкнення гостьового режиму (без входу в систему) */
    public static void setGuestMode() {
        currentUser = null;
        guestMode = true;
    }

    /** Перевірка, чи активний гостьовий режим */
    public static boolean isGuest() {
        return guestMode;
    }

    /** Перевірка, чи користувач увійшов */
    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    /** Отримання поточного користувача */
    public static User getCurrentUser() {
        return currentUser;
    }

    /** Вихід із системи або з гостьового режиму */
    public static void logout() {
        currentUser = null;
        guestMode = false;
    }
}
