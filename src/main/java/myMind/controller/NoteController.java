package myMind.controller;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.TextFieldTreeCell;
import myMind.constants.ConfigConstants;

import java.io.File;

public class NoteController {

    @FXML
    private TreeView<String> treeView;

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
