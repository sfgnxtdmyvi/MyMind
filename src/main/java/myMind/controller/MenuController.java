package myMind.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.stage.FileChooser;

import java.io.File;

public class MenuController {

    private NodeController nodeController;
    private FileController fileController;

    public void setControllers(NodeController nodeController, FileController fileController) {
        this.nodeController = nodeController;
        this.fileController = fileController;
    }

    //—————————————————————————————————————————文件—————————————————————————————————————————
    @FXML
    public void handleNew(ActionEvent actionEvent) {
    }

    @FXML
    private void handleLoad() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON 文件", "*.json"));
        File file = fc.showOpenDialog(nodeController.getMindMapPane().getScene().getWindow());
        if (file != null) {
            fileController.loadFromFile(file);
        }
    }

    @FXML
    public void handleLoadRecently(ActionEvent actionEvent) {
    }

    @FXML
    private void handleSave() {
        FileChooser fc = new FileChooser();
        fc.setInitialFileName("mindmap.json");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON 文件", "*.json"));
        File file = fc.showSaveDialog(nodeController.getMindMapPane().getScene().getWindow());
        if (file != null) {
            fileController.saveToFile(file);
        }
    }

    @FXML
    public void handleSaveAs(ActionEvent actionEvent) {

    }

    //—————————————————————————————————————————编辑—————————————————————————————————————————
    @FXML
    private void handleAddChild() {
        nodeController.addChild();
    }

    @FXML
    private void handleDelete() {
        nodeController.delete();
    }
}
