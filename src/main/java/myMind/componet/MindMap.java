package myMind.componet;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.geometry.Bounds;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import lombok.Data;
import myMind.common.constants.NodeConstants;
import myMind.common.constants.SizeConstants;
import myMind.controller.StyleWheelArcController;
import myMind.controller.SubjectController;
import org.fxmisc.richtext.StyleClassedTextArea;

@Data
public class MindMap extends TabPane {
    private SubjectController subjectController;
    private Subject subject;
    private String filePath;

    public MindMap() {
        //关闭按钮的显示策略
        //SELECTED_TAB：只在当前被选中的标签页显示
        //ALL_TABS：在所有标签页上都显示
        //UNAVAILABLE：完全不显示
        setTabClosingPolicy(TabClosingPolicy.SELECTED_TAB);
        setTabDragPolicy(TabDragPolicy.REORDER);
        getStyleClass().addAll("hide-tabs", "workspace");
        addSubject();
        Platform.runLater(() -> subjectController.getSelectedNode().getTextArea().requestFocus());
        addListener();
    }

    private void addListener() {
        // 只有一个主题时，隐藏标签栏
        getTabs().addListener((ListChangeListener.Change<? extends Tab> c) -> {
            if (getTabs().size() == 1) {
                getStyleClass().add("hide-tabs");
            } else {
                getStyleClass().remove("hide-tabs");
            }
        });

        // 画布移动和缩放
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
//                subject.constrainTranslationY();
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
            }
            e.consume();
        });

        addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            KeyCode code = event.getCode();
            // textArea 触发事件时忽略
            if (event.getTarget() != this || (code != KeyCode.HOME &&
                    code != KeyCode.END &&
                    code != KeyCode.PAGE_UP &&
                    code != KeyCode.PAGE_DOWN &&
                    code != KeyCode.SPACE &&
                    code != KeyCode.UP &&
                    code != KeyCode.DOWN &&
                    code != KeyCode.LEFT &&
                    code != KeyCode.RIGHT)) {
                return;
            }

            event.consume();
            switch (code) {
                case HOME -> {
                    double translateConstrain = subject.getScaleX() < 1 ? SizeConstants.TRANSLATE_CONSTRAIN / subject.getScaleX() : SizeConstants.TRANSLATE_CONSTRAIN;
                    subject.setTranslateY(subject.getTranslateY() + translateConstrain - subject.getBoundsInParent().getMinY());
                }
                case END -> {
                    double translateConstrain = subject.getScaleX() < 1 ? SizeConstants.TRANSLATE_CONSTRAIN / subject.getScaleX() : SizeConstants.TRANSLATE_CONSTRAIN;
                    // subject.getParent() 是 TabPaneSkin$TabContentRegion 所以不能直接用 getLayoutBounds().getHeight()
                    double deltaY = subject.getParent().getLayoutBounds().getHeight() - translateConstrain - subject.getBoundsInParent().getMaxY();
                    subject.setTranslateY(subject.getTranslateY() + deltaY);
                }
                case PAGE_UP -> subject.setTranslateY(subject.getTranslateY() + SizeConstants.TRANSLATE_OFFSET_PLUS);
                case PAGE_DOWN, SPACE ->
                        subject.setTranslateY(subject.getTranslateY() - SizeConstants.TRANSLATE_OFFSET_PLUS);
                case UP -> subject.setTranslateY(subject.getTranslateY() + SizeConstants.TRANSLATE_OFFSET);
                case DOWN -> subject.setTranslateY(subject.getTranslateY() - SizeConstants.TRANSLATE_OFFSET);
                case LEFT -> subject.setTranslateX(subject.getTranslateX() + SizeConstants.TRANSLATE_OFFSET);
                case RIGHT -> subject.setTranslateX(subject.getTranslateX() - SizeConstants.TRANSLATE_OFFSET);
            }
        });

        setOnKeyPressed(e -> {
            KeyCode code = e.getCode();
            boolean shortcutDown = e.isShortcutDown();

            // 回到中心
            if (shortcutDown && code == KeyCode.G) {
                subject.setTranslateX(0);
                subject.setTranslateY(0);
                return;
            }

            // 检查 Shift，防止干扰收起和展开
            if (shortcutDown && !e.isShiftDown()) {
                if (code == KeyCode.DIGIT0) {
                    subject.changeScale(1.0);
                } else if (code == KeyCode.MINUS) {
                    subject.changeScale(subject.getScaleX() - 0.1);
                } else if (code == KeyCode.EQUALS) {
                    subject.changeScale(subject.getScaleX() + 0.1);
                }
            }
        });
    }

    /**
     * 添加导图
     */
    public void addSubject() {
        subjectController = new SubjectController();
        String subjectName = "主题-" + (getTabs().size() + 1);
        Tab tab = addTab(subjectName);
        tab.setText(subjectName);
        getSelectionModel().select(tab);

        subject.layoutBoundsProperty().addListener(new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Bounds> observable, Bounds oldBounds, Bounds newBounds) {
                if (newBounds.getHeight() > 0) {
                    MindNode rootNode = subjectController.getRootNode();
                    rootNode.setLayoutX((newBounds.getWidth() - NodeConstants.MIN_NODE_WIDTH) / 2);
                    rootNode.setLayoutY((newBounds.getHeight() - NodeConstants.MIN_NODE_HEIGHT) / 2);
                    subject.layoutBoundsProperty().removeListener(this);
                    NodeConstants.CENTER_X = newBounds.getWidth() / 2;
                    NodeConstants.CENTER_Y = newBounds.getHeight() / 2;
                }
            }
        });
    }

    /**
     * 打开导图
     */
    public void addSubject(MindNode node) {
        subjectController = new SubjectController(node);
        String subjectName = "主题-" + (getTabs().size() + 1);
        Tab tab = addTab(subjectName);

        node.setLayoutX(NodeConstants.CENTER_X - node.getPrefWidth() / 2);
        node.setLayoutY(NodeConstants.CENTER_Y - node.getPrefHeight() / 2);

        StyleClassedTextArea textArea = node.getTextArea();
        if (!textArea.getText().isEmpty()) {
            tab.setText(textArea.getText());
        } else {
            tab.setText(subjectName);
        }
    }

    private Tab addTab(String subjectName) {
        subject = subjectController.getSubject();
        StyleWheelArcController.setSubjectController(subjectController);

        Tab tab = new Tab();
        tab.setContent(subject);
        tab.setUserData(subjectController);
        getTabs().add(tab);

        subjectController.getRootNode().getTextArea().textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.isEmpty()) {
                tab.setText(newValue);
            } else {
                tab.setText(subjectName);
            }
        });
        return tab;
    }

    public boolean isEmpty() {
        boolean empty = true;
        for (Tab tab : getTabs()) {
            SubjectController subjectController = (SubjectController) tab.getUserData();
            // 只要一个不为空就为 false
            if (!subjectController.getRootNode().isEmpty()) {
                empty = false;
                break;
            }
        }
        return empty;
    }
}
