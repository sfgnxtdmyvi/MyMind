package myMind;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import myMind.componet.Workspace;
import myMind.controller.MenuController;

import java.io.IOException;

public class App extends Application {
    private MenuController menuController;
    private Workspace workspace;

    @Override
    public void start(Stage primaryStage) {
        workspace = new Workspace();

        BorderPane root = new BorderPane();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/menu.fxml"));
            MenuBar menuBar = loader.load();
            menuController = loader.getController();
            menuController.setWorkspace(workspace);
            root.setTop(menuBar);
        } catch (IOException e) {
            e.printStackTrace();
        }

        root.setCenter(workspace);
        root.getStyleClass().add("workspace");

        Scene scene = new Scene(root, 1450, 740);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.setTitle("MyMind");
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}