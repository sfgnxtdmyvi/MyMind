package myMind;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import myMind.componet.Workspace;
import myMind.controller.FileHandler;
import myMind.controller.MenuController;
import myMind.util.MessageUtil;

import java.io.IOException;

public class App extends Application {
    private MenuController menuController;
    private Workspace workspace;

    @Override
    public void start(Stage primaryStage) {
        workspace = new Workspace();
        workspace.getStyleClass().add("workspace");
        BorderPane borderPane = new BorderPane();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/menu.fxml"));
            MenuBar menuBar = loader.load();
            menuController = loader.getController();
            menuController.setWorkspace(workspace);
            FileHandler.setWorkspace(workspace);
            borderPane.setTop(menuBar);

            borderPane.setCenter(workspace);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Pane 用来添加消息提示标签，BorderPane 无法手动指定位置
        Pane root = new Pane(borderPane);
        // Pane 不会自动调整子节点大小，需要手动绑定
        borderPane.prefWidthProperty().bind(root.widthProperty());
        borderPane.prefHeightProperty().bind(root.heightProperty());
        MessageUtil.init(root);

        Scene scene = new Scene(root, 1450, 740);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}