package myMind.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.Setter;
import myMind.componet.Workspace;

import java.io.File;
import java.util.ResourceBundle;

public class MenuController {

    private SubjectController subjectController;

    @Setter
    private Workspace workspace;

    private static String fileDir;

    static {
        ResourceBundle config = ResourceBundle.getBundle("config");
        fileDir = config.getString("directory.file");
    }

    //—————————————————————————————————————————文件—————————————————————————————————————————
    @FXML
    public void handleNew(ActionEvent actionEvent) {
    }

    @FXML
    private void handleLoad() {
        subjectController = workspace.getCurrentController();

        FileChooser fc = new FileChooser();
        fc.setInitialDirectory(new File("D:\\MyMind"));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("MyMind Files", "*.mm"));
        File file = fc.showOpenDialog(subjectController.getSubject().getScene().getWindow());
        if (file != null) {
            FileHandler.loadFile(file);
        }
    }

    @FXML
    public void handleLoadRecently(ActionEvent actionEvent) {
    }

    @FXML
    private void handleSave() {
        Stage stage = (Stage) workspace.getScene().getWindow();
        File file = new File(fileDir + stage.getTitle() + ".mm");
        FileHandler.saveToFile(file);
    }

    @FXML
    public void handleSaveAs() {
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

        subjectController.addChildR();
    }

    @FXML
    private void handleDelete() {
        subjectController = workspace.getCurrentController();

        subjectController.delete();
    }
}
