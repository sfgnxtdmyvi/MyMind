package myMind.componet;

import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import lombok.Getter;
import myMind.controller.NodeController;

/**
 * 主控面板，放节点和连线
 */
@Getter
public class MindPane extends Pane {
    /**
     * 节点层
     */
    private final Pane nodesLayer = new Pane();
    /**
     * 连线层
     */
    private final Pane linesLayer = new Pane();
    private final NodeController controller;

    private double dragStartX, dragStartY;
    private double mousePressedX;
    private double mousePressedY;
    private double panStartX, panStartY;
    private double currentTranslateX = 0;
    private double currentTranslateY = 0;

    public MindPane(NodeController controller) {
        this.controller = controller;

        // 让连线不干扰鼠标事件
        linesLayer.setMouseTransparent(true);
        nodesLayer.setMouseTransparent(false);
        getChildren().addAll(linesLayer, nodesLayer);

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
                MindNode selectedNode = controller.getSelectedNode();
                double newX = e.getSceneX() - dragStartX;
                double newY = e.getSceneY() - dragStartY;
                // 限制边界防止拖出视野外
                newX = Math.max(20, Math.min(newX, getWidth() - selectedNode.getWidth()));
                newY = Math.max(20, Math.min(newY, getHeight() - selectedNode.getHeight()));
                selectedNode.getModel().setX(newX);
                selectedNode.getModel().setY(newY);
                controller.refreshLines();
            }
            // 移动画布
            else if (e.getButton() == MouseButton.PRIMARY) {
                double deltaX = e.getSceneX() - panStartX;
                double deltaY = e.getSceneY() - panStartY;

                currentTranslateX += deltaX;
                currentTranslateY += deltaY;

                // 应用偏移量到图层
                nodesLayer.setTranslateX(currentTranslateX);
                linesLayer.setTranslateX(currentTranslateX);
                nodesLayer.setTranslateY(currentTranslateY);
                linesLayer.setTranslateY(currentTranslateY);

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
            boolean isControl = e.isControlDown();
            boolean isAltDown = e.isAltDown();
            KeyCode code = e.getCode();

            // 处理删除操作
            if (isAltDown && code == KeyCode.DELETE) {
                controller.delete();
                e.consume();
                return;
            }

            // 处理节点新增操作
            if (isControl && isAltDown) {
                handleComplexAdd(code);
            } else if (isAltDown) {
                switch (code) {
                    case RIGHT:
                        controller.addChild();
                        break;
                    case LEFT:
                        controller.addChild();
                        break;
                    case DOWN:
                        controller.addSibling();
                        break;
                    default:
                        break;
                }
            }

            // 回到中心
            if (isControl && code == KeyCode.B) {
                currentTranslateX = 0;
                currentTranslateY = 0;
                
                nodesLayer.setTranslateX(0);
                linesLayer.setTranslateX(0);
                nodesLayer.setTranslateY(0);
                linesLayer.setTranslateY(0);
            }
            e.consume();
        });
    }

    /**
     * 处理 Ctrl + Alt 组合键的复杂节点添加逻辑
     */
    private void handleComplexAdd(KeyCode code) {
        // 1个子节点和5个孙节点
        if (code == KeyCode.LEFT) {
            controller.addChild();
            controller.addChild();
            addSibling4();
        } else if (code == KeyCode.RIGHT) {
            controller.addChild();
            controller.addChild();
            addSibling4();
        }
        // 1个兄弟节点和5个孙节点
        else if (code == KeyCode.DOWN) {
            controller.addSibling();
            controller.addChild();
            addSibling4();
        }
    }

    private void addSibling4() {
        controller.addSibling();
        controller.addSibling();
        controller.addSibling();
        controller.addSibling();
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        //确保两个图层始终填满整个 MindPane
        nodesLayer.resizeRelocate(0, 0, getWidth(), getHeight());
        linesLayer.resizeRelocate(0, 0, getWidth(), getHeight());
    }
}