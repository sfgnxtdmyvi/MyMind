package myMind.controller;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import lombok.Getter;
import lombok.Setter;
import myMind.Launch;
import myMind.componet.MindMap;
import myMind.componet.MindNode;
import myMind.componet.Subject;
import myMind.constants.CssStyle;
import myMind.constants.FileConstants;
import myMind.constants.PosConstants;
import myMind.manager.CssManager;
import myMind.util.FileUtil;
import myMind.util.MessageUtil;
import myMind.util.ScheduleUtil;

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
    private Menu recentFilesMenu;
    @FXML
    private Button maximizeBtn;
    @FXML
    private HBox titleBar;

    private AnchorPane root;
    private Stage mapStage;
    private MindMap mindMap;
    private SubjectController subjectController;

    private final Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
    private boolean maximized = false;
    // 最大化时，记录宽高位置，恢复时使用
    private double width;
    private double height;
    private double x;
    private double y;

    @FXML
    public void initialize() {
        Tooltip.install(icon, new Tooltip("打开笔记"));
        title.textProperty().bind(titleProperty);
    }

    @FXML
    public void openNote() {
        if (mindMap.isEmpty()) {
            mapStage.hide();
        }
        Launch.getNoteStage().show();
        Launch.getNoteStage().toFront();
    }

    //—————————————————————————————————————————文件—————————————————————————————————————————
    @FXML
    public void newMindMap() {
        Launch.createMindMap(new Stage());
    }

    @FXML
    private void load() {
        File file = FileUtil.openFileChooser(FileConstants.OPEN_TYPE, mindMap);
        if (file == null) {
            return;
        }
        load(file);
    }

    private void load(File file) {
        if (ScheduleUtil.containsScheduled(file.getAbsolutePath())) {
            // todo 跳转过去
            MessageUtil.showMessage("导图已打开");
            return;
        }

        MindNode rootNode = subjectController.getRootNode();
        if (mindMap.getFilePath() == null && rootNode.isEmpty()) {
            FileUtil.load(file, mindMap);
            selectFirstSubject();
        } else {
            Stage stage = new Stage();
            Launch.createMindMap(stage);
            FileUtil.load(file, Launch.getMindMap());
            Launch.getTitleBarController().selectFirstSubject();
            // 打开 FileChooser 可能会导致图标设置失败
            Platform.runLater(() -> {
                ObservableList<Image> icons = Launch.getMapStage().getIcons();
                icons.clear();
                icons.add(new Image(Launch.class.getResourceAsStream("/icon.png")));
            });
        }
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
                menuItem.setOnAction(event -> load(file));
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

    /**
     * 选中第一个主题
     */
    public void selectFirstSubject() {
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

    @FXML
    public void deleteUnusefulImage() {
        FileUtil.deleteUnusefulImage();
    }

    //—————————————————————————————————————————添加—————————————————————————————————————————
    @FXML
    private void insertChild() {
        subjectController.insertChild();
    }

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

    @FXML
    private void batchAddChildR() {
        subjectController.batchAddChildR();
    }

    @FXML
    private void batchAddChildL() {
        subjectController.batchAddChildL();
    }

    @FXML
    private void batchAddSibling() {
        subjectController.batchAddSibling();
    }

    //—————————————————————————————————————————其他—————————————————————————————————————————
    @FXML
    private void addSubject() {
        mindMap.addSubject();
        subjectController = mindMap.getSubjectController();
    }

    @FXML
    public void change() {
        if (root.getStyle().isEmpty()) {
            CssManager.setCss("tabStyle", CssStyle.TAB_LIGHT.getStyleName());
            CssManager.setTabStyle(CssStyle.TAB_LIGHT.getStyleName());
            for (Window window : Stage.getWindows()) {
                window.getScene().getRoot().setStyle(CssStyle.TAB_LIGHT.getValue());
            }
        } else {
            CssManager.setCss("tabStyle", "");
            CssManager.setTabStyle("");
            for (Window window : Stage.getWindows()) {
                window.getScene().getRoot().setStyle("");
            }
        }
    }

    //—————————————————————————————————————————切换选中节点—————————————————————————————————————————
    public void moveRight() {
        MindNode selectedNode = subjectController.getSelectedNode();
        // 左边节点 -> 父节点
        // 根、右边节点 -> 中间的右子节点
        if (selectedNode.getPos() == PosConstants.LEFT) {
            subjectController.setSelectedNode(selectedNode.getParentNode());
        } else {
            List<MindNode> children = selectedNode.getChildrenR();
            if (!children.isEmpty()) {
                subjectController.setSelectedNode(children.get(children.size() / 2));
            }
        }
    }

    public void moveLeft() {
        MindNode selectedNode = subjectController.getSelectedNode();
        // 父节点 <- 右边节点
        // 中间的左子节点 <- 左边、根节点
        if (selectedNode.getPos() == PosConstants.RIGHT) {
            subjectController.setSelectedNode(selectedNode.getParentNode());
        } else {
            List<MindNode> children = selectedNode.getChildrenL();
            if (!children.isEmpty()) {
                subjectController.setSelectedNode(children.get(children.size() / 2));
            }
        }
    }

    public void moveUp() {
        MindNode selectedNode = subjectController.getSelectedNode();
        if (selectedNode.getPos() == PosConstants.MIDDLE) {
            return;
        }

        MindNode parentModel = selectedNode.getParentNode();
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

    public void moveDown() {
        MindNode selectedNode = subjectController.getSelectedNode();
        if (selectedNode.getPos() == PosConstants.MIDDLE) {
            return;
        }

        MindNode parentModel = selectedNode.getParentNode();
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
        mapStage.setIconified(true);
    }

    @FXML
    public void maximize() {
        if (maximized) {
            // 还原
            mapStage.setX(x);
            mapStage.setY(y);
            mapStage.setWidth(width);
            mapStage.setHeight(height);
            maximizeBtn.setText("□");
        } else {
            // 最大化
            x = mapStage.getX();
            y = mapStage.getY();
            width = mapStage.getWidth();
            height = mapStage.getHeight();
            mapStage.setX(0);
            mapStage.setY(0);
            // UNDECORATED 模式下，setMaximized()会让窗口覆盖整个屏幕（包括任务栏）
            mapStage.setWidth(screenBounds.getWidth());
            mapStage.setHeight(screenBounds.getHeight());
            maximizeBtn.setText("❐");
        }
        maximized = !maximized;
    }

    // todo bug
    @FXML
    public void close() {
        // 未保存且不为空
        if (mindMap.getFilePath() == null && !mindMap.isEmpty()) {
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
                    for (Tab tab : mindMap.getTabs()) {
                        ObservableList<Node> children = ((Subject) tab.getContent()).getNodesLayer().getChildren();
                        for (Node child : children) {
                            MindNode node = (MindNode) child;
                            FileUtil.deleteImage(node.getImageName());
                        }
                    }
                }
                default -> {
                    return;
                }
            }
        }

        if (Stage.getWindows().size() <= 1) {
            ScheduleUtil.cancelSchedule();
        } else {
            if (mindMap.getFilePath() != null) {
                ScheduleUtil.cancelSchedule(mindMap.getFilePath());
            }
        }
        mapStage.close();
    }

}
