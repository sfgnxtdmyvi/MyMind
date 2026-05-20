package myMind.componet;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import myMind.constants.PosConstants;
import myMind.controller.NodeController;
import myMind.util.CopyNodeUtil;
import org.fxmisc.richtext.StyleClassedTextArea;

import java.util.ArrayList;
import java.util.List;

public class Workspace extends TabPane {
    private NodeController nodeController;

    public Workspace() {
        //关闭按钮的显示策略
        //SELECTED_TAB：只在当前被选中的标签页显示
        //ALL_TABS：在所有标签页上都显示
        //UNAVAILABLE：完全不显示
        setTabClosingPolicy(TabClosingPolicy.ALL_TABS);
        getStyleClass().add("hide-tabs");
        addSubject();
        Platform.runLater(() -> nodeController.getSelectedNode().getTextArea().requestFocus());

        addListener();
    }

    private void addListener() {
        getSelectionModel().selectedItemProperty().addListener((observable, oldtab, newTab) -> {
            if (newTab == null) {
                return;
            }
            nodeController = ((Subject) newTab.getContent()).getNodeController();
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
                NodeModel selectedModel = nodeController.getSelectedModel();
                if (selectedModel == null) {
                    return;
                }
                byte pos = selectedModel.getPos();

                if (code == KeyCode.RIGHT) {
                    // 左边节点 -> 父节点
                    // 根、右边节点 -> 中间的右子节点
                    if (pos == PosConstants.LEFT) {
                        selectedModel = selectedModel.getParent();
                    } else {
                        List<NodeModel> children = selectedModel.getChildrenR();
                        if (!children.isEmpty()) {
                            selectedModel = children.get(children.size() / 2);
                        }
                    }
                } else if (code == KeyCode.LEFT) {
                    // 父节点 <- 右边节点
                    // 中间的左子节点 <- 左边、根节点
                    if (pos == PosConstants.RIGHT) {
                        selectedModel = selectedModel.getParent();
                    } else {
                        List<NodeModel> children = selectedModel.getChildrenL();
                        if (!children.isEmpty()) {
                            selectedModel = children.get(children.size() / 2);
                        }
                    }
                } else if (code == KeyCode.UP || code == KeyCode.DOWN) {
                    if (pos == PosConstants.MIDDLE) {
                        return;
                    }

                    // 得到当前节点的索引
                    NodeModel parentModel = selectedModel.getParent();
                    List<NodeModel> children;
                    if (pos == PosConstants.RIGHT) {
                        children = parentModel.getChildrenR();
                    } else {
                        children = parentModel.getChildrenL();
                    }
                    int index = children.indexOf(selectedModel);

                    // 切换成上下兄弟节点
                    if (code == KeyCode.UP) {
                        if (index != 0) {
                            selectedModel = children.get(index - 1);
                        }
                    } else {
                        if (index != children.size() - 1) {
                            selectedModel = children.get(index + 1);
                        }
                    }
                }

                nodeController.setSelectedModel(selectedModel);
                return;
            }

            //新增节点
            //Ctrl + Alt 批量新增
            if (shortcutDown && altDown) {
                // 1个子节点和5个孙节点
                if (code == KeyCode.RIGHT) {
                    if (nodeController.getSelectedModel().getPos() == PosConstants.LEFT) {
                        return;
                    }
                    nodeController.addChildR();
                    nodeController.addChildR();
                    for (int i = 0; i < 4; i++) {
                        nodeController.addSiblingR();
                    }
                } else if (code == KeyCode.LEFT) {
                    if (nodeController.getSelectedModel().getPos() == PosConstants.RIGHT) {
                        return;
                    }
                    nodeController.addChildL();
                    nodeController.addChildL();
                    for (int i = 0; i < 4; i++) {
                        nodeController.addSiblingL();
                    }
                }
                // 1个兄弟节点和5个孙节点
                else if (code == KeyCode.DOWN) {
                    nodeController.addSibling();
                    nodeController.addChild();
                    for (int i = 0; i < 4; i++) {
                        nodeController.addSibling();
                    }
                }
                return;
            } else if (altDown && code == KeyCode.RIGHT) {
                nodeController.addChildR();
                return;
            } else if (altDown && code == KeyCode.LEFT) {
                nodeController.addChildL();
                return;
            } else if (altDown && code == KeyCode.DOWN) {
                nodeController.addSibling();
                return;
            } else if (altDown && code == KeyCode.UP) {

                return;
            }

            // 删除
            if (altDown && code == KeyCode.DELETE) {
                nodeController.delete();
                return;
            }

            // 节点的复制粘贴
            if (shortcutDown && shiftDown) {
                if (code == KeyCode.C) {
                    CopyNodeUtil.set(nodeController.copy(nodeController.getSelectedModel()));
                } else if (code == KeyCode.X) {
                    CopyNodeUtil.set(nodeController.cut());
                }
                return;
            }

            // 文本样式
            if (shortcutDown && (code == KeyCode.B || code == KeyCode.R)) {
                MindNode selectedNode = nodeController.getSelectedNode();
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
        nodeController = new NodeController();
        Subject subject = nodeController.getSubject();

        int index = getTabs().size() + 1;
        Tab tab = new Tab();
        tab.setContent(subject);

        getTabs().add(tab);
        getSelectionModel().select(tab);

        // todo 动态计算中心点
//        Platform.runLater(() -> {
//            double centerX = (getWidth() - SizeConstants.MIN_NODE_WIDTH) / 2.0;
//            double centerY = getHeight() / 2.0 - SizeConstants.MIN_NODE_HEIGHT;
//        });
        nodeController.initRootNode(670, 311);
        tab.textProperty().bind(nodeController.getRootNode().getTextArea().textProperty());
    }

    /**
     * 获取当前选中标签页的控制器
     */
    public NodeController getCurrentController() {
        Tab selectedTab = getSelectionModel().getSelectedItem();
        nodeController = ((Subject) selectedTab.getContent()).getNodeController();
        return nodeController;
    }
}
