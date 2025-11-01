package com.example.systemactivitymonitor.controller;

import com.example.systemactivitymonitor.model.Report;
import com.example.systemactivitymonitor.model.User;
import com.example.systemactivitymonitor.service.ReportService;
import com.example.systemactivitymonitor.util.Session;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class ReportsController {

    @FXML private TextField reportNameField;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private Label messageLabel;
    @FXML private ListView<Report> reportsList;
    @FXML private Label avgCpuLabel;
    @FXML private Label avgRamLabel;
    @FXML private Label uptimeAvgLabel;
    @FXML private ListView<String> appUsageList;

    private final ReportService reportService = new ReportService();

    @FXML
    public void initialize() {
        // ✅ Відображення короткого опису звіту (без CPU по годинах)
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

    @FXML
    private void generateReport() {
        User user = Session.getCurrentUser();
        if (user == null) {
            messageLabel.setText("Спочатку увійдіть у систему!");
            return;
        }

        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();
        if (start == null || end == null) {
            messageLabel.setText("Виберіть дати.");
            return;
        }

        try {
            Report report = reportService.generateReport(user, reportNameField.getText(), start, end);
            List<Report> reports = reportService.getReportsByUser(user);
            reportsList.getItems().setAll(reports);
            displayReportDetails(report);

            messageLabel.setText("✅ Звіт \"" + report.getReportName() + "\" створено.");
        } catch (Exception e) {
            messageLabel.setText("❌ Помилка при створенні звіту: " + e.getMessage());
        }
    }

    @FXML
    private void showReports() {
        User user = Session.getCurrentUser();
        if (user == null) {
            messageLabel.setText("Спочатку увійдіть у систему!");
            return;
        }

        try {
            List<Report> reports = reportService.getReportsByUser(user);
            reportsList.getItems().setAll(reports);

            if (!reports.isEmpty()) {
                displayReportDetails(reports.get(reports.size() - 1));
            }

            messageLabel.setText("Завантажено " + reports.size() + " звіт(ів).");
        } catch (Exception e) {
            messageLabel.setText("❌ Помилка при завантаженні звітів: " + e.getMessage());
        }
    }

    @FXML
    private void filterReportsByDate() {
        User user = Session.getCurrentUser();
        if (user == null) {
            messageLabel.setText("Спочатку увійдіть у систему!");
            return;
        }

        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();

        if (start == null || end == null) {
            messageLabel.setText("Виберіть дати для фільтрації.");
            return;
        }

        try {
            List<Report> filteredReports = reportService.getReportsInPeriod(user, start, end);
            reportsList.getItems().setAll(filteredReports);

            if (!filteredReports.isEmpty()) {
                displayReportDetails(filteredReports.get(filteredReports.size() - 1));
            }

            messageLabel.setText("Відфільтровано звіти за період: " + start + " → " + end);
        } catch (Exception e) {
            messageLabel.setText("❌ Помилка при фільтрації: " + e.getMessage());
        }
    }

    /** 🧾 Відкриття детального звіту (через ітератор) */
    @FXML
    private void showDetailedStats() {
        User user = Session.getCurrentUser();
        if (user == null) {
            messageLabel.setText("Спочатку увійдіть у систему!");
            return;
        }

        Report selected = reportsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            messageLabel.setText("⚠️ Виберіть звіт для перегляду статистики.");
            return;
        }

        try {
            String details = reportService.getCpuAndRamReport(user, selected); // ⬅ передаємо user
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Детальна статистика");
            alert.setHeaderText("📊 " + selected.getReportName());
            alert.setContentText(details);
            alert.getDialogPane().setPrefWidth(450);
            alert.getDialogPane().setPrefHeight(400);
            alert.showAndWait();
        } catch (Exception e) {
            messageLabel.setText("❌ Помилка при отриманні статистики: " + e.getMessage());
        }
    }

    @FXML
    private void deleteSelectedReport() {
        Report selected = reportsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            messageLabel.setText("⚠️ Виберіть звіт для видалення.");
            return;
        }

        try {
            reportService.deleteReport(selected.getId());
            reportsList.getItems().remove(selected);
            appUsageList.getItems().clear();
            messageLabel.setText("🗑 Звіт \"" + selected.getReportName() + "\" видалено.");
        } catch (Exception e) {
            messageLabel.setText("❌ Помилка при видаленні звіту: " + e.getMessage());
        }
    }

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
            messageLabel.setText("❌ Помилка при переході до " + title);
        }
    }
}
