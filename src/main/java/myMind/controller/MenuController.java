package myMind.controller;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.Setter;
import myMind.componet.MindMap;
import myMind.componet.MindNode;
import myMind.constants.PosConstants;
import myMind.util.FileHandler;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;

public class MenuController {

    private static SubjectController subjectController;
    @Setter
    private FileHandler fileHandler;
    @Setter
    private MindMap mindMap;

    @FXML
    private Menu recentFilesMenu;

    private static final String dirFiles;

    static {
        ResourceBundle config = ResourceBundle.getBundle("config");
        dirFiles = config.getString("directory.files");
    }

    public static void setSubjectController(SubjectController subjectController) {
        MenuController.subjectController = subjectController;
    }

    //—————————————————————————————————————————文件—————————————————————————————————————————
    @FXML
    public void newMindMap() {
        new MindMap(new Stage());
    }

    @FXML
    private void load() {
        FileChooser fc = new FileChooser();
        fc.setInitialDirectory(new File(dirFiles));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("MyMind Files", "*.mm"));
        File file = fc.showOpenDialog(subjectController.getSubject().getScene().getWindow());
        if (file != null) {
            fileHandler.loadFile(file);
        }
    }

    @FXML
    private void loadRecentFiles() {
        ObservableList<MenuItem> items = recentFilesMenu.getItems();
        items.clear();

        LinkedList<String> recentFiles = FileHandler.getRecentFiles();
        if (recentFiles.isEmpty()) {
            items.add(new MenuItem("无最近文件"));
            return;
        }

        Iterator<String> iterator = recentFiles.iterator();
        boolean changed = false;
        while (iterator.hasNext()) {
            String[] split = iterator.next().split("=");
            File file = new File(split[1]);
            if (file.exists()) {
                MenuItem menuItem = new MenuItem(split[0]);
                menuItem.setOnAction(event -> fileHandler.loadFile(file));
                items.add(menuItem);
            } else {
                iterator.remove();
                changed = true;
            }
        }
        if (changed) {
            fileHandler.saveRecentFiles();
        }
    }

    @FXML
    private void save() {
        // 没有保存过，就保存到新建文件
        if (mindMap.getFilePath() == null) {
            saveAs();
        } else {
            fileHandler.saveFile(new File(mindMap.getFilePath()));
        }
    }

    @FXML
    public void saveAs() {
        FileChooser fc = new FileChooser();
        fc.setInitialDirectory(new File(dirFiles));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("MyMind Files", "*.mm"));
        File file = fc.showSaveDialog(subjectController.getSubject().getScene().getWindow());

        // 取消时，file 为 null
        if (file != null) {
            fileHandler.saveFile(file);

            if (mindMap.getFilePath() != null) {
                fileHandler.CancelSchedule(mindMap.getFilePath());
            }
            String absolutePath = file.getAbsolutePath();
            fileHandler.scheduleAutoSave(absolutePath);

            fileHandler.addRecentFile(file);
            mindMap.setFilePath(absolutePath);
            Stage stage = (Stage) mindMap.getScene().getWindow();
            stage.setTitle(file.getName().substring(0, file.getName().length() - 3));
        }
    }

