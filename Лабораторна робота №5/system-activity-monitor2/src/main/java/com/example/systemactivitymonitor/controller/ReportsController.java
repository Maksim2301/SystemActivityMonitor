package com.example.systemactivitymonitor.controller;

import com.example.systemactivitymonitor.command.reports.*;
import com.example.systemactivitymonitor.model.Report;
import com.example.systemactivitymonitor.model.User;
import com.example.systemactivitymonitor.service.ReportService;
import com.example.systemactivitymonitor.util.Session;
import com.example.systemactivitymonitor.factory.EnvironmentFactoryProducer;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * ✅ Контролер для роботи зі звітами користувача.
 * Використовує шаблон Команда через ReportCommandManager.
 * Підтримує Undo для створення, видалення, експорту звітів.
 */
public class ReportsController {

    // ===================== FXML-поля =====================
    @FXML private TextField reportNameField;
    @FXML private DatePicker startDatePicker, endDatePicker;
    @FXML private Label messageLabel;
    @FXML private ListView<Report> reportsList;
    @FXML private Label avgCpuLabel, avgRamLabel, uptimeAvgLabel;
    @FXML private ListView<String> appUsageList;

    // ===================== Сервіси =====================
    private final ReportService reportService = EnvironmentFactoryProducer.getFactory().createReportService();
    private final Invoker commandManager = new Invoker();

    // ============================================================
    // 🔹 Ініціалізація інтерфейсу
    // ============================================================

