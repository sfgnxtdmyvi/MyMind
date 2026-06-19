package myMind.controller;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.image.Image;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.Data;
import myMind.common.manager.CssManager;
import myMind.common.manager.ShortcutManager;
import myMind.common.util.MessageUtil;
import myMind.componet.MindMap;
import myMind.componet.MindNode;
import myMind.componet.StyleWheel;
import myMind.componet.Subject;

import java.io.IOException;
import java.util.List;

@Data
public class MindMapController {
    private Stage stage;
    private Scene scene;
    private AnchorPane root;
    private HBox titleBar;

    private TitleBarController titleBarController;
    private MindMap mindMap;

    private ContextMenuController contextMenuController;
    private ContextMenu contextMenu;
    private static final StyleWheel styleWheel = StyleWheel.getInstance();

    private static final List<String> STYLE_SHEETS = List.of(
            MindMapController.class.getResource("/css/base.css").toExternalForm(),
            MindMapController.class.getResource("/css/menu.css").toExternalForm(),
            MindMapController.class.getResource("/css/node.css").toExternalForm(),
            MindMapController.class.getResource("/css/style-wheel.css").toExternalForm(),
            MindMapController.class.getResource("/css/title-bar.css").toExternalForm()
    );

    public void createMindMap(Stage stage) {
        this.stage = stage;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/title-bar.fxml"));
            titleBar = loader.load();
            titleBarController = loader.getController();
            titleBarController.setMapStage(stage);

            loader = new FXMLLoader(getClass().getResource("/fxml/context-menu.fxml"));
            contextMenu = loader.load();
            contextMenuController = loader.getController();
        } catch (IOException e) {
            e.printStackTrace();
        }

        root = new AnchorPane(titleBar);
        scene = new Scene(root);
        scene.getStylesheets().addAll(STYLE_SHEETS);
        CssManager.init(root);

        stage.setScene(scene);
        Image icon = new Image(getClass().getResourceAsStream("/icon.png"));
        stage.getIcons().add(icon);
        titleBarController.getIcon().setImage(icon);
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
        mindMap = new MindMap();
        root.getChildren().add(mindMap);
        AnchorPane.setTopAnchor(mindMap, titleBar.getHeight());
        AnchorPane.setBottomAnchor(mindMap, 0.0);
        AnchorPane.setLeftAnchor(mindMap, 0.0);
        AnchorPane.setRightAnchor(mindMap, 0.0);
        MessageUtil.init(root, stage);

        titleBarController.setMindMap(mindMap);
        titleBarController.setRoot(root);
        titleBarController.setSubjectController(mindMap.getSubjectController());
        contextMenuController.setSubjectController(mindMap.getSubjectController());

        addListener();
    }

    private void addListener() {
        stage.setUserData(new ShortcutManager(scene, contextMenuController, titleBarController));

        // 防止被 StyleClassedTextArea 阻止事件
        root.addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, event -> {
            event.consume();
            MindNode selectedNode = mindMap.getSubjectController().getSelectedNode();
            if (selectedNode == null) {
                return;
            }

            if (selectedNode.getTextArea().getSelection().getLength() != 0) {
                // 默认将 content 的左上角放到 (x, y) 处，所以加上偏移，使轮盘居中
                styleWheel.show(stage, event.getScreenX() - 125, event.getScreenY() - 125);
            }
            // 得到当前被点中的实际节点，再向上查找是否属于 MindNode
            else if (isMindNode(event.getPickResult().getIntersectedNode())) {
                contextMenu.show(stage, event.getScreenX(), event.getScreenY());
            }
        });

        // 切换主题
        mindMap.getSelectionModel().selectedItemProperty().addListener((observable, oldtab, newTab) -> {
            if (newTab == null) {
                return;
            }
            SubjectController subjectController = (SubjectController) newTab.getUserData();
            mindMap.setSubject(((Subject) newTab.getContent()));

            mindMap.setSubjectController(subjectController);
            titleBarController.setSubjectController(subjectController);
            contextMenuController.setSubjectController(subjectController);
            StyleWheelArcController.setSubjectController(subjectController);
        });

        // 切换导图
        stage.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                StyleWheelArcController.setSubjectController(mindMap.getSubjectController());
            }
        });

        // 通过任务栏关闭时，也执行关闭逻辑
        stage.setOnCloseRequest(event -> titleBarController.close());
    }

    private boolean isMindNode(Node node) {
        while (node != null) {
            if (node instanceof MindNode) {
                return true;
            }
            node = node.getParent();
        }
        return false;
    }
}