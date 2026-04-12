package myMind.componet;

import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import lombok.Getter;
import myMind.controller.MindController;

/**
 * 主控面板，放结点和连线
 */
@Getter
public class MindPane extends Pane {

    /**
     * 结点层
     */
    private final Pane nodesLayer = new Pane();
    /**
     * 连线层
     */
    private final Pane linesLayer = new Pane();

    public MindPane(MindController controller) {
        // 让连线不干扰鼠标事件
        linesLayer.setMouseTransparent(true);
        nodesLayer.setMouseTransparent(false);
        getChildren().addAll(linesLayer, nodesLayer);

        // 键盘快捷键
        setOnKeyPressed(e -> {
            // 新增结点
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

            // 删除结点
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