    @FXML
    public void initialize() {
        reportsList.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Report report, boolean empty) {
                super.updateItem(report, empty);
                if (empty || report == null) {
                    setText(null);
                } else {
                    String uptime = report.getFilePath() != null && report.getFilePath().contains("Середній аптайм:")
                            ? report.getFilePath().split("Середній аптайм:")[1].trim()
                            : "—";
                    setText(String.format(
                            "%s\n%s → %s\n⚙ CPU: %s%% | 💾 RAM: %s MB\n%s",
                            report.getReportName(),
                            report.getPeriodStart(),
                            report.getPeriodEnd(),
                            report.getCpuAvg(),
                            report.getRamAvg(),
                            uptime
                    ));
                }
            }
        });
        reportsList.setPlaceholder(new Label("Звітів поки немає."));
    }

    // ============================================================
    // 🧾 Генерація нового звіту
    // ============================================================

    @FXML
    private void generateReport() {
        User user = Session.getCurrentUser();
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();

        if (user == null || start == null || end == null) {
            messageLabel.setText("⚠️ Заповніть усі поля.");
            return;
        }

        try {
            ReportCommand cmd = new GenerateReportCommand(reportService, user, reportNameField.getText(), start, end);
            commandManager.executeCommand(cmd);
            refreshReports(user);
            messageLabel.setText("✅ Звіт успішно створено.");
        } catch (Exception e) {
            messageLabel.setText("❌ Помилка створення: " + e.getMessage());
        }
    }

    // ============================================================
    // 🗑 Видалення звіту
    // ============================================================

    @FXML
    private void deleteReport() {
        Report selected = reportsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            messageLabel.setText("⚠️ Виберіть звіт для видалення.");
            return;
        }

        try {
            ReportCommand cmd = new DeleteReportCommand(reportService, selected.getId());
            commandManager.executeCommand(cmd);
            reportsList.getItems().remove(selected);
            appUsageList.getItems().clear();
            messageLabel.setText("🗑 Звіт \"" + selected.getReportName() + "\" видалено.");
        } catch (Exception e) {
            messageLabel.setText("❌ Помилка видалення: " + e.getMessage());
        }
    }

    // ============================================================
    // 📤 Експорт звітів (CSV / Excel / PDF)
    // ============================================================

    @FXML private void exportCSV() { exportSelected("csv"); }
    @FXML private void exportExcel() { exportSelected("excel"); }
    @FXML private void exportPDF() { exportSelected("pdf"); }

    private void exportSelected(String format) {
        Report selected = reportsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            messageLabel.setText("⚠️ Виберіть звіт для експорту.");
            return;
        }

        try {
            ReportCommand cmd = new ExportReportCommand(reportService, selected, format);
            commandManager.executeCommand(cmd);
            messageLabel.setText("📤 Експортовано у форматі " + format.toUpperCase());
        } catch (Exception e) {
            messageLabel.setText("❌ Помилка експорту: " + e.getMessage());
        }
    }

    // ============================================================
    // ⏪ Скасування останньої дії
    // ============================================================

    @FXML
    private void undoLastCommand() {
        commandManager.undoLastCommand();
        messageLabel.setText("↩️ Скасовано останню дію.");
        User user = Session.getCurrentUser();
        if (user != null) refreshReports(user);
    }

    // ============================================================
    // 📊 Перегляд / фільтрація звітів
    // ============================================================

    @FXML
    private void showReports() {
        User user = Session.getCurrentUser();
        if (user == null) {
            messageLabel.setText("⚠️ Спочатку увійдіть у систему.");
            return;
        }

        refreshReports(user);
        messageLabel.setText("📋 Завантажено звіти користувача.");
    }

    @FXML
    private void filterReportsByDate() {
        User user = Session.getCurrentUser();
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();

        if (user == null || start == null || end == null) {
            messageLabel.setText("⚠️ Виберіть дати для фільтрації.");
            return;
        }

        try {
            List<Report> filtered = reportService.getReportsInPeriod(user, start, end);
            reportsList.getItems().setAll(filtered);
            if (!filtered.isEmpty()) displayReportDetails(filtered.getLast());
            messageLabel.setText("📅 Відфільтровано " + filtered.size() + " звіт(ів).");
        } catch (Exception e) {
            messageLabel.setText("❌ Помилка фільтрації: " + e.getMessage());
        }
    }

    // ============================================================
    // 🧩 Детальна статистика (CPU/RAM по годинах)
    // ============================================================

    @FXML
    private void showDetailedStats() {
        User user = Session.getCurrentUser();
        Report selected = reportsList.getSelectionModel().getSelectedItem();

        if (user == null || selected == null) {
            messageLabel.setText("⚠️ Виберіть звіт для перегляду.");
            return;
        }

        try {
            String details = reportService.getCpuAndRamReport(user, selected);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("📊 Детальна статистика");
            alert.setHeaderText(selected.getReportName());
            alert.setContentText(details);
            alert.getDialogPane().setPrefWidth(450);
            alert.getDialogPane().setPrefHeight(400);
            alert.showAndWait();
        } catch (Exception e) {
            messageLabel.setText("❌ Помилка відображення: " + e.getMessage());
        }
    }

    // ============================================================
    // 🧮 Відображення деталей звіту
    // ============================================================

    private void displayReportDetails(Report report) {
        avgCpuLabel.setText(String.format("⚙ CPU: %.2f%%", report.getCpuAvg()));
        avgRamLabel.setText(String.format("💾 RAM: %.2f MB", report.getRamAvg()));
        uptimeAvgLabel.setText(report.getFilePath() != null && report.getFilePath().contains("Середній аптайм:")
                ? report.getFilePath().split("Середній аптайм:")[1].trim()
                : "—");

        appUsageList.getItems().clear();
        Map<String, BigDecimal> appUsage = report.getAppUsagePercent();
        if (appUsage != null && !appUsage.isEmpty()) {
            appUsageList.getItems().addAll(appUsage.entrySet().stream()
                    .map(e -> String.format("%s → %.2f %%", e.getKey(), e.getValue()))
                    .toList());
        } else {
            appUsageList.getItems().add("Немає даних про використання програм.");
        }
    }

    // ============================================================
    // 🔙 Повернення до головного меню
    // ============================================================

    @FXML
    private void goBack() {
        switchScene("/fxml/main.fxml", "Main Menu");
    }

    private void switchScene(String fxml, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) messageLabel.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle(title);
        } catch (Exception e) {
            messageLabel.setText("❌ Помилка переходу: " + e.getMessage());
        }
    }

    // ============================================================
    // 🔁 Оновлення списку звітів
    // ============================================================

    private void refreshReports(User user) {
        try {
            List<Report> reports = reportService.getReportsByUser(user);
            reportsList.getItems().setAll(reports);
            if (!reports.isEmpty()) displayReportDetails(reports.getLast());
        } catch (Exception e) {
            messageLabel.setText("❌ Помилка оновлення: " + e.getMessage());
        }
    }
}
