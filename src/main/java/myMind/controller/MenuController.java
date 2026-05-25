package myMind.controller;

import javafx.fxml.FXML;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.Setter;
import myMind.App;
import myMind.componet.Workspace;

import java.io.File;
import java.util.ResourceBundle;

public class MenuController {

    private SubjectController subjectController;

    @Setter
    private Workspace workspace;

    private static String rootDir;

    static {
        ResourceBundle config = ResourceBundle.getBundle("config");
        rootDir = config.getString("directory.root");
    }

    //—————————————————————————————————————————文件—————————————————————————————————————————
    @FXML
    public void handleNew() {
        App.newMyMind(new Stage());
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
    public void handleLoadRecently() {
    }

    @FXML
    private void handleSave() {
        Stage stage = (Stage) workspace.getScene().getWindow();
        String title = stage.getTitle();
        // 没有保存过，就保存到新建文件，并设置标题
        if (title == null) {
            String fileName = handleSaveAs();
            if (fileName == null) {
                stage.setTitle(fileName.substring(0, fileName.length() - 3));
            }
        } else {
            File file = new File(rootDir + title + ".mm");
            FileHandler.saveFile(file);
        }
    }

    @FXML
    public String handleSaveAs() {
        subjectController = workspace.getCurrentController();

        FileChooser fc = new FileChooser();
        fc.setInitialDirectory(new File(rootDir));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("MyMind Files", "*.mm"));
        File file = fc.showSaveDialog(subjectController.getSubject().getScene().getWindow());
        if (file != null) {
            FileHandler.saveFile(file);
            return file.getName();
        }

        return null;
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
        subjectController.addChild();
    }

    @FXML
    private void handleDelete() {
        subjectController = workspace.getCurrentController();
        subjectController.delete();
    }
}
