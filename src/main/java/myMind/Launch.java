package myMind;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.image.Image;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.Getter;
import myMind.common.manager.CssManager;
import myMind.common.manager.ShortcutManager;
import myMind.common.util.MessageUtil;
import myMind.componet.MindMap;
import myMind.componet.MindNode;
import myMind.componet.StyleWheel;
import myMind.componet.Subject;
import myMind.controller.ContextMenuController;
import myMind.controller.StyleWheelArcController;
import myMind.controller.SubjectController;
import myMind.controller.TitleBarController;

import java.io.IOException;
import java.util.List;

public class Launch extends Application {
    @Getter
    private static Stage mapStage;
    @Getter
    private static Stage noteStage;
    private static Scene scene;
    private static AnchorPane root;
    private static HBox titleBar;

    @Getter
    private static TitleBarController titleBarController;
    @Getter
    private static MindMap mindMap;

    private static ContextMenuController contextMenuController;
    private static ContextMenu contextMenu;
    private static final StyleWheel styleWheel = StyleWheel.getInstance();

    private static final List<String> STYLE_SHEETS = List.of(
            Launch.class.getResource("/css/base.css").toExternalForm(),
            Launch.class.getResource("/css/menu.css").toExternalForm(),
            Launch.class.getResource("/css/node.css").toExternalForm(),
            Launch.class.getResource("/css/note.css").toExternalForm(),
            Launch.class.getResource("/css/style-wheel.css").toExternalForm(),
            Launch.class.getResource("/css/title-bar.css").toExternalForm()
    );

    public static void createMindMap(Stage stage) {
        Launch.mapStage = stage;
        try {
            FXMLLoader loader = new FXMLLoader(Launch.class.getResource("/fxml/title-bar.fxml"));
            titleBar = loader.load();
            titleBarController = loader.getController();
            titleBarController.setMapStage(stage);

            loader = new FXMLLoader(Launch.class.getResource("/fxml/context-menu.fxml"));
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
        Image icon = new Image(Launch.class.getResourceAsStream("/icon.png"));
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

    private static void addListener() {
        mapStage.setUserData(new ShortcutManager(scene, contextMenuController, titleBarController));

        // 防止被 StyleClassedTextArea 阻止事件
        root.addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, event -> {
            event.consume();
            MindNode selectedNode = mindMap.getSubjectController().getSelectedNode();
            if (selectedNode == null) {
                return;
            }

            if (selectedNode.getTextArea().getSelection().getLength() != 0) {
                // 默认将 content 的左上角放到 (x, y) 处，所以加上偏移，使轮盘居中
                styleWheel.show(mapStage, event.getScreenX() - 125, event.getScreenY() - 125);
            }
            // 得到当前被点中的实际节点，再向上查找是否属于 MindNode
            else if (isMindNode(event.getPickResult().getIntersectedNode())) {
                contextMenu.show(mapStage, event.getScreenX(), event.getScreenY());
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
        mapStage.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                StyleWheelArcController.setSubjectController(mindMap.getSubjectController());
            }
        });

        // 通过任务栏关闭时，也执行关闭逻辑
        mapStage.setOnCloseRequest(event -> titleBarController.close());
    }

    private static boolean isMindNode(Node node) {
        while (node != null) {
            if (node instanceof MindNode) {
                return true;
            }
            node = node.getParent();
        }
        return false;
    }

    @Override
    public void start(Stage primaryStage) {
        createMindMap(primaryStage);

        BorderPane borderPane = null;
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Launch.class.getResource("/fxml/note.fxml"));
            borderPane = fxmlLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Scene scene = new Scene(borderPane);
        scene.getStylesheets().addAll(STYLE_SHEETS);

        noteStage = new Stage();
        noteStage.setScene(scene);
        Image icon = new Image(Launch.class.getResourceAsStream("/icon.png"));
        noteStage.getIcons().add(icon);
        noteStage.setTitle("MyNote");
        noteStage.setWidth(1200);
        noteStage.setHeight(720);
        noteStage.setMaximized(true);
    }

    public static void toMap() {
//        if (mindMap.isEmpty()) {
//            noteStage.hide();
//        }
        mapStage.show();
        mapStage.toFront();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
