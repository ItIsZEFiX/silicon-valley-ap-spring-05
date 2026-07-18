package com.techcartel.siliconvalley;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class GameApp extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        // Create a loader and point it to our FXML file
        FXMLLoader fxmlLoader = new FXMLLoader(GameApp.class.getResource("main-layout.fxml"));

        // Load the FXML. This returns the root node (the BorderPane)
        Scene scene = new Scene(fxmlLoader.load(), 1024, 768);

        primaryStage.setTitle("Silicon Valley: The Tech Cartel");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}