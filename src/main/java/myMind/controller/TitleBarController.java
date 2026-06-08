package myMind.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import lombok.Getter;
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

@Setter
public class TitleBarController {
    @FXML
    @Getter
    private ImageView icon;
    @FXML
    private Label title;
    private StringProperty titleProperty = new SimpleStringProperty();

    public StringProperty titleProperty() {
        return titleProperty;
    }

    @FXML
    public void initialize() {
        title.textProperty().bind(titleProperty);
    }

    @FXML
    private Menu recentFilesMenu;
    @FXML
    private Button maximizeBtn;
    @FXML
    private HBox titleBar;

    private Stage stage;
    private MindMap mindMap;
    private SubjectController subjectController;

    private final Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
    private boolean maximized = false;
    // 最大化时，记录宽高位置，恢复时使用
    private double width;
    private double height;
    private double x;
    private double y;

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
    private void addSubject() {
        mindMap.addSubject();
        subjectController = mindMap.getSubjectController();
    }

    // todo 抽取到快捷键管理类
    //—————————————————————————————————————————切换选中节点—————————————————————————————————————————
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

    public void registerGlobalAccelerators(Scene scene) {
        scene.getAccelerators().put(KeyCombination.keyCombination("Shift+Alt+Right"), this::moveRight);
        scene.getAccelerators().put(KeyCombination.keyCombination("Shift+Alt+Left"), this::moveLeft);
        scene.getAccelerators().put(KeyCombination.keyCombination("Shift+Alt+Up"), this::moveUp);
        scene.getAccelerators().put(KeyCombination.keyCombination("Shift+Alt+Down"), this::moveDown);
    }

    //—————————————————————————————————————————最小化、最大化、关闭—————————————————————————————————————————
    // todo 调整尺寸
    // todo 移动窗口
    @FXML
    public void clickBar(MouseEvent event) {
        if (event.getClickCount() == 2) {
            maximize();
        }
    }

    @FXML
    public void minimize() {
        stage.setIconified(true);
    }

    @FXML
    public void maximize() {
        if (maximized) {
            // 还原
            stage.setX(x);
            stage.setY(y);
            stage.setWidth(width);
            stage.setHeight(height);
            maximizeBtn.setText("□");
        } else {
            // 最大化
            x = stage.getX();
            y = stage.getY();
            width = stage.getWidth();
            height = stage.getHeight();
            stage.setX(0);
            stage.setY(0);
            // UNDECORATED 模式下，setMaximized()会让窗口覆盖整个屏幕（包括任务栏）
            stage.setWidth(screenBounds.getWidth());
            stage.setHeight(screenBounds.getHeight());
            maximizeBtn.setText("❐");
        }
        maximized = !maximized;
    }

    @FXML
    public void close() {
        if (mindMap.getFilePath() == null) {
            for (Tab tab : mindMap.getTabs()) {

                ObservableList<Node> children = ((Subject) tab.getContent()).getNodesLayer().getChildren();
                for (Node child : children) {
                    MindNode node = (MindNode) child;
                    FileUtil.deleteImage(node.getImageName());
                }
            }

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("保存导图");
            alert.setHeaderText(null);
            alert.setContentText("是否保存当前导图？");
            alert.getButtonTypes().setAll(new ButtonType("保存", ButtonBar.ButtonData.YES),
                    new ButtonType("不保存", ButtonBar.ButtonData.NO),
                    new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE));
            ButtonBar.ButtonData buttonData = alert.showAndWait().get().getButtonData();
            switch (buttonData) {
                case YES -> saveAs();
                case NO -> {
                }
                default -> {
                    return;
                }
            }
        }

        if (Stage.getWindows().size() <= 1) {
            FileUtil.cancelSchedule();
        } else {
            FileUtil.cancelSchedule(mindMap.getFilePath());
        }
        stage.close();
    }
}
