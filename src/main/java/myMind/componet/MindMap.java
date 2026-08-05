package myMind.componet;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import lombok.Data;
import myMind.common.constants.SizeConstants;
import myMind.common.util.FormatUtil;
import myMind.controller.StyleWheelArcController;
import myMind.controller.SubjectController;
import org.fxmisc.richtext.StyleClassedTextArea;

@Data
public class MindMap extends TabPane {
    private SubjectController subjectController;
    private Subject subject;
    private String filePath;

    public MindMap(Boolean addSubject) {
        //关闭按钮的显示策略
        //SELECTED_TAB：只在当前被选中的标签页显示
        //ALL_TABS：在所有标签页上都显示
        //UNAVAILABLE：完全不显示
        setTabClosingPolicy(TabClosingPolicy.SELECTED_TAB);
        setTabDragPolicy(TabDragPolicy.REORDER);
        getStyleClass().addAll("workspace");
        if (addSubject) {
            // 等 getWidth 有值
            Platform.runLater(this::addSubject);
        }
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
            boolean shortcutDown = event.isShortcutDown();

            // todo
            if (shortcutDown && event.isShiftDown() && code == KeyCode.Z) {
                // 先让 StyleClassedTextArea 撤消，如果撤消之后没有变化，则执行节点的撤消
                StyleClassedTextArea textArea = subjectController.getSelectedNode().getTextArea();
                if (textArea.isFocused()) {
                    if (!textArea.getUndoManager().redo()) {
                        subjectController.redo();
                    }
                } else {
                    subjectController.redo();
                }
                return;
            }

            if (shortcutDown && code == KeyCode.Z) {
                StyleClassedTextArea textArea = subjectController.getSelectedNode().getTextArea();
                if (textArea.isFocused()) {
                    if (!textArea.getUndoManager().undo()) {
                        subjectController.undo();
                    }
                } else {
                    subjectController.undo();
                }
                return;
            }

            // textArea 触发光标移动事件和 MindMap 触发 subject 移动事件之外的事件时，放行
            if (event.getTarget() != this || (code != KeyCode.HOME && code != KeyCode.END &&
                    code != KeyCode.PAGE_UP && code != KeyCode.PAGE_DOWN && code != KeyCode.SPACE &&
                    code != KeyCode.UP && code != KeyCode.DOWN &&
                    code != KeyCode.LEFT && code != KeyCode.RIGHT)) {
                return;
            }
            event.consume();

            switch (code) {
                case HOME -> {
                    double translateConstrain = subject.getScaleX() < 1 ? SizeConstants.SUBJECT_MARGIN / subject.getScaleX() : SizeConstants.SUBJECT_MARGIN;
                    subject.setTranslateY(subject.getTranslateY() + translateConstrain - subject.getBoundsInParent().getMinY());
                }
                case END -> {
                    double translateConstrain = subject.getScaleX() < 1 ? SizeConstants.SUBJECT_MARGIN / subject.getScaleX() : SizeConstants.SUBJECT_MARGIN;
                    // subject.getParent() 是 TabPaneSkin$TabContentRegion 所以不能直接用 getLayoutBounds().getHeight()
                    double deltaY = subject.getParent().getLayoutBounds().getHeight() - translateConstrain - subject.getBoundsInParent().getMaxY();
                    subject.setTranslateY(subject.getTranslateY() + deltaY);
                }
                case PAGE_UP -> subject.setTranslateY(subject.getTranslateY() + SizeConstants.TRANSLATE_MOVE_TEN);
                case PAGE_DOWN, SPACE ->
                        subject.setTranslateY(subject.getTranslateY() - SizeConstants.TRANSLATE_MOVE_TEN);
                case UP -> subject.setTranslateY(subject.getTranslateY() + SizeConstants.TRANSLATE_MOVE);
                case DOWN -> subject.setTranslateY(subject.getTranslateY() - SizeConstants.TRANSLATE_MOVE);
                case LEFT -> subject.setTranslateX(subject.getTranslateX() + SizeConstants.TRANSLATE_MOVE);
                case RIGHT -> subject.setTranslateX(subject.getTranslateX() - SizeConstants.TRANSLATE_MOVE);
            }
        });

        setOnKeyPressed(e -> {
            KeyCode code = e.getCode();
            boolean shortcutDown = e.isShortcutDown();
            boolean shiftDown = e.isShiftDown();

            // 回到中心
            if (shortcutDown && code == KeyCode.G) {
                subject.setTranslateX(0);
                subject.setTranslateY(0);
                return;
            }

            // 检查 Shift，防止干扰收起和展开
            if (shortcutDown && !shiftDown) {
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
     * 添加主题
     */
    public void addSubject() {
        subjectController = new SubjectController();
        String subjectName = "主题-" + (getTabs().size() + 1);
        Tab tab = addTab(subjectName);
        tab.setText(subjectName);
        getSelectionModel().select(tab);
    }

    /**
     * 打开导图时用
     */
    public void addSubject(MapNode node, long id) {
        subjectController = new SubjectController(node, id);
        String subjectName = "主题-" + (getTabs().size() + 1);
        Tab tab = addTab(subjectName);

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

        MapNode rootNode = subjectController.getRootNode();
//        StackPane tabHeaderArea = (StackPane) lookup(".tab-header-area");
        rootNode.setLayoutX((getWidth() - rootNode.getPrefWidth()) / 2);
        rootNode.setLayoutY((getHeight() - rootNode.getPrefHeight()) / 2);
        rootNode.getTextArea().textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.isEmpty()) {
                tab.setText(newValue);
            } else {
                tab.setText(subjectName);
            }
        });

        return tab;
    }

    public Tab jumpToSubject(long subjectId) {
        for (Tab tab : getTabs()) {
            Subject subject = (Subject) tab.getContent();
            if (subject.getSubjectId() == subjectId) {
                getSelectionModel().select(tab);
                return tab;
            }
        }
        return null;
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

    /**
     * 格式化
     */
    public void format() {
        StyleClassedTextArea textArea = subjectController.getSelectedNode().getTextArea();
        IndexRange selection = textArea.getSelection();
        // 没有选中文本则格式化全部，否则只格式化选中的文本
        if (selection.getLength() == 0) {
            textArea.selectAll();
            String selectedText = textArea.getSelectedText();
            textArea.replaceText(FormatUtil.format(selectedText));
        } else {
            String selectedText = textArea.getSelectedText();
            String formatedText = FormatUtil.format(selectedText);
            textArea.replaceText(selection.getStart(), selection.getEnd(), formatedText);
        }
    }

    /**
     * 分割节点
     * 冒号左边的保留在原节点，冒号右边的移到子节点
     */
    public void split() {
        MapNode selectedNode = subjectController.getSelectedNode();
        StyleClassedTextArea textArea = selectedNode.getTextArea();
        String[] split = FormatUtil.split(textArea.getText());
        if (split == null) {
            return;
        }
        textArea.replaceText(split[0]);

        subjectController.addChild(selectedNode.getPos());
        subjectController.getSelectedNode().getTextArea().replaceText(split[1]);
    }

    /**
     * 向下复制一行
     */
    public void copyLine() {
        MapNode selectedNode = subjectController.getSelectedNode();
        StyleClassedTextArea textArea = selectedNode.getTextArea();

        textArea.selectLine();
        String selectedText = textArea.getSelectedText();
        IndexRange selection = textArea.getSelection();
        textArea.replaceText(selection.getStart(), selection.getEnd(), selectedText + "\n"+ selectedText);
    }
}
