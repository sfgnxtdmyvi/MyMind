package myMind.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.stage.FileChooser;

import java.io.File;

public class MenuController {

    private NodeController nodeController;
    private FileHandler fileHandler;

    public void setControllers(NodeController nodeController, FileHandler fileHandler) {
        this.nodeController = nodeController;
        this.fileHandler = fileHandler;
    }

    //—————————————————————————————————————————文件—————————————————————————————————————————
    @FXML
    public void handleNew(ActionEvent actionEvent) {
    }

    @FXML
    private void handleLoad() {
        FileChooser fc = new FileChooser();
        fc.setInitialDirectory(new File("C:\\Users\\k8255\\Documents\\MindLine"));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("MyMind Files", "*.mm"));
        File file = fc.showOpenDialog(nodeController.getSubject().getScene().getWindow());
        if (file != null) {
            fileHandler.loadFromFile(file);
        }
    }

    @FXML
    public void handleLoadRecently(ActionEvent actionEvent) {
    }

    @FXML
    private void handleSave() {
        FileChooser fc = new FileChooser();
        fc.setInitialFileName("mindmap.json");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("MyMind Files", "*.mm"));
        File file = fc.showSaveDialog(nodeController.getSubject().getScene().getWindow());
        if (file != null) {
            fileHandler.saveToFile(file);
        }
    }

    @FXML
    public void handleSaveAs(ActionEvent actionEvent) {

    }

    @FXML
    private void handleImport() {
        FileChooser fc = new FileChooser();
        fc.setInitialDirectory(new File("C:\\Users\\k8255\\Documents\\MindLine"));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("MyMind Files", "*.mm"));
        File file = fc.showOpenDialog(nodeController.getSubject().getScene().getWindow());
        if (file != null) {
            fileHandler.importFile(file);
        }
    }

    //—————————————————————————————————————————编辑—————————————————————————————————————————
    @FXML
    private void handleAddChild(){
        nodeController.addChildR(null);
    }

    @FXML
    private void handleDelete() {
        nodeController.delete();
    }
}
