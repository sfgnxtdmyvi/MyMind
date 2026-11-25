package myMind.controller;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import lombok.Getter;
import lombok.Setter;
import myMind.common.constants.CssStyle;
import myMind.common.constants.IOConstants;
import myMind.common.constants.PosConstants;
import myMind.common.manager.CssManager;
import myMind.common.util.IOUtil;
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
    private Menu recentFilesMenu;
    @FXML
    private MenuItem collapseOrExpandItem;

    private AnchorPane root;
    private Stage stage;
    private MindMap mindMap;
    private SubjectController subjectController;

    public StringProperty titleProperty() {
        return titleProperty;
    }

    @FXML
    public void initialize() {
        title.textProperty().bind(titleProperty);
    }

    //—————————————————————————————————————————文件—————————————————————————————————————————

    @FXML
    public void newMindMap() {
        new MindMapController().createMindMap(new Stage(), true);
    }

    @FXML
    private void load() {
        File file = IOUtil.openFileChooser(IOConstants.OPEN_TYPE, mindMap);
        if (file == null) {
            return;
        }
        load(file);
    }

    public void load(File file) {
        String path = file.getAbsolutePath();
        for (Window window : Stage.getWindows()) {
            if (path.equals(((MindMap) window.getScene().getRoot().getChildrenUnmodifiable().get(1)).getFilePath())) {
                // 导图已打开，跳转过去
                window.requestFocus();
                return;
            }
        }

        MindMapController mindMapController = new MindMapController();
        mindMapController.createMindMap(new Stage(), false);
        // 其他更新 UI 的操作，可能会导致图标不显示，所以放在下一帧执行
        Platform.runLater(() -> {
            IOUtil.load(file, mindMapController.getMindMap());
            mindMapController.getTitleBarController().selectFirstSubject();
        });
    }

    @FXML
    private void loadRecentFiles() {
        ObservableList<MenuItem> items = recentFilesMenu.getItems();
        items.clear();

        LinkedList<String> recentFiles = IOUtil.getRecentFiles();
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
            IOUtil.saveRecentFiles();
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
        IOUtil.save(mindMap);
    }

    @FXML
    public void saveAs() {
        IOUtil.saveAs(mindMap);
    }

    @FXML
    public void deleteUnusefulImage() {
        IOUtil.deleteUnusefulImage();
    }

    @FXML
    public void updateMap() {
        IOUtil.updateMap();
    }

    //—————————————————————————————————————————添加—————————————————————————————————————————
    @FXML
    private void addSubject() {
        mindMap.addSubject();
        subjectController = mindMap.getSubjectController();
        subjectController.saveLater();
    }

    @FXML
    private void insert() {
        subjectController.insert();
    }

    @FXML
    private void addChildR() {
        subjectController.addChild(PosConstants.RIGHT);
    }

    @FXML
    private void addChildL() {
        subjectController.addChild(PosConstants.LEFT);
    }

    @FXML
    private void addSibling() {
        subjectController.addSibling();
    }

    @FXML
    private void batchAddChildR() {
        subjectController.batchAddChild(PosConstants.RIGHT);
    }

    @FXML
    private void batchAddChildL() {
        subjectController.batchAddChild(PosConstants.LEFT);
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
