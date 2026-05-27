package myMind.componet;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import lombok.Getter;
import myMind.controller.MenuController;
import myMind.controller.SubjectController;
import org.fxmisc.richtext.StyleClassedTextArea;

import java.util.ArrayList;
import java.util.List;

public class Workspace extends TabPane {
    @Getter
    private SubjectController subjectController;

    public Workspace() {
        //关闭按钮的显示策略
        //SELECTED_TAB：只在当前被选中的标签页显示
        //ALL_TABS：在所有标签页上都显示
        //UNAVAILABLE：完全不显示
        setTabClosingPolicy(TabClosingPolicy.ALL_TABS);
        getStyleClass().add("hide-tabs");
        addSubject();
        Platform.runLater(() -> subjectController.getSelectedNode().getTextArea().requestFocus());

        addListener();
    }

    private void addListener() {
        getSelectionModel().selectedItemProperty().addListener((observable, oldtab, newTab) -> {
            if (newTab == null) {
                return;
            }
            subjectController = ((Subject) newTab.getContent()).getSubjectController();
            MenuController.setSubjectController(subjectController);
        });

        getTabs().addListener((ListChangeListener.Change<? extends Tab> c) -> {
            //只有一个主题时，隐藏标签栏
            if (getTabs().size() == 1) {
                getStyleClass().add("hide-tabs");
            } else {
                getStyleClass().remove("hide-tabs");
            }
        });

        // 键盘快捷键
        setOnKeyPressed(e -> {
            //跨平台修饰键
            //在 Windows / Linux 上：它等同于 e.isControlDown() (即 Ctrl 键)
            //在 macOS 上：它等同于 e.isMetaDown() (即 Command ⌘ 键)
            boolean shortcutDown = e.isShortcutDown();
            KeyCode code = e.getCode();

            // 文本样式
            if (shortcutDown && (code == KeyCode.B || code == KeyCode.R)) {
                MindNode selectedNode = subjectController.getSelectedNode();
                StyleClassedTextArea textArea = selectedNode.getTextArea();
                IndexRange selection = textArea.getSelection();

                if (selection.getLength() > 0) {
                    int start = selection.getStart();
                    List<String> styles = new ArrayList<>(textArea.getStyleOfChar(start));
                    // getStyleAtPosition(p) is equivalent to getStyleOfChar(p-1)
                    // 用getStyleAtPosition获取的是指定位置的前一个位置的样式
                    // List<String> styles = new ArrayList<>(textArea.getStyleAtPosition(start + 1));
                    if (code == KeyCode.B) {
                        if (styles.contains("bold-text")) {
                            styles.remove("bold-text");
                        } else {
                            styles.add("bold-text");
                        }
                    } else if (code == KeyCode.R) {
                        if (styles.contains("red-text")) {
                            styles.remove("red-text");
                        } else {
                            styles.add("red-text");
                        }
                    }

                    textArea.setStyle(start, selection.getEnd(), styles);
                }
            }
        });
    }

    public void addSubject() {
        subjectController = new SubjectController();
        Subject subject = subjectController.getSubject();
        MenuController.setSubjectController(subjectController);

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
