package myMind.controller;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.TextFieldTreeCell;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import myMind.Launch;
import myMind.common.constants.ConfigConstants;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class NoteController {

    @FXML
    private TreeView<String> treeView;

    private static final List<String> STYLE_SHEETS = List.of(
            Launch.class.getResource("/css/note.css").toExternalForm()
    );

    public static void createNote(Stage stage) {
        BorderPane borderPane = null;
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Launch.class.getResource("/fxml/note.fxml"));
            borderPane = fxmlLoader.load();
        } catch (
                IOException e) {
            e.printStackTrace();
        }

        Scene scene = new Scene(borderPane);
        scene.getStylesheets().addAll(STYLE_SHEETS);

        stage = new Stage();
        stage.setScene(scene);
        Image icon = new Image(Launch.class.getResourceAsStream("/icon.png"));
        stage.getIcons().add(icon);
        stage.setTitle("MyNote");
        stage.setWidth(1200);
        stage.setHeight(720);
        stage.setMaximized(true);

        stage.show();
        stage.toFront();
    }

    @FXML
    public void initialize() {
        File dirNote = new File(ConfigConstants.DIR_NOTES);
        TreeItem<String> rootItem = new TreeItem<>(dirNote.getName());
        treeView.setRoot(rootItem);
        treeView.setShowRoot(false);
        refreshItem(rootItem, dirNote);

        treeView.setEditable(true);
        treeView.setCellFactory(TextFieldTreeCell.forTreeView());
    }

    private static void refreshItem(TreeItem<String> parentItem, File dirNote) {
        ObservableList<TreeItem<String>> children = parentItem.getChildren();
        for (File file : dirNote.listFiles()) {
            TreeItem<String> childItem = new TreeItem<>(file.getName());
            children.add(childItem);
            if (file.isDirectory()) {
                refreshItem(childItem, file);
            }
        }
    }
}
