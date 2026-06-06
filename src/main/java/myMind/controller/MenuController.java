package myMind.controller;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.stage.Stage;
import lombok.Setter;
import myMind.Launch;
import myMind.componet.MindMap;
import myMind.componet.MindNode;
import myMind.componet.Subject;
import myMind.constants.PosConstants;
import myMind.util.FileUtil;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class MenuController {

    @Setter
    private SubjectController subjectController;
    @Setter
    private MindMap mindMap;

    @FXML
    private Menu recentFilesMenu;

    //—————————————————————————————————————————文件—————————————————————————————————————————
    @FXML
    public void newMindMap() {
        Launch.createMindMap(new Stage());
    }

    @FXML
    private void load() {
        FileUtil.load(mindMap);
        selectFirstSubject();
    }

    @FXML
    private void loadRecentFiles() {
        ObservableList<MenuItem> items = recentFilesMenu.getItems();
        items.clear();

        LinkedList<String> recentFiles = FileUtil.getRecentFiles();
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
                menuItem.setOnAction(event -> {
                    FileUtil.loadFile(file, mindMap);
                    selectFirstSubject();
                });
                items.add(menuItem);
            } else {
                iterator.remove();
                changed = true;
            }
        }
        if (changed) {
            FileUtil.saveRecentFiles();
        }
    }

    private void selectFirstSubject() {
        // 选中第一个主题
        Tab tab = mindMap.getTabs().get(0);
        Subject firstSubject = (Subject) tab.getContent();
        SubjectController subjectController = (SubjectController) tab.getUserData();
        mindMap.setSubjectController(subjectController);
        mindMap.setSubject(firstSubject);
        this.subjectController = subjectController;
        StyleWheelArcController.setSubjectController(subjectController);
    }

    @FXML
    private void save() {
        FileUtil.save(mindMap);
    }

    @FXML
    public void saveAs() {
        FileUtil.saveAs(mindMap);
    }

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
        subjectController = mindMap.getSubjectController();
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
        MindNode selectedNode = subjectController.getSelectedNode();
        // 父节点 <- 右边节点
        // 中间的左子节点 <- 左边、根节点
        if (selectedNode.getPos() == PosConstants.RIGHT) {
            subjectController.setSelectedNode(selectedNode.getNodeParent());
        } else {
            List<MindNode> children = selectedNode.getChildrenL();
            if (!children.isEmpty()) {
                subjectController.setSelectedNode(children.get(children.size() / 2));
            }
        }
    }

    @FXML
    private void moveUp() {
        MindNode selectedNode = subjectController.getSelectedNode();
        if (selectedNode.getPos() == PosConstants.MIDDLE) {
            return;
        }

        MindNode parentModel = selectedNode.getNodeParent();
        List<MindNode> children;
        if (selectedNode.getPos() == PosConstants.RIGHT) {
            children = parentModel.getChildrenR();
        } else {
            children = parentModel.getChildrenL();
        }
        int index = children.indexOf(selectedNode);
        if (index != 0) {
            subjectController.setSelectedNode(children.get(index - 1));
        }
    }

    @FXML
    private void moveDown() {
        MindNode selectedNode = subjectController.getSelectedNode();
        if (selectedNode.getPos() == PosConstants.MIDDLE) {
            return;
        }

        MindNode parentModel = selectedNode.getNodeParent();
        List<MindNode> children;
        if (selectedNode.getPos() == PosConstants.RIGHT) {
            children = parentModel.getChildrenR();
        } else {
            children = parentModel.getChildrenL();
        }
        int index = children.indexOf(selectedNode);
        if (index != children.size() - 1) {
            subjectController.setSelectedNode(children.get(index + 1));
        }
    }
}
