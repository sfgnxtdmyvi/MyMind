package myMind;

import javafx.application.Application;
import javafx.stage.Stage;
import myMind.controller.MindMapController;

import java.util.List;

public class Launch extends Application {

    private static final List<String> STYLE_SHEETS = List.of(
            MindMapController.class.getResource("/css/base.css").toExternalForm(),
            MindMapController.class.getResource("/css/menu.css").toExternalForm(),
            MindMapController.class.getResource("/css/node.css").toExternalForm(),
            MindMapController.class.getResource("/css/style-wheel.css").toExternalForm(),
            MindMapController.class.getResource("/css/title-bar.css").toExternalForm()
    );

    @Override
    public void start(Stage primaryStage) {
        new MindMapController().createMindMap(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
