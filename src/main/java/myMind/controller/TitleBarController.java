package myMind.controller;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import lombok.Getter;
import lombok.Setter;
import myMind.common.constants.CssStyle;
import myMind.common.constants.FileConstants;
import myMind.common.constants.SizeConstants;
import myMind.common.manager.CssManager;
import myMind.common.util.FileUtil;
import myMind.common.util.MessageUtil;
import myMind.common.util.ScheduleUtil;
import myMind.componet.MindMap;
import myMind.componet.Subject;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;

@Setter
public class TitleBarController {
    @FXML
    @Getter
    private ImageView icon;
    @FXML
    private Label title;
    private StringProperty titleProperty = new SimpleStringProperty();

    @FXML
    private MenuItem collapseOrExpandItem;

    @FXML
    private Menu recentFilesMenu;
    @FXML
    private Button maximizeBtn;
    @FXML
    private HBox titleBar;

    private AnchorPane root;
    private Stage stage;
    private MindMap mindMap;
    private SubjectController subjectController;

    private boolean maximized = false;
    // 最大化时，记录宽高位置，恢复时使用
    private double width;
    private double height;
    private double x;
    private double y;

    public StringProperty titleProperty() {
        return titleProperty;
    }

    @FXML
    public void initialize() {
        Tooltip.install(icon, new Tooltip("打开笔记"));
        title.textProperty().bind(titleProperty);
    }

    @FXML
    public void openNote() {
        if (mindMap.isEmpty()) {
            Event.fireEvent(stage, new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
        }
        NoteController.createNote(new Stage());
    }

    //—————————————————————————————————————————文件—————————————————————————————————————————
    @FXML
    public void newMindMap() {
        new MindMapController().createMindMap(new Stage(), true);
    }

    @FXML
    private void load() {
        File file = FileUtil.openFileChooser(FileConstants.OPEN_TYPE, mindMap);
        if (file == null) {
            return;
        }
        load(file);
    }

    public void load(File file) {
        if (ScheduleUtil.containsScheduled(file.getAbsolutePath())) {
            // todo 跳转过去
            MessageUtil.showMessage("导图已打开");
            return;
        }

        MindMapController mindMapController = new MindMapController();
        mindMapController.createMindMap(new Stage(), false);
        // 其他更新 UI 的操作，可能会导致图标不显示，所以放在下一帧执行
        Platform.runLater(() -> {
            FileUtil.load(file, mindMapController.getMindMap());
            mindMapController.getTitleBarController().selectFirstSubject();
        });
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

    @FXML
    public void deletePeriod() {
        FileUtil.deletePeriod();
    }

    @FXML
    public void updateMap() {
        FileUtil.updateMap(mindMap);
    }

    //—————————————————————————————————————————添加—————————————————————————————————————————
    @FXML
    private void addSubject() {
        mindMap.addSubject();
        subjectController = mindMap.getSubjectController();
    }

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

    //—————————————————————————————————————————样式—————————————————————————————————————————
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

    @FXML
    public void collapseOrExpand() {
        if (collapseOrExpandItem.getUserData().equals("collapse")) {
            subjectController.collapseLeaf();
            collapseOrExpandItem.setText("展开叶子节点 ▼");
            collapseOrExpandItem.setAccelerator(KeyCombination.keyCombination("Ctrl+Shift+="));
            collapseOrExpandItem.setUserData("expand");
        } else {
            subjectController.expandLeaf();
            collapseOrExpandItem.setText("收起叶子节点 ▲");
            collapseOrExpandItem.setAccelerator(KeyCombination.keyCombination("Ctrl+Shift+-"));
            collapseOrExpandItem.setUserData("collapse");
        }
    }

    //—————————————————————————————————————————切换选中节点—————————————————————————————————————————
    public void moveRight() {
        subjectController.moveRight();
    }

    public void moveLeft() {
        subjectController.moveLeft();
    }

    public void moveUp() {
        subjectController.moveUp();
    }

    public void moveDown() {
        subjectController.moveDown();
    }

    //—————————————————————————————————————————标题栏—————————————————————————————————————————
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
            stage.setWidth(SizeConstants.SCREEN_WIDTH);
            stage.setHeight(SizeConstants.SCREEN_HEIGHT);
            maximizeBtn.setText("❐");
        }
        maximized = !maximized;
    }

    @FXML
    public void close() {
        Event.fireEvent(stage, new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
    }

    public void dispose() {
        recentFilesMenu.getItems().clear();
        collapseOrExpandItem.setOnAction(null);
        title.textProperty().unbind();
        titleProperty().unbind();

        this.stage = null;
        this.root = null;
        this.mindMap = null;
        this.subjectController = null;
    }
}
