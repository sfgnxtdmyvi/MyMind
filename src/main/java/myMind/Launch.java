package myMind;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.IndexRange;
import javafx.scene.control.MenuBar;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import myMind.componet.MindNode;
import myMind.componet.StyleWheel;
import myMind.componet.Workspace;
import myMind.controller.FileHandler;
import myMind.controller.MenuController;
import myMind.controller.StyleWheelArcController;
import myMind.util.MessageUtil;
import org.fxmisc.richtext.StyleClassedTextArea;

import java.io.IOException;
import java.util.List;

public class Launch extends Application {

    private static final List<String> STYLE_SHEETS = List.of(
            Launch.class.getResource("/css/style.css").toExternalForm(),
            Launch.class.getResource("/css/style-wheel.css").toExternalForm()
    );

    @Override
    public void start(Stage primaryStage) {
        newMindMap(primaryStage);
    }

    public static void newMindMap(Stage stage) {
        Workspace workspace = new Workspace();
        workspace.getStyleClass().add("workspace");
        BorderPane borderPane = new BorderPane();

        try {
            FXMLLoader loader = new FXMLLoader(Launch.class.getResource("/fxml/menu.fxml"));
            MenuBar menuBar = loader.load();
            MenuController menuController = loader.getController();
            menuController.setWorkspace(workspace);
            menuController.setFileHandler(new FileHandler(workspace));
            borderPane.setTop(menuBar);
            borderPane.setCenter(workspace);
        } catch (IOException e) {
            e.printStackTrace();
        }

        stage.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                MenuController.setSubjectController(workspace.getSubjectController());
                StyleWheelArcController.setSubjectController(workspace.getSubjectController());
            }
        });

        // Pane 用来添加消息提示标签，BorderPane 无法手动指定位置
        Pane root = new Pane(borderPane);
        // Pane 不会自动调整子节点大小，需要手动绑定
        borderPane.prefWidthProperty().bind(root.widthProperty());
        borderPane.prefHeightProperty().bind(root.heightProperty());
        MessageUtil.init(root, stage);

        StyleWheel styleWheel = StyleWheel.getInstance();
        // 防止被 StyleClassedTextArea 阻止事件
        root.addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, event -> {
            MindNode selectedNode = workspace.getSubjectController().getSelectedNode();
            if (selectedNode == null) {
                return;
            }
            StyleClassedTextArea textArea = selectedNode.getTextArea();
            IndexRange selection = textArea.getSelection();
            if (selection.getLength() != 0) {
                // 默认将 content 的左上角放到 (x, y) 处，所以加上偏移，使轮盘居中
                styleWheel.show(stage, event.getScreenX() - 125, event.getScreenY() - 125);
            }
            event.consume();
        });

        Scene scene = new Scene(root, 1450, 740);
        scene.getStylesheets().addAll(STYLE_SHEETS);

        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}