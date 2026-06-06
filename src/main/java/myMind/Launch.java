package myMind;

import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.IndexRange;
import javafx.scene.control.MenuBar;
import javafx.scene.control.Tab;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import myMind.componet.MindMap;
import myMind.componet.MindNode;
import myMind.componet.StyleWheel;
import myMind.componet.Subject;
import myMind.controller.MenuController;
import myMind.controller.StyleWheelArcController;
import myMind.controller.SubjectController;
import myMind.util.FileUtil;
import myMind.util.MessageUtil;

import java.io.IOException;
import java.util.List;

public class Launch extends Application {
    private static MenuController menuController;
    private static final StyleWheel styleWheel = StyleWheel.getInstance();

    private static final List<String> STYLE_SHEETS = List.of(
            Launch.class.getResource("/css/style.css").toExternalForm(),
            Launch.class.getResource("/css/style-wheel.css").toExternalForm()
    );

    public static void createMindMap(Stage stage) {
        MindMap mindMap = new MindMap();
        BorderPane borderPane = new BorderPane();
        try {
            FXMLLoader loader = new FXMLLoader(Launch.class.getResource("/fxml/menu.fxml"));
            MenuBar menuBar = loader.load();
            menuController = loader.getController();
            menuController.setMindMap(mindMap);
            borderPane.setTop(menuBar);
            borderPane.setCenter(mindMap);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Pane 用来添加消息提示标签，BorderPane 无法手动指定位置
        Pane root = new Pane(borderPane);
        // Pane 不会自动调整子节点大小，需要手动绑定
        borderPane.prefWidthProperty().bind(root.widthProperty());
        borderPane.prefHeightProperty().bind(root.heightProperty());
        MessageUtil.init(root, stage);

        Scene scene = new Scene(root, 1450, 740);
        scene.getStylesheets().addAll(STYLE_SHEETS);

        stage.setScene(scene);
        stage.setMaximized(true);
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
            menuController.setSubjectController(subjectController);
            StyleWheelArcController.setSubjectController(subjectController);
        });

        // 切换导图
        stage.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                StyleWheelArcController.setSubjectController(mindMap.getSubjectController());
            }
        });

        stage.setOnCloseRequest(event -> {
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
                    case YES -> {
                        menuController.saveAs();
                        stage.close();
                    }
                    case NO -> stage.close();
                    default -> event.consume();
                }
            }

            if (Stage.getWindows().size() <= 1) {
                FileUtil.cancelSchedule();
            } else {
                FileUtil.cancelSchedule(mindMap.getFilePath());
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
