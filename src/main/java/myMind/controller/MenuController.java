package myMind.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.stage.FileChooser;
import lombok.Setter;
import myMind.componet.Workspace;

import java.io.File;

public class MenuController {

    private SubjectController subjectController;

    @Setter
    private Workspace workspace;

    //—————————————————————————————————————————文件—————————————————————————————————————————
    @FXML
    public void handleNew(ActionEvent actionEvent) {
    }

    @FXML
    private void handleLoad() {
        subjectController = workspace.getCurrentController();

        FileChooser fc = new FileChooser();
        fc.setInitialDirectory(new File("C:\\Users\\k8255\\Documents\\MindLine"));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("MyMind Files", "*.mm"));
        File file = fc.showOpenDialog(subjectController.getSubject().getScene().getWindow());
        if (file != null) {
            FileHandler.loadFromFile(file);
        }
    }

    @FXML
    public void handleLoadRecently(ActionEvent actionEvent) {
    }

    @FXML
    private void handleSave() {
        subjectController = workspace.getCurrentController();

        FileChooser fc = new FileChooser();
        fc.setInitialDirectory(new File("D:\\MyMind"));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("MyMind Files", "*.mm"));
        File file = fc.showSaveDialog(subjectController.getSubject().getScene().getWindow());
        if (file != null) {
            FileHandler.saveToFile(file);
        }
    }

    @FXML
    public void handleSaveAs(ActionEvent actionEvent) {

    }

    @FXML
    private void handleImport() {
        subjectController = workspace.getCurrentController();

        FileChooser fc = new FileChooser();
        fc.setInitialDirectory(new File("C:\\Users\\k8255\\Documents\\MindLine"));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("MyMind Files", "*.mm"));
        File file = fc.showOpenDialog(subjectController.getSubject().getScene().getWindow());
        if (file != null) {
            FileHandler.importFile(file);
        }
    }

    //—————————————————————————————————————————编辑—————————————————————————————————————————
    @FXML
    private void handleAddChild() {
        subjectController = workspace.getCurrentController();

        subjectController.addChildR(null);
    }

    @FXML
    private void handleDelete() {
        subjectController = workspace.getCurrentController();

        subjectController.delete();
    }
}
