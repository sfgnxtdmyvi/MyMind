package myMind.componet;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.IndexRange;
import javafx.scene.control.MenuBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import lombok.Data;
import myMind.Launch;
import myMind.constants.SizeConstants;
import myMind.controller.FileHandler;
import myMind.controller.MenuController;
import myMind.controller.StyleWheelArcController;
import myMind.controller.SubjectController;
import myMind.util.MessageUtil;
import org.fxmisc.richtext.StyleClassedTextArea;

import java.io.IOException;
import java.util.List;

@Data
public class MindMap extends TabPane {
    private final Pane root;
    private final Stage stage;
    private final StyleWheel styleWheel = StyleWheel.getInstance();
    private SubjectController subjectController;
    private Subject subject;
    private String filePath;

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
        FileHandler fileHandler = new FileHandler(this);
        try {
            FXMLLoader loader = new FXMLLoader(Launch.class.getResource("/fxml/menu.fxml"));
            MenuBar menuBar = loader.load();
            MenuController menuController = loader.getController();
            menuController.setMindMap(this);
            menuController.setFileHandler(fileHandler);
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

        addListener(fileHandler);
    }

    private void addListener(FileHandler fileHandler) {
        // 切换主题
        getSelectionModel().selectedItemProperty().addListener((observable, oldtab, newTab) -> {
            if (newTab == null) {
                return;
            }
            subject = ((Subject) newTab.getContent());
            subjectController = subject.getSubjectController();
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
            if (filePath == null) {
                for (Tab tab : getTabs()) {
                    for (MindNode node : ((Subject) tab.getContent()).getModelToView().values()) {
                        FileHandler.deleteImage(node.getImageName());
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
//                        menuController.
                        Platform.exit();
                    }
                    case NO -> Platform.exit();
                    default -> event.consume();
                }
            } else {
                if (Stage.getWindows().size() <= 1) {
                    fileHandler.CancelSchedule();
                } else {
                    fileHandler.CancelSchedule(filePath);
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
            event.consume();
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
        });

        addTranslateListener();
    }

    // 画布移动和缩放
    private void addTranslateListener() {
        // EventFilter 能让节点不干扰鼠标滚动
        addEventFilter(ScrollEvent.SCROLL, e -> {
            double deltaY = e.getDeltaY();
            if (e.isShortcutDown()) {
                double scale = subject.getScaleX() + (deltaY > 0 ? 0.1 : -0.1);
                subject.changeScale(scale);
            } else {
                for (int i = 0; i < 3; i++) {
                    subject.setTranslateY(subject.getTranslateY() + deltaY);
                }
                subject.constrainTranslationY();
            }
        });

        setOnMousePressed(e -> {
            subject.setDragStartX(e.getSceneX());
            subject.setDragStartY(e.getSceneY());
            e.consume();
        });

        setOnMouseDragged(e -> {
            // 调整图片大小时，不能移动画布
            if (e.getButton() == MouseButton.PRIMARY && !subjectController.getSelectedNode().isResizing()) {
                subject.setTranslateX(subject.getTranslateX() + e.getSceneX() - subject.getDragStartX());
                subject.setTranslateY(subject.getTranslateY() + e.getSceneY() - subject.getDragStartY());
                subject.setDragStartX(e.getSceneX());
                subject.setDragStartY(e.getSceneY());

                subject.constrainTranslation();
            }
            e.consume();
        });

        addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            KeyCode code = event.getCode();
            if (code == KeyCode.HOME) {
                subject.setTranslateY(subject.getTranslateY() - subject.getBoundsInParent().getMinY() + SizeConstants.TRANSLATE_OFFSET);
                event.consume();
                return;
            }
            if (code == KeyCode.END) {
                double deltaY = getLayoutBounds().getHeight() - subject.getBoundsInParent().getMaxY() - SizeConstants.TRANSLATE_OFFSET;
                subject.setTranslateY(subject.getTranslateY() + deltaY);
                event.consume();
            }
        });

        setOnKeyPressed(e -> {
            KeyCode code = e.getCode();
            boolean shortcutDown = e.isShortcutDown();

            if (code == KeyCode.HOME) {
                subject.setTranslateY(subject.getTranslateY() - subject.getBoundsInParent().getMinY() + SizeConstants.TRANSLATE_OFFSET);
                return;
            }
            if (code == KeyCode.END) {
                double deltaY = getLayoutBounds().getHeight() - subject.getBoundsInParent().getMaxY() - SizeConstants.TRANSLATE_OFFSET;
                subject.setTranslateY(subject.getTranslateY() + deltaY);
                return;
            }

            if (code == KeyCode.PAGE_UP) {
                subject.setTranslateY(subject.getTranslateY() + 500);
                subject.constrainTranslationY();
                return;
            }
            if (code == KeyCode.PAGE_DOWN || code == KeyCode.SPACE) {
                subject.setTranslateY(subject.getTranslateY() - 500);
                subject.constrainTranslationY();
                return;
            }

            // 回到中心
            if (shortcutDown && code == KeyCode.G) {
                subject.setTranslateX(0);
                subject.setTranslateY(0);
                return;
            }

            if (shortcutDown) {
                if (code == KeyCode.DIGIT0) {
                    subject.changeScale(1.0);
                } else if (code == KeyCode.MINUS) {
                    subject.changeScale(getScaleX() - 0.1);
                } else if (code == KeyCode.EQUALS) {
                    subject.changeScale(getScaleX() + 0.1);
                }
            }
        });
    }

    /**
     * 添加导图
     */
    public void addSubject() {
        String subjectName = "主题" + (getTabs().size() + 1);
        Tab tab = addTab();
        tab.setText(subjectName);
        getSelectionModel().select(tab);

        // todo 动态计算中心点
        subjectController.initRootNode(670, 311);
        subjectController.getRootNode().getTextArea().textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.isEmpty()) {
                tab.setText(newValue);
            } else {
                tab.setText(subjectName);
            }
        });
    }

    /**
     * 打开导图
     */
    public void addSubject(MindNode node) {
        String subjectName = "主题" + (getTabs().size() + 1);
        Tab tab = addTab();

        subjectController.initRootNode(node);
        StyleClassedTextArea textArea = node.getTextArea();
        if (!textArea.getText().isEmpty()) {
            tab.setText(textArea.getText());
        } else {
            tab.setText(subjectName);
        }
        textArea.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.isEmpty()) {
                tab.setText(newValue);
            } else {
                tab.setText(subjectName);
            }
        });
    }

    private Tab addTab() {
        subjectController = new SubjectController();
        subject = subjectController.getSubject();
        MenuController.setSubjectController(subjectController);
        StyleWheelArcController.setSubjectController(subjectController);

        Tab tab = new Tab();
        tab.setContent(subject);
        getTabs().add(tab);
        return tab;
    }
}
