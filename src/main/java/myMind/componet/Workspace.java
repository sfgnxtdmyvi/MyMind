package myMind.componet;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import myMind.constants.PosConstants;
import myMind.constants.SizeConstants;
import myMind.controller.NodeController;
import org.fxmisc.richtext.StyleClassedTextArea;

import java.util.ArrayList;
import java.util.List;

public class Workspace extends TabPane {
    private NodeController controller;
    private MindNode copyNode;

    public Workspace() {
        //关闭按钮的显示策略
        //SELECTED_TAB：只在当前被选中的标签页显示
        //ALL_TABS：在所有标签页上都显示
        //UNAVAILABLE：完全不显示
        setTabClosingPolicy(TabClosingPolicy.ALL_TABS);
        getStyleClass().add("hide-tabs");
        addNewTab();

        addListener();
    }

    private void addListener() {
        getTabs().addListener((ListChangeListener.Change<? extends Tab> c) -> {
            //只有一个主题时，隐藏标签栏
            if (getTabs().size() <= 1) {
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
                MindNode selectedNode = controller.getSelectedNode();
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
                    } else if (code == KeyCode.DOWN) {
                        if (index != children.size() - 1) {
                            newNode = children.get(index + 1).getMindNode();
                        }
                    }
                }

                controller.setSelectedNode(newNode);
                return;
            }

            //新增节点
            //Ctrl + Alt 批量新增
            if (shortcutDown && altDown) {
                // 1个子节点和5个孙节点
                if (code == KeyCode.LEFT) {
                    controller.addChildL(null);
                    controller.addChildL(null);
                    for (int i = 0; i < 4; i++) {
                        controller.addSiblingL();
                    }
                } else if (code == KeyCode.RIGHT) {
                    controller.addChildR(null);
                    controller.addChildR(null);
                    for (int i = 0; i < 4; i++) {
                        controller.addSiblingR();
                    }
                }
                // 1个兄弟节点和5个孙节点
                else if (code == KeyCode.DOWN) {
                    controller.addSibling();
                    controller.addChild();
                    for (int i = 0; i < 4; i++) {
                        controller.addSibling();
                    }
                }
                return;
            } else if (altDown && code == KeyCode.RIGHT) {
                controller.addChildR(null);
                return;
            } else if (altDown && code == KeyCode.LEFT) {
                controller.addChildL(null);
                return;
            } else if (altDown && code == KeyCode.DOWN) {
                controller.addSibling();
                return;
            } else if (altDown && code == KeyCode.UP) {

                return;
            }

            // 删除
            if (altDown && code == KeyCode.DELETE) {
                controller.delete();
                return;
            }

            // 节点的复制粘贴
            if (altDown) {
                if (code == KeyCode.C) {
                    MindNode selectedNode = controller.getSelectedNode();
                    if (selectedNode.getTextArea().getSelectedText().isEmpty()) {
                        copyNode = selectedNode;
                    }
                } else if (code == KeyCode.X) {
                    MindNode selectedNode = controller.getSelectedNode();
                    if (selectedNode.getTextArea().getSelectedText().isEmpty()) {
                        copyNode = selectedNode;
                    }
                    controller.delete();
                } else if (code == KeyCode.V) {
                    controller.pasteChild(copyNode);
                }
            }

            // 文本样式
            if (shortcutDown && (code == KeyCode.B || code == KeyCode.R)) {
                MindNode selectedNode = controller.getSelectedNode();
                StyleClassedTextArea textArea = selectedNode.getTextArea();
                IndexRange selection = textArea.getSelection();

                if (selection.getLength() > 0) {
                    int start = selection.getStart();
                    List<String> styles = new ArrayList<>(textArea.getStyleOfChar(start));
                    // getStyleAtPosition(p) is equivalent to getStyleOfChar(p-1)
                    // 用于getStyleAtPosition获取的是指定位置的前一个位置的样式
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
                addNewTab();
            }
        });
    }

    public void addNewTab() {
        NodeController nodeController = new NodeController();
        Subject subject = nodeController.getSubject();

        int index = getTabs().size() + 1;
        Tab tab = new Tab("主题" + index);
        tab.setContent(subject);

        getTabs().add(tab);
        getSelectionModel().select(tab);

        Platform.runLater(() -> {
            double centerX = (getWidth() - SizeConstants.MIN_NODE_WIDTH) / 2.0;
            double centerY = getHeight() / 2.0 - SizeConstants.MIN_NODE_HEIGHT;
            nodeController.initRootNode(centerX, centerY);
        });
    }

    /**
     * 获取当前选中标签页的控制器
     */
    public NodeController getCurrentController() {
        Tab selectedTab = getSelectionModel().getSelectedItem();
        controller = ((Subject) selectedTab.getContent()).getController();
        return controller;
    }
}
