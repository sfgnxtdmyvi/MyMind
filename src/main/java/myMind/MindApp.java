package myMind;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import myMind.constants.SizeConstants;
import myMind.controller.FileController;
import myMind.controller.MenuController;
import myMind.controller.NodeController;

import java.io.IOException;

public class MindApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        NodeController nodeController = new NodeController();
        FileController fileController = new FileController(nodeController);

        BorderPane root = new BorderPane();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/menu.fxml"));
            MenuBar menuBar = loader.load();
            MenuController menuController = loader.getController();
            menuController.setControllers(nodeController, fileController);
            root.setTop(menuBar);
        } catch (IOException e) {
            e.printStackTrace();
        }
        root.setCenter(nodeController.getMindMapPane());
        root.getStyleClass().add("root");

        Scene scene = new Scene(root, 1450, 740);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.setTitle("MyMind");
        primaryStage.setMaximized(true);
        primaryStage.show();

        // 窗口显示后，使用实际MindMapPane大小初始化根节点并居中
        javafx.application.Platform.runLater(() -> {
            double centerX = (nodeController.getMindMapPane().getWidth()) / 2 - SizeConstants.MIN_NODE_WIDTH;
            double centerY = (nodeController.getMindMapPane().getHeight() - SizeConstants.MIN_NODE_HEIGHT) / 2;
            nodeController.initRootNode(centerX, centerY);
        });

        // 窗口大小改变时重新计算连线 （结点位置可能相对变化？结点是绝对布局，不受窗口缩放影响，但为了安全）
        primaryStage.widthProperty().addListener((obs, oldVal, newVal) -> nodeController.refreshLines());
        primaryStage.heightProperty().addListener((obs, oldVal, newVal) -> nodeController.refreshLines());
    }

    public static void main(String[] args) {
        launch(args);
    }
}