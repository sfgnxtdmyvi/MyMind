package myMind;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import myMind.constants.SizeConstants;
import myMind.controller.MindController;

public class MindApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        MindController controller = new MindController();

        BorderPane root = new BorderPane();
        root.setTop(controller.createToolBar());
        root.setCenter(controller.getMindMapPane());
        root.getStyleClass().add("root");

        Scene scene = new Scene(root, 1450, 740);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.setTitle("MyMind");
        primaryStage.setMaximized(true);
        primaryStage.show();

        // 窗口显示后，使用实际MindMapPane大小初始化根节点并居中
        javafx.application.Platform.runLater(() -> {
            double centerX = (controller.getMindMapPane().getWidth()) / 2 - SizeConstants.MIN_NODE_WIDTH;
            double centerY = (controller.getMindMapPane().getHeight() - SizeConstants.MIN_NODE_HEIGHT) / 2;
            controller.initRootNode(centerX, centerY);
        });

        // 窗口大小改变时重新计算连线 （结点位置可能相对变化？结点是绝对布局，不受窗口缩放影响，但为了安全）
        primaryStage.widthProperty().addListener((obs, oldVal, newVal) -> controller.refreshLines());
        primaryStage.heightProperty().addListener((obs, oldVal, newVal) -> controller.refreshLines());
    }

    public static void main(String[] args) {
        launch(args);
    }
}