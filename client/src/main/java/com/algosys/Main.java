package com.algosys;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.net.URL;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/com/algosys/main.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1280, 820);
        URL stylesheet = Main.class.getResource("/com/algosys/styles.css");
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }

        stage.initStyle(StageStyle.UNDECORATED);
        stage.setTitle("Algorithm Performance Evaluator");
        stage.setScene(scene);
        stage.setWidth(1280);
        stage.setHeight(820);
        stage.setMinWidth(980);
        stage.setMinHeight(680);
        stage.setMaximized(false);
        stage.setFullScreen(false);
        stage.centerOnScreen();
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
