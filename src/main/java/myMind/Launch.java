package myMind;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import myMind.common.util.FileUtil;
import myMind.controller.MindMapController;

import java.io.File;
import java.util.LinkedList;

public class Launch extends Application {

    @Override
    public void start(Stage primaryStage) {
        LinkedList<String> recentFiles = FileUtil.getRecentFiles();
        MindMapController mindMapController = new MindMapController();
        if (recentFiles.isEmpty()) {
            mindMapController.createMindMap(primaryStage, true);
        } else {
            // 打开上一次打开的导图
            mindMapController.createMindMap(primaryStage, false);
            Platform.runLater(() -> {
                String[] split = recentFiles.getFirst().split("=");
                FileUtil.load(new File(split[1]), mindMapController.getMindMap());
                mindMapController.getTitleBarController().selectFirstSubject();
            });
        }

//        StyleClassedTextArea textArea = new StyleClassedTextArea();
//        textArea.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
//        textArea.setWrapText(true);
//        textArea.richChanges().subscribe(change -> {
//            // 强制让 prefWidth 重算
//            textArea.requestLayout();
//            // 有些 JavaFX 版本需要显式让宽度缓存失效
//            textArea.setPrefWidth(Region.USE_COMPUTED_SIZE);
//        });
//
//        VBox contentBox = new VBox(textArea);
//        contentBox.setPadding(new Insets(20));
//        contentBox.setAlignment(Pos.TOP_LEFT);
//        contentBox.setStyle("-fx-background-color: #f0f0f0;");
//        contentBox.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
//
//        StackPane mapNode = new StackPane(contentBox);
//        mapNode.setStyle("-fx-background-color: #cccccc;");
//
//        Pane root = new Pane(mapNode);
//
//        Scene scene = new Scene(root, 800, 600);
//        primaryStage.setScene(scene);
//        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

}
