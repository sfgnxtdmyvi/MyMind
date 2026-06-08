package myMind;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.image.Image;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import myMind.componet.MindMap;
import myMind.componet.MindNode;
import myMind.componet.StyleWheel;
import myMind.componet.Subject;
import myMind.controller.ContextMenuController;
import myMind.controller.StyleWheelArcController;
import myMind.controller.SubjectController;
import myMind.controller.TitleBarController;
import myMind.util.MessageUtil;

import java.io.IOException;
import java.util.List;

public class Launch extends Application {
    private static Stage stage;
    private static Pane root;
    private static Scene scene;

    private static MindMap mindMap;
    private static TitleBarController titleBarController;
    private static ContextMenuController contextMenuController;
    private static ContextMenu contextMenu;
    private static final StyleWheel styleWheel = StyleWheel.getInstance();

    private static final List<String> STYLE_SHEETS = List.of(
            Launch.class.getResource("/css/base.css").toExternalForm(),
            Launch.class.getResource("/css/node.css").toExternalForm(),
            Launch.class.getResource("/css/style-wheel.css").toExternalForm(),
            Launch.class.getResource("/css/title-bar.css").toExternalForm()
    );

    public static void createMindMap(Stage stage) {
        Launch.stage = stage;
        mindMap = new MindMap();
        BorderPane borderPane = new BorderPane();
        try {
            FXMLLoader loader = new FXMLLoader(Launch.class.getResource("/fxml/title-bar.fxml"));
            HBox titleBar = loader.load();
            titleBarController = loader.getController();
            titleBarController.setMindMap(mindMap);
            titleBarController.setSubjectController(mindMap.getSubjectController());
            titleBarController.setStage(stage);
            borderPane.setTop(titleBar);
            borderPane.setCenter(mindMap);

            loader = new FXMLLoader(Launch.class.getResource("/fxml/context-menu.fxml"));
            contextMenu = loader.load();
            contextMenuController = loader.getController();
            contextMenuController.setSubjectController(mindMap.getSubjectController());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // todo 合并成一个Pane，换成其他布局
        // Pane 用来添加消息提示标签，BorderPane 无法手动指定位置
        root = new Pane(borderPane);
        // Pane 不会自动调整子节点大小，需要手动绑定
        borderPane.prefWidthProperty().bind(root.widthProperty());
        borderPane.prefHeightProperty().bind(root.heightProperty());
        MessageUtil.init(root, stage);

        scene = new Scene(root);
        scene.getStylesheets().addAll(STYLE_SHEETS);

        stage.setScene(scene);
        Image icon = new Image("icon.png");
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

        addListener();
    }

    private static void addListener() {
        contextMenuController.registerGlobalAccelerators(scene);
        titleBarController.registerGlobalAccelerators(scene);

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
    }

    public static void main(String[] args) {
        launch(args);
    }
}
