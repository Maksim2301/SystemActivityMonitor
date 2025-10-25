CREATE DATABASE IF NOT EXISTS system_activity_monitor CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE system_activity_monitor;

-- Створення бази даних (якщо її ще нема)
CREATE DATABASE IF NOT EXISTS system_activity_monitor
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE system_activity_monitor;

-- ==============================
-- 1️⃣ Таблиця користувачів
-- ==============================
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    created_at DATETIME NOT NULL
    );

-- ==============================
-- 2️⃣ Таблиця простоїв
-- ==============================
CREATE TABLE IF NOT EXISTS idle_time (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME,
    duration_seconds INT,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

-- ==============================
-- 3️⃣ Таблиця системної статистики
-- ==============================
CREATE TABLE IF NOT EXISTS system_stats (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    cpu_load DECIMAL(5,2),
    memory_usage_mb DECIMAL(10,2),
    active_window VARCHAR(255),
    keyboard_presses INT DEFAULT 0,
    mouse_clicks INT DEFAULT 0,
    recorded_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

-- ==============================
-- 4️⃣ Таблиця звітів
-- ==============================
CREATE TABLE IF NOT EXISTS reports (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    report_name VARCHAR(255),
    period_start DATE,
    period_end DATE,
    cpu_avg DECIMAL(5,2),
    ram_avg DECIMAL(10,2),
    idle_time_total_seconds DECIMAL(10,2),
    browser_usage_percent DECIMAL(5,2),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

