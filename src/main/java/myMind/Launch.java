package myMind;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import myMind.common.util.IOUtil;
import myMind.controller.MindMapController;

import java.io.File;
import java.util.LinkedList;

public class Launch extends Application {

    @Override
    public void start(Stage primaryStage) {
        LinkedList<String> recentFiles = IOUtil.getRecentFiles();
        MindMapController mindMapController = new MindMapController();
        if (recentFiles.isEmpty()) {
            mindMapController.createMindMap(primaryStage, true);
        } else {
            // 打开上一次打开的导图
            mindMapController.createMindMap(primaryStage, false);
            Platform.runLater(() -> {
                String[] split = recentFiles.getFirst().split("=");
                IOUtil.load(new File(split[1]), mindMapController.getMindMap());
                mindMapController.getTitleBarController().selectFirstSubject();
            });
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

}
