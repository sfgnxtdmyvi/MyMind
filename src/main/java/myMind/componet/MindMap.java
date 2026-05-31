package myMind.componet;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.IndexRange;
import javafx.scene.control.MenuBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import lombok.Getter;
import myMind.Launch;
import myMind.controller.FileHandler;
import myMind.controller.MenuController;
import myMind.controller.StyleWheelArcController;
import myMind.controller.SubjectController;
import myMind.util.MessageUtil;
import org.fxmisc.richtext.StyleClassedTextArea;

import java.io.IOException;
import java.util.List;

public class MindMap extends TabPane {
    private final Pane root;
    private final Stage stage;
    private final StyleWheel styleWheel = StyleWheel.getInstance();
    @Getter
    private SubjectController subjectController;

    private static final List<String> STYLE_SHEETS = List.of(
            MindMap.class.getResource("/css/style.css").toExternalForm(),
            MindMap.class.getResource("/css/style-wheel.css").toExternalForm()
    );

    public MindMap(Stage stage) {
        //关闭按钮的显示策略
        //SELECTED_TAB：只在当前被选中的标签页显示
        //ALL_TABS：在所有标签页上都显示
        //UNAVAILABLE：完全不显示
        setTabClosingPolicy(TabClosingPolicy.ALL_TABS);
        getStyleClass().addAll("hide-tabs", "workspace");
        addSubject();
        Platform.runLater(() -> subjectController.getSelectedNode().getTextArea().requestFocus());

        BorderPane borderPane = new BorderPane();
        try {
            FXMLLoader loader = new FXMLLoader(Launch.class.getResource("/fxml/menu.fxml"));
            MenuBar menuBar = loader.load();
            MenuController menuController = loader.getController();
            menuController.setMindMap(this);
            menuController.setFileHandler(new FileHandler(this));
            borderPane.setTop(menuBar);
            borderPane.setCenter(this);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Pane 用来添加消息提示标签，BorderPane 无法手动指定位置
        root = new Pane(borderPane);
        // Pane 不会自动调整子节点大小，需要手动绑定
        borderPane.prefWidthProperty().bind(root.widthProperty());
        borderPane.prefHeightProperty().bind(root.heightProperty());
        MessageUtil.init(root, stage);

        Scene scene = new Scene(root, 1450, 740);
        scene.getStylesheets().addAll(STYLE_SHEETS);

        this.stage = stage;
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();

        addListener();
    }

    private void addListener() {
        // 切换主题
        getSelectionModel().selectedItemProperty().addListener((observable, oldtab, newTab) -> {
            if (newTab == null) {
                return;
            }
            subjectController = ((Subject) newTab.getContent()).getSubjectController();
            MenuController.setSubjectController(subjectController);
            StyleWheelArcController.setSubjectController(subjectController);
        });

        // 切换导图
        stage.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                MenuController.setSubjectController(subjectController);
                StyleWheelArcController.setSubjectController(subjectController);
            }
        });

        stage.setOnCloseRequest(event -> {
            if (stage.getTitle() == null) {
                for (Tab tab : getTabs()) {
                    for (MindNode node : ((Subject) tab.getContent()).getModelToView().values()) {
                        FileHandler.deleteImage(node.getImageName());
                    }
                }
            }
        });

        // 只有一个主题时，隐藏标签栏
        getTabs().addListener((ListChangeListener.Change<? extends Tab> c) -> {
            if (getTabs().size() == 1) {
                getStyleClass().add("hide-tabs");
            } else {
                getStyleClass().remove("hide-tabs");
            }
        });

        // 防止被 StyleClassedTextArea 阻止事件
        root.addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, event -> {
            MindNode selectedNode = subjectController.getSelectedNode();
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
    }

    public void addSubject() {
        subjectController = new SubjectController();
        Subject subject = subjectController.getSubject();
        MenuController.setSubjectController(subjectController);
        StyleWheelArcController.setSubjectController(subjectController);

        String subjectName = "主题" + (getTabs().size() + 1);
        Tab tab = new Tab(subjectName);
        tab.setContent(subject);

        getTabs().add(tab);
        getSelectionModel().select(tab);

        // todo 动态计算中心点
//        Platform.runLater(() -> {
//            double centerX = (getWidth() - SizeConstants.MIN_NODE_WIDTH) / 2.0;
//            double centerY = getHeight() / 2.0 - SizeConstants.MIN_NODE_HEIGHT;
//        });
        subjectController.initRootNode(670, 311);
        subjectController.getRootNode().getTextArea().textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.isEmpty()) {
                tab.setText(newValue);
            } else {
                tab.setText(subjectName);
            }
        });
    }
}
