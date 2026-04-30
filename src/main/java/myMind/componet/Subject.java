package myMind.componet;

import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import lombok.Getter;
import myMind.constants.PosConstants;
import myMind.controller.NodeController;

import java.util.List;

/**
 * 主控面板，放节点和连线
 */
@Getter
public class Subject extends Pane {
    /**
     * 节点层
     */
    private final Pane nodesLayer = new Pane();
    /**
     * 连线层
     */
    private final Pane linesLayerR = new Pane();
    private final Pane linesLayerL = new Pane();
    private final NodeController controller;

    private double dragStartX, dragStartY;
    private double mousePressedX;
    private double mousePressedY;
    private double panStartX, panStartY;
    private double currentTranslateX = 0;
    private double currentTranslateY = 0;

    public Subject(NodeController controller) {
        this.controller = controller;

        // 让连线不干扰鼠标事件
        linesLayerL.setMouseTransparent(true);
        linesLayerR.setMouseTransparent(true);
        nodesLayer.setMouseTransparent(false);
        getChildren().addAll(linesLayerL, linesLayerR, nodesLayer);

        addListener();
    }

    private void addListener() {
        setOnMousePressed(e -> {
            //PRIMARY = 左键
            //SECONDARY = 右键
            //MIDDLE = 滚轮
            if (e.getButton() == MouseButton.PRIMARY) {
                //拖拽画布
                if (e.getTarget() == this || e.getTarget() == nodesLayer) {
                    controller.setSelectedNode(null);
                    panStartX = e.getSceneX();
                    panStartY = e.getSceneY();
                    e.consume();
                    return;
                }

                // 拖拽节点
                // 获取鼠标按下时的坐标
                mousePressedX = e.getSceneX();
                mousePressedY = e.getSceneY();

                //记录拖拽起始位置
                //鼠标距离节点左上角的距离
                MindNode selectedNode = controller.getSelectedNode();
                if (selectedNode != null) {
                    dragStartX = mousePressedX - selectedNode.getLayoutX();
                    dragStartY = mousePressedY - selectedNode.getLayoutY();
                }
                e.consume();
            }
        });

        setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY && controller.getSelectedNode() != null) {
//                MindNode selectedNode = controller.getSelectedNode();
//                double newX = e.getSceneX() - dragStartX;
//                double newY = e.getSceneY() - dragStartY;
//                // 限制边界防止拖出视野外
//                newX = Math.max(20, Math.min(newX, getWidth() - selectedNode.getWidth()));
//                newY = Math.max(20, Math.min(newY, getHeight() - selectedNode.getHeight()));
//                selectedNode.getModel().setX(newX);
//                selectedNode.getModel().setY(newY);
//                controller.refreshLines();
            }
            // 移动画布
            else if (e.getButton() == MouseButton.PRIMARY) {
                double deltaX = e.getSceneX() - panStartX;
                double deltaY = e.getSceneY() - panStartY;

                currentTranslateX += deltaX;
                currentTranslateY += deltaY;

                // 应用偏移量到图层
                nodesLayer.setTranslateX(currentTranslateX);
                linesLayerR.setTranslateX(currentTranslateX);
                linesLayerL.setTranslateX(currentTranslateX);
                nodesLayer.setTranslateY(currentTranslateY);
                linesLayerR.setTranslateY(currentTranslateY);
                linesLayerL.setTranslateY(currentTranslateY);

                panStartX = e.getSceneX();
                panStartY = e.getSceneY();
            }

            e.consume();
        });

        setOnMouseReleased(e -> {
            // Todo 拖拽移动节点
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
                    controller.addChildL();
                    controller.addChildL();
                    for (int i = 0; i < 4; i++) {
                        controller.addSiblingL();
                    }
                } else if (code == KeyCode.RIGHT) {
                    controller.addChildR();
                    controller.addChildR();
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
                controller.addChildR();
                return;
            } else if (altDown && code == KeyCode.LEFT) {
                controller.addChildL();
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

            // 回到中心
            if (shortcutDown && code == KeyCode.G) {
                currentTranslateX = 0;
                currentTranslateY = 0;

                nodesLayer.setTranslateX(0);
                linesLayerR.setTranslateX(0);
                nodesLayer.setTranslateY(0);
                linesLayerR.setTranslateY(0);
            }
        });
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        //确保两个图层始终填满整个 Subject
        nodesLayer.resizeRelocate(0, 0, getWidth(), getHeight());
        linesLayerR.resizeRelocate(0, 0, getWidth(), getHeight());
    }
}