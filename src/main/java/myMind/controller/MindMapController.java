package myMind.controller;

import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Tab;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.Data;
import myMind.common.constants.IOConstants;
import myMind.common.constants.StyleSheetConstants;
import myMind.common.manager.CssManager;
import myMind.common.manager.StyleWheelManager;
import myMind.common.manager.shortcut.ShortcutManager;
import myMind.common.util.IOUtil;
import myMind.common.util.MessageUtil;
import myMind.common.util.SaveUtil;
import myMind.componet.MapNode;
import myMind.componet.MindMap;
import myMind.componet.PrettyDialog;
import myMind.componet.Subject;

import java.io.File;
import java.io.IOException;

@Data
public class MindMapController {

    private Stage stage;
    private Scene scene;

    private AnchorPane root;
    private HBox titleBar;
    private TitleBarController titleBarController;

    private MindMap mindMap;
    private VBox searchPanel;
    private SearchController searchController;

    private ContextMenuController contextMenuController;
    private ContextMenu contextMenu;
    private static final Popup styleWheel = StyleWheelManager.getStyleWheel();

    private ShortcutManager shortcutManager;
    private EventHandler<ContextMenuEvent> contextMenuEventEventHandler;
    private ChangeListener<Tab> tabChangeListener;
    private ChangeListener<Boolean> stageChangeListener;

    public void createMindMap(Stage stage, Boolean addSubject) {
        this.stage = stage;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/title-bar.fxml"));
            titleBar = loader.load();
            titleBarController = loader.getController();
            titleBarController.setStage(stage);

            loader = new FXMLLoader(getClass().getResource("/fxml/context-menu.fxml"));
            contextMenu = loader.load();
            contextMenuController = loader.getController();

            loader = new FXMLLoader(getClass().getResource("/fxml/search-panel.fxml"));
            searchPanel = loader.load();
            searchController = loader.getController();
        } catch (IOException e) {
            e.printStackTrace();
        }

        root = new AnchorPane(titleBar);
        scene = new Scene(root);
        scene.getStylesheets().addAll(StyleSheetConstants.MAIN);
        CssManager.init(root);

        stage.setScene(scene);
        stage.getIcons().add(IOConstants.ICON);
        titleBarController.getIcon().setImage(IOConstants.ICON);
        titleBarController.titleProperty().bind(stage.titleProperty());
        stage.setTitle("MyMind");
        stage.initStyle(StageStyle.UNDECORATED);
        // 取消最大化时的位置
        stage.setX(6);
        stage.setY(12);
        stage.setHeight(740);
        stage.setWidth(1450);
        titleBarController.maximize();
        stage.show();

        AnchorPane.setLeftAnchor(titleBar, 0.0);
        AnchorPane.setRightAnchor(titleBar, 0.0);
        mindMap = new MindMap(addSubject);
        root.getChildren().addAll(mindMap, searchPanel);
        AnchorPane.setTopAnchor(mindMap, titleBar.getHeight());
        AnchorPane.setBottomAnchor(mindMap, 0.0);
        AnchorPane.setLeftAnchor(mindMap, 0.0);
        AnchorPane.setRightAnchor(mindMap, 0.0);
        searchController.setMindMap(mindMap);
        searchPanel.setVisible(false);
        AnchorPane.setTopAnchor(searchPanel, titleBar.getHeight());
        AnchorPane.setBottomAnchor(searchPanel, 0.0);
        AnchorPane.setLeftAnchor(searchPanel, 0.0);
        MessageUtil.init(root, stage);

        titleBarController.setMindMap(mindMap);
        titleBarController.setRoot(root);
        titleBarController.setSubjectController(mindMap.getSubjectController());
        contextMenuController.setSubjectController(mindMap.getSubjectController());

