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

    private double dragStartX, dragStartY;
    private double mousePressedX;
    private double mousePressedY;

    public MindPane(NodeController controller) {
        // 让连线不干扰鼠标事件
        linesLayer.setMouseTransparent(true);
        nodesLayer.setMouseTransparent(false);
        getChildren().addAll(linesLayer, nodesLayer);

        addListener(controller);
    }

    private void addListener(NodeController controller) {
        // 鼠标拖拽移动节点
        setOnMousePressed(e -> {
            //PRIMARY = 左键
            //SECONDARY = 右键
            //MIDDLE = 滚轮
            if (e.getButton() == MouseButton.PRIMARY) {
                if (e.getTarget() == this || e.getTarget() == nodesLayer) {
                    controller.setSelectedNode(null);
                    return;
                }
                // 获取鼠标按下时的坐标
                mousePressedX = e.getSceneX();
                mousePressedY = e.getSceneY();

                //记录拖拽起始位置
                //鼠标距离节点左边缘的距离
                MindNode selectedNode = controller.getSelectedNode();
                if (selectedNode != null) {
                    dragStartX = mousePressedX - selectedNode.getLayoutX();
                    dragStartY = mousePressedY - selectedNode.getLayoutY();
                }
                e.consume();
            }
        });

        setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                MindNode selectedNode = controller.getSelectedNode();
                if (selectedNode != null) {
                    double newX = e.getSceneX() - dragStartX;
                    double newY = e.getSceneY() - dragStartY;
                    // 限制边界防止拖出视野外
                    newX = Math.max(20, Math.min(newX, getWidth() - selectedNode.getWidth()));
                    newY = Math.max(20, Math.min(newY, getHeight() - selectedNode.getHeight()));
                    selectedNode.getModel().setX(newX);
                    selectedNode.getModel().setY(newY);
                    controller.refreshLines();
                }
                e.consume();
            }
        });

        setOnMouseReleased(e -> {
            // Todo 拖拽移动节点
        });

        // 键盘快捷键
        setOnKeyPressed(e -> {
            // 新增节点
            // Alt + → 右方向键
            if (e.isAltDown() && e.getCode() == KeyCode.RIGHT) {
                controller.addChild();
                e.consume();
            }
            // Alt + ← 左方向键
            else if (e.isAltDown() && e.getCode() == KeyCode.LEFT) {
                controller.addChild();
                e.consume();
            }
            // Alt + ↓ 下方向键
            else if (e.isAltDown() && e.getCode() == KeyCode.DOWN) {
                controller.addSibling();
                e.consume();
            }

            // 删除节点
            // Alt + Delete
            else if (e.isAltDown() && e.getCode() == KeyCode.DELETE) {
                controller.delete();
                e.consume();
            }
        });
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        //确保两个图层始终填满整个 MindPane
        nodesLayer.resizeRelocate(0, 0, getWidth(), getHeight());
        linesLayer.resizeRelocate(0, 0, getWidth(), getHeight());
    }
}