package myMind.componet;

import javafx.collections.ListChangeListener;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import myMind.constants.PosConstants;
import myMind.controller.SubjectController;
import org.fxmisc.richtext.StyleClassedTextArea;

import java.util.ArrayList;
import java.util.List;

public class Workspace extends TabPane {
    private SubjectController subjectController;
    private MindNode copyNode;

    public Workspace() {
        //关闭按钮的显示策略
        //SELECTED_TAB：只在当前被选中的标签页显示
        //ALL_TABS：在所有标签页上都显示
        //UNAVAILABLE：完全不显示
        setTabClosingPolicy(TabClosingPolicy.ALL_TABS);
        getStyleClass().add("hide-tabs");
        addSubject();

        addListener();
    }

    private void addListener() {
        getSelectionModel().selectedItemProperty().addListener((observable, oldtab, newTab) -> {
            if (newTab == null) {
                return;
            }
            subjectController = ((Subject) newTab.getContent()).getSubjectController();
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
            boolean altDown = e.isAltDown();
            boolean shiftDown = e.isShiftDown();
            KeyCode code = e.getCode();

            // 切换选中节点
            if (shiftDown && altDown) {
                MindNode selectedNode = subjectController.getSelectedNode();
                if (selectedNode == null) {
                    return;
                }
                NodeModel model = selectedNode.getModel();
                byte pos = model.getPos();
                MindNode newNode = selectedNode;

                if (code == KeyCode.RIGHT) {
                    // 左边节点 -> 父节点
                    // 根、右边节点 -> 中间的右子节点
                    if (pos == PosConstants.LEFT) {
                        newNode = model.getParent().getMindNode();
                    } else {
                        List<NodeModel> children = model.getRightChildren();
                        if (!children.isEmpty()) {
                            newNode = children.get(children.size() / 2).getMindNode();
                        }
                    }
                } else if (code == KeyCode.LEFT) {
                    // 父节点 <- 右边节点
                    // 中间的左子节点 <- 左边、根节点
                    if (pos == PosConstants.RIGHT) {
                        newNode = model.getParent().getMindNode();
                    } else {
                        List<NodeModel> children = model.getLeftChildren();
                        if (!children.isEmpty()) {
                            newNode = children.get(children.size() / 2).getMindNode();
                        }
                    }
                } else if (code == KeyCode.UP || code == KeyCode.DOWN) {
                    if (pos == PosConstants.MIDDLE) {
                        return;
                    }

                    // 得到当前节点的索引
                    NodeModel parentModel = model.getParent();
                    List<NodeModel> children;
                    if (pos == PosConstants.RIGHT) {
                        children = parentModel.getRightChildren();
                    } else {
                        children = parentModel.getLeftChildren();
                    }
                    int index = children.indexOf(model);

                    // 切换成上下兄弟节点
                    if (code == KeyCode.UP) {
                        if (index != 0) {
                            newNode = children.get(index - 1).getMindNode();
                        }
                    } else {
                        if (index != children.size() - 1) {
                            newNode = children.get(index + 1).getMindNode();
                        }
                    }
                }

                subjectController.setSelectedNode(newNode);
                return;
            }

            //新增节点
            //Ctrl + Alt 批量新增
            if (shortcutDown && altDown) {
                // 1个子节点和5个孙节点
                if (code == KeyCode.RIGHT) {
                    if (subjectController.getSelectedNode().getModel().getPos() == PosConstants.LEFT) {
                        return;
                    }
                    subjectController.addChildR(null);
                    subjectController.addChildR(null);
                    for (int i = 0; i < 4; i++) {
                        subjectController.addSiblingR();
                    }
                } else if (code == KeyCode.LEFT) {
                    if (subjectController.getSelectedNode().getModel().getPos() == PosConstants.RIGHT) {
                        return;
                    }
                    subjectController.addChildL(null);
                    subjectController.addChildL(null);
                    for (int i = 0; i < 4; i++) {
                        subjectController.addSiblingL();
                    }
                }
                // 1个兄弟节点和5个孙节点
                else if (code == KeyCode.DOWN) {
                    subjectController.addSibling();
                    subjectController.addChild();
                    for (int i = 0; i < 4; i++) {
                        subjectController.addSibling();
                    }
                }
                return;
            } else if (altDown && code == KeyCode.RIGHT) {
                subjectController.addChildR(null);
                return;
            } else if (altDown && code == KeyCode.LEFT) {
                subjectController.addChildL(null);
                return;
            } else if (altDown && code == KeyCode.DOWN) {
                subjectController.addSibling();
                return;
            } else if (altDown && code == KeyCode.UP) {

                return;
            }

            // 删除
            if (altDown && code == KeyCode.DELETE) {
                subjectController.delete();
                return;
            }

            // 节点的复制粘贴
            if (altDown) {
                if (code == KeyCode.C) {
                    MindNode selectedNode = subjectController.getSelectedNode();
                    if (selectedNode.getTextArea().getSelectedText().isEmpty()) {
                        copyNode = selectedNode;
                    }
                } else if (code == KeyCode.X) {
                    MindNode selectedNode = subjectController.getSelectedNode();
                    if (selectedNode.getTextArea().getSelectedText().isEmpty()) {
                        copyNode = selectedNode;
                    }
                    subjectController.delete();
                } else if (code == KeyCode.V) {
                    subjectController.pasteChild(copyNode);
                }
            }

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
//                    List<String> styles = new ArrayList<>(textArea.getStyleAtPosition(start + 1));
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
                return;
            }

            //新增主题
            if (shortcutDown && code == KeyCode.M) {
                addSubject();
            }
        });
    }

    public void addSubject() {
        subjectController = new SubjectController();
        Subject subject = subjectController.getSubject();

        int index = getTabs().size() + 1;
        Tab tab = new Tab("主题" + index);
        tab.setContent(subject);

        getTabs().add(tab);
        getSelectionModel().select(tab);

        // todo 动态计算中心点
//        Platform.runLater(() -> {
//            double centerX = (getWidth() - SizeConstants.MIN_NODE_WIDTH) / 2.0;
//            double centerY = getHeight() / 2.0 - SizeConstants.MIN_NODE_HEIGHT;
//        });
        subjectController.initRootNode(670, 311);
    }

    /**
     * 获取当前选中标签页的控制器
     */
    public SubjectController getCurrentController() {
        Tab selectedTab = getSelectionModel().getSelectedItem();
        subjectController = ((Subject) selectedTab.getContent()).getSubjectController();
        return subjectController;
    }
}
