package com.example.systemactivitymonitor.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL = "jdbc:mysql://localhost:3306/system_activity_monitor?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "admin";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("MySQL драйвер успішно завантажено!");
        } catch (ClassNotFoundException e) {
            System.err.println("Не знайдено MySQL драйвер: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
                // System.out.println("З’єднання з БД закрито.");
            } catch (SQLException e) {
                System.err.println("Помилка при закритті з’єднання: " + e.getMessage());
            }
        }
    }
}
