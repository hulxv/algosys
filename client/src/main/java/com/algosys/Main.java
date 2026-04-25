package com.algosys;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        TextArea codeInput = new TextArea();
        codeInput.setPromptText("Paste your algorithm here...");
        codeInput.setPrefHeight(300);

        Button analyzeBtn = new Button("Analyze");
        analyzeBtn.setOnAction(e -> {
            // TODO: send codeInput.getText() to POST /analyze
        });

        Label resultLabel = new Label("Results will appear here.");

        VBox root = new VBox(10, new Label("Algorithm Code:"), codeInput, analyzeBtn, resultLabel);
        root.setPadding(new Insets(16));

        stage.setTitle("Algorithm Performance Evaluator");
        stage.setScene(new Scene(root, 640, 480));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