        addListener();
    }

    private void addListener() {
        shortcutManager = new ShortcutManager(scene, mindMap, contextMenuController, titleBarController, searchController);
        stage.setUserData(shortcutManager);

        // 防止被 StyleClassedTextArea 阻止事件
        contextMenuEventEventHandler = event -> {
            event.consume();
            MapNode selectedNode = mindMap.getSubjectController().getSelectedNode();
            if (selectedNode == null) {
                return;
            }

            if (selectedNode.getTextArea().getSelection().getLength() != 0) {
                // 默认将 content 的左上角放到 (x, y) 处，所以加上偏移，使轮盘居中
                styleWheel.show(stage, event.getScreenX() - 125, event.getScreenY() - 125);
            }
            // 得到当前被点中的实际节点，再向上查找是否属于 MapNode
            else if (isMapNode(event.getPickResult().getIntersectedNode())) {
                contextMenu.show(stage, event.getScreenX(), event.getScreenY());
            }
        };
        root.addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, contextMenuEventEventHandler);

        // 切换主题
        tabChangeListener = (observable, oldtab, newTab) -> {
            if (newTab == null) {
                return;
            }
            SubjectController subjectController = (SubjectController) newTab.getUserData();
            mindMap.setSubject(((Subject) newTab.getContent()));

            mindMap.setSubjectController(subjectController);
            titleBarController.setSubjectController(subjectController);
            contextMenuController.setSubjectController(subjectController);
            StyleWheelArcController.setSubjectController(subjectController);
            mindMap.recordLocation();
        };
        mindMap.getSelectionModel().selectedItemProperty().addListener(tabChangeListener);

        // 切换导图
        stageChangeListener = (obs, oldVal, newVal) -> {
            if (newVal) {
                StyleWheelArcController.setSubjectController(mindMap.getSubjectController());
            }
        };
        stage.focusedProperty().addListener(stageChangeListener);

        stage.setOnCloseRequest(event -> {
            if (mindMap.getFilePath() != null) {
                SaveUtil.saveNow(mindMap);
            }
            // 未保存且不为空
            else if (!mindMap.isEmpty()) {
                PrettyDialog dialog = new PrettyDialog("保存导图");
                dialog.setContentText("是否保存当前导图？");
                dialog.setButtonTypes(new ButtonType("保存", ButtonBar.ButtonData.YES),
                        new ButtonType("不保存", ButtonBar.ButtonData.NO),
                        new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE));
                ButtonBar.ButtonData buttonData = dialog.showAndWait().orElse(ButtonType.CANCEL).getButtonData();

                switch (buttonData) {
                    case YES -> titleBarController.saveAs();
                    case NO -> {
                        for (Tab tab : mindMap.getTabs()) {
                            ObservableList<Node> children = ((Subject) tab.getContent()).getNodesLayer().getChildren();
                            for (Node child : children) {
                                MapNode node = (MapNode) child;
                                IOUtil.deleteImage(node.getImageName());
                            }
                        }
                    }
                    default -> {
                        event.consume();
                        return;
                    }
                }
            }

            if (Stage.getWindows().size() <= 1) {
                SaveUtil.shutdown();
                // 关闭导图且不保存时，filePath 为 null
                if (mindMap.getFilePath() != null) {
                    IOUtil.addRecentFile(new File(mindMap.getFilePath()));
                }
            }

            styleWheel.hide();
            contextMenu.hide();

            root.removeEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, contextMenuEventEventHandler);
            mindMap.getSelectionModel().selectedItemProperty().removeListener(tabChangeListener);
            stage.focusedProperty().removeListener(stageChangeListener);
            stage.setOnCloseRequest(null);
            contextMenuEventEventHandler = null;
            tabChangeListener = null;
            stageChangeListener = null;
            MessageUtil.dispose(stage);

            stage.setUserData(null);
            shortcutManager.dispose();
            shortcutManager = null;

            titleBarController.dispose();
            titleBarController = null;
            contextMenu = null;
            contextMenuController.dispose();
            contextMenuController = null;
            titleBar = null;

            mindMap.setSubject(null);
            mindMap.setSubjectController(null);
            ObservableList<Tab> tabs = mindMap.getTabs();
            for (Tab tab : tabs) {
                SubjectController subjectController = (SubjectController) tab.getUserData();
                ObservableList<Node> children = subjectController.getSubject().getNodesLayer().getChildren();
                for (Node child : children) {
                    ((MapNode) child).getTextArea().dispose();
                }
                subjectController.getSubject().getNodesLayer().getChildren().removeAll();
                subjectController.getSubject().getLinesLayerR().getChildren().removeAll();
                subjectController.getSubject().getLinesLayerL().getChildren().removeAll();
            }
            tabs.clear();

            mindMap = null;
            root = null;
            scene = null;
            stage.setScene(null);
            stage = null;
            System.gc();
            System.runFinalization();
        });
    }

    private boolean isMapNode(Node node) {
        while (node != null) {
            if (node instanceof MapNode) {
                return true;
            }
            node = node.getParent();
        }
        return false;
    }

}