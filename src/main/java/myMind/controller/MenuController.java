package myMind.controller;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.Setter;
import myMind.App;
import myMind.componet.NodeModel;
import myMind.componet.Workspace;
import myMind.constants.PosConstants;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;

public class MenuController {

    private static SubjectController subjectController;
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

    public static void setSubjectController(SubjectController subjectController) {
        MenuController.subjectController = subjectController;
    }

    //—————————————————————————————————————————文件—————————————————————————————————————————
    @FXML
    public void handleNew() {
        App.newMyMind(new Stage());
    }

    @FXML
    private void handleLoad() {
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
        FileChooser fc = new FileChooser();
        fc.setInitialDirectory(new File("C:\\Users\\k8255\\Documents\\MindLine"));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("MyMind Files", "*.mm"));
        File file = fc.showOpenDialog(subjectController.getSubject().getScene().getWindow());
        if (file != null) {
            fileHandler.importFile(file);
        }
    }

    //—————————————————————————————————————————编辑—————————————————————————————————————————
    //—————————————————————————————————————————添加—————————————————————————————————————————
    @FXML
    private void handleAddChildR() {
        
        subjectController.addChildR();
    }

    @FXML
    private void handleAddChildL() {
        
        subjectController.addChildL();
    }

    @FXML
    private void handleAddSibling() {
        
        subjectController.addSibling();
    }

    /**
     * 1个子节点和5个孙节点
     */
    @FXML
    private void handleBatchAddChildR() {
        if (subjectController.getSelectedModel().getPos() == PosConstants.LEFT) {
            return;
        }
        subjectController.addChildR();
        subjectController.addChildR();
        for (int i = 0; i < 4; i++) {
            subjectController.addSiblingR();
        }
    }

    @FXML
    private void handleBatchAddChildL() {
        if (subjectController.getSelectedModel().getPos() == PosConstants.RIGHT) {
            return;
        }
        subjectController.addChildL();
        subjectController.addChildL();
        for (int i = 0; i < 4; i++) {
            subjectController.addSiblingL();
        }
    }

    /**
     * 1个兄弟节点和5个孙节点
     */
    @FXML
    private void handleBatchAddSibling() {
        subjectController.addSibling();
        subjectController.addChildR();
        subjectController.addChildL();
        for (int i = 0; i < 4; i++) {
            subjectController.addSibling();
        }
    }

    @FXML
    private void handleCopy() {
        subjectController.copy();
    }

    //—————————————————————————————————————————复制粘贴—————————————————————————————————————————
    @FXML
    private void handleCut() {
        subjectController.cut();
    }

    @FXML
    private void handleDelete() {
        subjectController.delete();
    }

    @FXML
    private void handleAddSubject() {
        workspace.addSubject();
    }

    //—————————————————————————————————————————切换选中节点—————————————————————————————————————————
    @FXML
    private void handleMoveRight() {
        NodeModel selectedModel = subjectController.getSelectedModel();
        // 左边节点 -> 父节点
        // 根、右边节点 -> 中间的右子节点
        if (selectedModel.getPos() == PosConstants.LEFT) {
            subjectController.setSelectedModel(selectedModel.getParent());
        } else {
            List<NodeModel> children = selectedModel.getChildrenR();
            if (!children.isEmpty()) {
                subjectController.setSelectedModel(children.get(children.size() / 2));
            }
        }
    }

    @FXML
    private void handleMoveLeft() {
        NodeModel selectedModel = subjectController.getSelectedModel();
        // 父节点 <- 右边节点
        // 中间的左子节点 <- 左边、根节点
        if (selectedModel.getPos() == PosConstants.RIGHT) {
            subjectController.setSelectedModel(selectedModel.getParent());
        } else {
            List<NodeModel> children = selectedModel.getChildrenL();
            if (!children.isEmpty()) {
                subjectController.setSelectedModel(children.get(children.size() / 2));
            }
        }
    }

    @FXML
    private void handleMoveUp() {
        NodeModel selectedModel = subjectController.getSelectedModel();
        if (selectedModel.getPos() == PosConstants.MIDDLE) {
            return;
        }

        NodeModel parentModel = selectedModel.getParent();
        List<NodeModel> children;
        if (selectedModel.getPos() == PosConstants.RIGHT) {
            children = parentModel.getChildrenR();
        } else {
            children = parentModel.getChildrenL();
        }
        int index = children.indexOf(selectedModel);
        if (index != 0) {
            subjectController.setSelectedModel(children.get(index - 1));
        }
    }

    @FXML
    private void handleMoveDown() {
        NodeModel selectedModel = subjectController.getSelectedModel();

        if (selectedModel.getPos() == PosConstants.MIDDLE) {
            return;
        }

        NodeModel parentModel = selectedModel.getParent();
        List<NodeModel> children;
        if (selectedModel.getPos() == PosConstants.RIGHT) {
            children = parentModel.getChildrenR();
        } else {
            children = parentModel.getChildrenL();
        }
        int index = children.indexOf(selectedModel);
        if (index != children.size() - 1) {
            subjectController.setSelectedModel(children.get(index + 1));
        }
    }
}
