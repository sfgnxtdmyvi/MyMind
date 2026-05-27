package myMind.controller;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.Setter;
import myMind.App;
import myMind.componet.Workspace;

import java.io.File;
import java.util.LinkedList;
import java.util.ResourceBundle;

public class MenuController {

    private SubjectController subjectController;
    @Setter
    private FileHandler fileHandler;
    @Setter
    private Workspace workspace;

    @FXML
    private Menu recentFilesMenu;

    private static String dirFiles;

    static {
        ResourceBundle config = ResourceBundle.getBundle("config");
        dirFiles = config.getString("directory.files");
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
        fc.setInitialDirectory(new File(dirFiles));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("MyMind Files", "*.mm"));
        File file = fc.showOpenDialog(subjectController.getSubject().getScene().getWindow());
        if (file != null) {
            fileHandler.loadFile(file);
        }
    }

    /**
     * 加载最近文件
     */
    @FXML
    public void initialize() {
        recentFilesMenu.setOnShowing(event -> loadRecentFiles());
    }

    private void loadRecentFiles() {
        ObservableList<MenuItem> items = recentFilesMenu.getItems();
        items.clear();

        LinkedList<String> recentFiles = fileHandler.getRecentFiles();
        if (recentFiles.isEmpty()) {
            items.add(new MenuItem("无最近文件"));
            return;
        }

        for (String recentFile : recentFiles) {
            MenuItem menuItem = new MenuItem(recentFile);
            menuItem.setOnAction(event -> fileHandler.loadFile(new File(dirFiles + recentFile)));
            items.add(menuItem);
        }
    }

    @FXML
    private void handleSave() {
        Stage stage = (Stage) workspace.getScene().getWindow();
        String title = stage.getTitle();
        // 没有保存过，就保存到新建文件，并设置标题
        if (title == null) {
            String fileName = handleSaveAs();
            if (fileName != null) {
                stage.setTitle(fileName.substring(0, fileName.length() - 3));
            }
        } else {
            File file = new File(dirFiles + title + ".mm");
            fileHandler.saveFile(file);
        }
    }

    @FXML
    public String handleSaveAs() {
        subjectController = workspace.getCurrentController();

        FileChooser fc = new FileChooser();
        fc.setInitialDirectory(new File(dirFiles));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("MyMind Files", "*.mm"));
        File file = fc.showSaveDialog(subjectController.getSubject().getScene().getWindow());
        if (file != null) {
            fileHandler.saveFile(file);
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
            fileHandler.importFile(file);
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
