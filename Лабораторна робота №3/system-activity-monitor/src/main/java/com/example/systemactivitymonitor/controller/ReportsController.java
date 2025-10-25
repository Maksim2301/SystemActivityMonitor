package com.example.systemactivitymonitor.controller;

import com.example.systemactivitymonitor.model.Report;
import com.example.systemactivitymonitor.model.User;
import com.example.systemactivitymonitor.service.impl.ReportServiceImpl;
import com.example.systemactivitymonitor.util.Session;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.List;

public class ReportsController {

    @FXML private TextField reportNameField;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private Label messageLabel;
    @FXML private TableView<Report> reportsTable;

    @FXML private TableColumn<Report, Integer> idColumn;
    @FXML private TableColumn<Report, String> nameColumn;
    @FXML private TableColumn<Report, String> periodColumn;
    @FXML private TableColumn<Report, String> cpuColumn;
    @FXML private TableColumn<Report, String> ramColumn;

    private final ReportServiceImpl reportService = new ReportServiceImpl();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getId()));
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getReportName()));
        periodColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getPeriodStart() + " → " + data.getValue().getPeriodEnd()
        ));
        cpuColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCpuAvg() + "%"));
        ramColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRamAvg() + " MB"));
    }

    @FXML
    private void generateReport() {
        if (Session.getCurrentUser() == null) {
            messageLabel.setText("Спочатку увійдіть у систему!");
            return;
        }

        User activeUser = Session.getCurrentUser();
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();

        if (start == null || end == null) {
            messageLabel.setText("Виберіть дати.");
            return;
        }

        Report r = reportService.generateReport(activeUser, reportNameField.getText(), start, end);
        messageLabel.setText("Звіт створено: " + r.getReportName());
    }

    @FXML
    private void showReports() {
        if (Session.getCurrentUser() == null) {
            messageLabel.setText("Спочатку увійдіть у систему!");
            return;
        }

        List<Report> reports = reportService.getReportsByUser(Session.getCurrentUser());
        reportsTable.getItems().setAll(reports);
        messageLabel.setText("Звітів знайдено: " + reports.size());
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
            e.printStackTrace();
        }
    }
}
