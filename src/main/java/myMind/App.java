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
    @Override
    public void start(Stage primaryStage) {
        newMyMind(primaryStage);
    }

    public static void newMyMind(Stage stage) {
        Workspace workspace = new Workspace();
        workspace.getStyleClass().add("workspace");
        BorderPane borderPane = new BorderPane();
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/menu.fxml"));
            MenuBar menuBar = loader.load();
            MenuController menuController = loader.getController();
            menuController.setWorkspace(workspace);
            menuController.setFileHandler(new FileHandler(workspace));
            borderPane.setTop(menuBar);

            borderPane.setCenter(workspace);
        } catch (IOException e) {
            e.printStackTrace();
        }

        stage.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                MenuController.setSubjectController(workspace.getSubjectController());
            }
        });

        // Pane 用来添加消息提示标签，BorderPane 无法手动指定位置
        Pane root = new Pane(borderPane);
        // Pane 不会自动调整子节点大小，需要手动绑定
        borderPane.prefWidthProperty().bind(root.widthProperty());
        borderPane.prefHeightProperty().bind(root.heightProperty());
        MessageUtil.init(root, stage);

        Scene scene = new Scene(root, 1450, 740);
        scene.getStylesheets().add(App.class.getResource("/css/style.css").toExternalForm());

        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}