package com.example.systemactivitymonitor.controller;

import com.example.systemactivitymonitor.command.*;
import com.example.systemactivitymonitor.command.reports.*;
import com.example.systemactivitymonitor.factory.RepositoryFactory;
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

    // UI
    @FXML private TextField reportNameField;
    @FXML private DatePicker startDatePicker, endDatePicker;
    @FXML private Label messageLabel;

    @FXML private ListView<Report> reportsList;

    @FXML private Label avgCpuLabel;
    @FXML private Label avgRamLabel;
    @FXML private Label uptimeAvgLabel;

    @FXML private ListView<String> appUsageList;

    // Services
    private final ReportService reportService;
    private final Invoker commandManager = new Invoker();

    // Constructor DI
    public ReportsController() {
        this.reportService = new ReportService(
                RepositoryFactory.getReportRepository(),
                RepositoryFactory.getStatsRepository(),
                RepositoryFactory.getIdleRepository()
        );
    }

    // ========================================================================
    // Initialization
    // ========================================================================
    @FXML
    public void initialize() {

        reportsList.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Report report, boolean empty) {
                super.updateItem(report, empty);

                if (empty || report == null) {
                    setText(null);
                    return;
                }

                setText(String.format(
                        "%s\n📅 %s → %s\n⚙ CPU: %.2f%% | 💾 RAM: %.2f MB\n⏱ Avg uptime: %.2f h/day",
                        report.getReportName(),
                        report.getPeriodStart(),
                        report.getPeriodEnd(),
                        report.getCpuAvg(),
                        report.getRamAvg(),
                        report.getAvgUptimeHours()
                ));
            }
        });

        reportsList.setPlaceholder(new Label("Звітів поки немає."));
    }

    // ========================================================================
    // Generate report
    // ========================================================================
    @FXML
    private void generateReport() {

        User user = Session.getCurrentUser();
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();
        String name = reportNameField.getText();

        if (user == null || start == null || end == null || name.isBlank()) {
            messageLabel.setText("⚠ Заповніть усі поля.");
            return;
        }

        ReportCommand cmd = new GenerateReportCommand(reportService, user, name, start, end);

        if (commandManager.executeAndStore(cmd)) {
            refreshReports(user);
            messageLabel.setText("✅ Звіт створено.");
        } else {
            messageLabel.setText("❌ Не вдалося створити звіт.");
        }
    }

    // ========================================================================
    // Delete report
    // ========================================================================
    @FXML
    private void deleteReport() {

        Report selected = reportsList.getSelectionModel().getSelectedItem();

        if (selected == null) {
            messageLabel.setText("⚠ Виберіть звіт.");
            return;
        }

        ReportCommand cmd = new DeleteReportCommand(reportService, selected.getId());

        if (commandManager.executeAndStore(cmd)) {
            refreshReports(Session.getCurrentUser());
            messageLabel.setText("🗑 Звіт видалено.");
        } else {
            messageLabel.setText("❌ Не вдалося видалити звіт.");
        }
    }

    // ========================================================================
    // Export report
    // ========================================================================
    @FXML private void exportCSV() { exportSelected("csv"); }
    @FXML private void exportExcel() { exportSelected("excel"); }
    @FXML private void exportPDF() { exportSelected("pdf"); }

    private void exportSelected(String format) {

        Report selected = reportsList.getSelectionModel().getSelectedItem();

        if (selected == null) {
            messageLabel.setText("⚠ Виберіть звіт.");
            return;
        }

        ReportCommand cmd = new ExportReportCommand(reportService, selected, format);

        if (commandManager.executeAndStore(cmd)) {
            messageLabel.setText("📤 Експортовано (" + format.toUpperCase() + ")");
        } else {
            messageLabel.setText("❌ Помилка експорту.");
        }
    }

    // ========================================================================
    // Undo
    // ========================================================================
    @FXML
    private void undoLastCommand() {

        if (commandManager.undoLastCommand()) {
            messageLabel.setText("↩ Скасовано останню дію.");
            User user = Session.getCurrentUser();
            if (user != null) refreshReports(user);
        } else {
            messageLabel.setText("⚠ Немає команд для скасування.");
        }
    }

    // ========================================================================
    // Show reports
    // ========================================================================
    @FXML
    private void showReports() {

        User user = Session.getCurrentUser();

        if (user == null) {
            messageLabel.setText("⚠ Увійдіть у систему.");
            return;
        }

        refreshReports(user);
        messageLabel.setText("📋 Звіти оновлено.");
    }

    // ========================================================================
    // Filter reports by dates
    // ========================================================================
    @FXML
    private void filterReportsByDate() {

        User user = Session.getCurrentUser();
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();

        if (user == null || start == null || end == null) {
            messageLabel.setText("⚠ Виберіть дати.");
            return;
        }

        List<Report> filtered = reportService.getReportsInPeriod(user, start, end);

        reportsList.getItems().setAll(filtered);

        if (!filtered.isEmpty()) {
            displayReportDetails(filtered.get(filtered.size() - 1));
        }

        messageLabel.setText("📅 Знайдено: " + filtered.size());
    }

    // ========================================================================
    // Detailed hourly stats via ReportService
    // ========================================================================
    @FXML
    private void showDetailedStats() {

        Report report = reportsList.getSelectionModel().getSelectedItem();
        User user = Session.getCurrentUser();

        if (report == null || user == null) {
            messageLabel.setText("⚠ Виберіть звіт.");
            return;
        }

        String details = reportService.getCpuAndRamReport(user, report);

        Alert popup = new Alert(Alert.AlertType.INFORMATION);
        popup.setTitle("Детальна статистика");
        popup.setHeaderText(report.getReportName());
        popup.setContentText(details);
        popup.getDialogPane().setPrefWidth(420);
        popup.getDialogPane().setPrefHeight(480);
        popup.showAndWait();
    }

    // ========================================================================
    // Display selected report details
    // ========================================================================
    private void displayReportDetails(Report report) {

        avgCpuLabel.setText(String.format("⚙ CPU: %.2f%%", report.getCpuAvg()));
        avgRamLabel.setText(String.format("💾 RAM: %.2f MB", report.getRamAvg()));
        uptimeAvgLabel.setText(String.format("⏱ Avg uptime: %.2f h/day", report.getAvgUptimeHours()));

        appUsageList.getItems().clear();

        Map<String, BigDecimal> apps = report.getAppUsagePercent();
        if (apps == null || apps.isEmpty()) {
            appUsageList.getItems().add("Немає даних");
            return;
        }

        apps.forEach((k, v) ->
                appUsageList.getItems().add(String.format("%s → %.2f%%", k, v))
        );
    }

    // ========================================================================
    // Navigation
    // ========================================================================
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
            messageLabel.setText("❌ " + e.getMessage());
        }
    }

    // ========================================================================
    // Refresh list
    // ========================================================================
    private void refreshReports(User user) {
        try {
            List<Report> list = reportService.getReportsByUser(user);
            reportsList.getItems().setAll(list);

            if (!list.isEmpty()) {
                displayReportDetails(list.get(list.size() - 1));
            }

        } catch (Exception e) {
            messageLabel.setText("❌ " + e.getMessage());
        }
    }
}