//    @FXML
//    private void importMindMap() {
//        FileChooser fc = new FileChooser();
//        fc.setInitialDirectory(new File("C:\\Users\\k8255\\Documents\\MindLine"));
//        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("MyMind Files", "*.mm"));
//        File file = fc.showOpenDialog(subjectController.getSubject().getScene().getWindow());
//        if (file != null) {
//            fileHandler.importFile(file);
//        }
//    }
//
//    @FXML
//    private void importMindMapBatch() throws IOException {
//        File rootFile = new File("C:\\Users\\k8255\\Documents\\MindLine");
//        importMindMapBatch(rootFile);
//    }
//
//    private void importMindMapBatch(File parentFile) throws IOException {
//        for (File file : parentFile.listFiles()) {
//            if (file.isDirectory()) {
//                importMindMapBatch(file);
//            } else if (file.getName().endsWith(".mm")) {
//                fileHandler.importFile(file);
//                File saveFile = new File(dirFiles + file.getPath().replace("C:\\Users\\k8255\\Documents\\MindLine\\", ""));
//                saveFile.getParentFile().mkdirs();
//                saveFile.createNewFile();
//                fileHandler.saveFile(saveFile);
//            }
//        }
//    }

    //—————————————————————————————————————————添加—————————————————————————————————————————
    @FXML
    private void addChildR() {
        subjectController.addChildR();
    }

    @FXML
    private void addChildL() {
        subjectController.addChildL();
    }

    @FXML
    private void addSibling() {
        subjectController.addSibling();
    }

    /**
     * 1个子节点和5个孙节点
     */
    @FXML
    private void batchAddChildR() {
        if (subjectController.getSelectedNode().getPos() == PosConstants.LEFT) {
            return;
        }
        subjectController.addChildR();
        subjectController.addChildR();
        for (int i = 0; i < 4; i++) {
            subjectController.addSiblingR();
        }
    }

    @FXML
    private void batchAddChildL() {
        if (subjectController.getSelectedNode().getPos() == PosConstants.RIGHT) {
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
    private void batchAddSibling() {
        subjectController.addSibling();
        subjectController.addChildR();
        subjectController.addChildL();
        for (int i = 0; i < 4; i++) {
            subjectController.addSibling();
        }
    }

    //—————————————————————————————————————————复制粘贴—————————————————————————————————————————
    @FXML
    private void copy() {
        subjectController.copy();
    }

    @FXML
    private void cut() {
        subjectController.cut();
    }

    @FXML
    private void delete() {
        subjectController.delete();
    }

    @FXML
    private void addSubject() {
        mindMap.addSubject();
    }

    //—————————————————————————————————————————切换选中节点—————————————————————————————————————————
    @FXML
    private void moveRight() {
        MindNode selectedNode = subjectController.getSelectedNode();
        // 左边节点 -> 父节点
        // 根、右边节点 -> 中间的右子节点
        if (selectedNode.getPos() == PosConstants.LEFT) {
            subjectController.setSelectedNode(selectedNode.getNodeParent());
        } else {
            List<MindNode> children = selectedNode.getChildrenR();
            if (!children.isEmpty()) {
                subjectController.setSelectedNode(children.get(children.size() / 2));
            }
        }
    }

    @FXML
    private void moveLeft() {
        MindNode selectedModel = subjectController.getSelectedNode();
        // 父节点 <- 右边节点
        // 中间的左子节点 <- 左边、根节点
        if (selectedModel.getPos() == PosConstants.RIGHT) {
            subjectController.setSelectedNode(selectedModel.getNodeParent());
        } else {
            List<MindNode> children = selectedModel.getChildrenL();
            if (!children.isEmpty()) {
                subjectController.setSelectedNode(children.get(children.size() / 2));
            }
        }
    }

    @FXML
    private void moveUp() {
        MindNode selectedModel = subjectController.getSelectedNode();
        if (selectedModel.getPos() == PosConstants.MIDDLE) {
            return;
        }

        MindNode parentModel = selectedModel.getNodeParent();
        List<MindNode> children;
        if (selectedModel.getPos() == PosConstants.RIGHT) {
            children = parentModel.getChildrenR();
        } else {
            children = parentModel.getChildrenL();
        }
        int index = children.indexOf(selectedModel);
        if (index != 0) {
            subjectController.setSelectedNode(children.get(index - 1));
        }
    }

    @FXML
    private void moveDown() {
        MindNode selectedModel = subjectController.getSelectedNode();

        if (selectedModel.getPos() == PosConstants.MIDDLE) {
            return;
        }

        MindNode parentModel = selectedModel.getNodeParent();
        List<MindNode> children;
        if (selectedModel.getPos() == PosConstants.RIGHT) {
            children = parentModel.getChildrenR();
        } else {
            children = parentModel.getChildrenL();
        }
        int index = children.indexOf(selectedModel);
        if (index != children.size() - 1) {
            subjectController.setSelectedNode(children.get(index + 1));
        }
    }
}
