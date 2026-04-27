package com.algosys.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class MainController {

    @FXML private HBox titleBar;
    @FXML private Button closeBtn;
    @FXML private Button minBtn;
    @FXML private Button maxBtn;

    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    public void initialize() {
        // Drag window implementation
        titleBar.setOnMousePressed(e -> {
            Stage stage = (Stage) titleBar.getScene().getWindow();
            xOffset = stage.getX() - e.getScreenX();
            yOffset = stage.getY() - e.getScreenY();
        });

        titleBar.setOnMouseDragged(e -> {
            Stage stage = (Stage) titleBar.getScene().getWindow();
            stage.setX(e.getScreenX() + xOffset);
            stage.setY(e.getScreenY() + yOffset);
        });

        closeBtn.setOnAction(e -> Platform.exit());
        minBtn.setOnAction(e -> {
            Stage stage = (Stage) titleBar.getScene().getWindow();
            stage.setIconified(true);
        });
        maxBtn.setOnAction(e -> {
            Stage stage = (Stage) titleBar.getScene().getWindow();
            stage.setMaximized(!stage.isMaximized());
        });
    }
}