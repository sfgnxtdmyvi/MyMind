package myMind;

import javafx.application.Application;
import javafx.stage.Stage;
import myMind.controller.MindMapController;

public class Launch extends Application {

    @Override
    public void start(Stage primaryStage) {
        new MindMapController().createMindMap(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
