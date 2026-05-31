package myMind;

import javafx.application.Application;
import javafx.stage.Stage;
import myMind.componet.MindMap;

public class Launch extends Application {
    @Override
    public void start(Stage primaryStage) {
        new MindMap(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}