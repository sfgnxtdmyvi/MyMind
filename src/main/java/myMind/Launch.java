package myMind;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.IndexRange;
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
import myMind.controller.StyleWheelArcController;
import myMind.controller.SubjectController;
import myMind.controller.TitleBarController;
import myMind.util.MessageUtil;

import java.io.IOException;
import java.util.List;

public class Launch extends Application {
    private static TitleBarController titleBarController;
    private static final StyleWheel styleWheel = StyleWheel.getInstance();

    private static final List<String> STYLE_SHEETS = List.of(
            Launch.class.getResource("/css/base.css").toExternalForm(),
            Launch.class.getResource("/css/node.css").toExternalForm(),
            Launch.class.getResource("/css/style-wheel.css").toExternalForm(),
            Launch.class.getResource("/css/title-bar.css").toExternalForm()
    );

    public static void createMindMap(Stage stage) {
        MindMap mindMap = new MindMap();
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
        } catch (IOException e) {
            e.printStackTrace();
        }

        // todo 合并成一个Pane，换成其他布局
        // Pane 用来添加消息提示标签，BorderPane 无法手动指定位置
        Pane root = new Pane(borderPane);
        // Pane 不会自动调整子节点大小，需要手动绑定
        borderPane.prefWidthProperty().bind(root.widthProperty());
        borderPane.prefHeightProperty().bind(root.heightProperty());
        MessageUtil.init(root, stage);

        Scene scene = new Scene(root);
        scene.getStylesheets().addAll(STYLE_SHEETS);

        // todo 窗口圆角
        stage.setScene(scene);
        stage.initStyle(StageStyle.UNDECORATED);
        // 取消最大化时的位置
        stage.setX(6);
        stage.setY(12);
        stage.setHeight(740);
        stage.setWidth(1450);
        titleBarController.maximize();
        stage.show();

        // 防止被 StyleClassedTextArea 阻止事件
        root.addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, event -> {
            event.consume();
            MindNode selectedNode = mindMap.getSubjectController().getSelectedNode();
            if (selectedNode == null) {
                return;
            }
            IndexRange selection = selectedNode.getTextArea().getSelection();
            if (selection.getLength() != 0) {
                // 默认将 content 的左上角放到 (x, y) 处，所以加上偏移，使轮盘居中
                styleWheel.show(stage, event.getScreenX() - 125, event.getScreenY() - 125);
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
            StyleWheelArcController.setSubjectController(subjectController);
        });

        // 切换导图
        stage.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                StyleWheelArcController.setSubjectController(mindMap.getSubjectController());
            }
        });
    }

    @Override
    public void start(Stage primaryStage) {
        createMindMap(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
