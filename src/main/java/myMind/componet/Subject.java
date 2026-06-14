package myMind.componet;

import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.QuadCurve;
import lombok.Data;
import myMind.constants.SizeConstants;
import myMind.util.MessageUtil;

@Data
public class Subject extends StackPane {
    /**
     * 节点层
     */
    private final Pane nodesLayer = new Pane();
    /**
     * 连线层
     */
    private final Pane linesLayerR = new Pane();
    private final Pane linesLayerL = new Pane();

    private double dragStartX;
    private double dragStartY;

    public Subject() {
        // 让连线不干扰鼠标事件
        linesLayerL.setMouseTransparent(true);
        linesLayerR.setMouseTransparent(true);
        nodesLayer.setMouseTransparent(false);
        getChildren().addAll(linesLayerL, linesLayerR, nodesLayer);

        // 阻止焦点从 textArea 转移出去，导致清空选区，进行无法唤出样式轮盘
        // 不能用空处理器，如果事件冒泡给 TabPane，在 setOnMousePressed 之前就会获取焦点
        setOnMousePressed(e -> {
            //PRIMARY = 左键
            //SECONDARY = 右键
            //MIDDLE = 滚轮
            if (e.getButton() == MouseButton.SECONDARY) {
                e.consume();
            }
        });

        // 限制 Subject 的移动
        translateXProperty().addListener((observable, oldValue, newValue) -> {
            double dx = 0;
            // 父容器视口大小
            Bounds parentBounds = getParent().getLayoutBounds();
            double parentWidth = parentBounds.getWidth();
            // Subject（包含所有子节点）在父容器中的实际边界
            Bounds subjectBounds = getBoundsInParent();

            //        |                 |
            // 视口的左边缘（0） 视口的右边缘（parentWidth）
            //         |                    |
            //  Subject 的左边缘（10） Subject 的右边缘（parentWidth+10）
            // - subjectBounds.getMinX()后 Subject 左边缘与视口的左边缘重合
            //        |        |            |
            // 视口的左边缘（0） 200 视口的右边缘（parentWidth）
            //                  |                     |
            //          Subject 的左边缘（210） Subject 的右边缘（parentWidth+210）
            // 调整后 Subject 左边缘与 SizeConstants.TRANSLATE_OFFSET 重合，中间存在 SizeConstants.TRANSLATE_OFFSET 的间隔

            // 变小后，四周的间隔要变大
            // 0 200         800 1000
            // 0     400 600     1000
            double translateConstrain = getScaleX() < 1 ? SizeConstants.TRANSLATE_CONSTRAIN / getScaleX() : SizeConstants.TRANSLATE_CONSTRAIN;
            if (translateConstrain < subjectBounds.getMinX()) {
                dx = translateConstrain - subjectBounds.getMinX();
            } else if (subjectBounds.getMaxX() < parentWidth - translateConstrain) {
                dx = parentWidth - translateConstrain - subjectBounds.getMaxX();
            }

            setTranslateX(getTranslateX() + dx);
        });

        translateYProperty().addListener((observable, oldValue, newValue) -> {
            double dy = 0;
            Bounds parentBounds = getParent().getLayoutBounds();
            double parentHeight = parentBounds.getHeight();
            Bounds subjectBounds = getBoundsInParent();

            double translateOffset = SizeConstants.TRANSLATE_CONSTRAIN / getScaleX();
            if (translateOffset < subjectBounds.getMinY()) {
                dy = translateOffset - subjectBounds.getMinY();
            } else if (subjectBounds.getMaxY() < parentHeight - translateOffset) {
                dy = parentHeight - translateOffset - subjectBounds.getMaxY();
            }
            setTranslateY(getTranslateY() + dy);
        });
    }

    /**
     * 改变缩放比例
     *
     * @param scale
     */
    public void changeScale(Double scale) {
        scale = Math.round(scale * 10.0) / 10.0;
        scale = Math.max(0.5, Math.min(scale, 2.0));
        setScaleX(scale);
        setScaleY(scale);

        MessageUtil.showScale(scale);
    }

    //———————————————————————————————————————————节点———————————————————————————————————————————
    public void addNode(MindNode node) {
        nodesLayer.getChildren().add(node);
    }

    public void addClone(MindNode CloneNode) {
        addNode(CloneNode);
        addCloneChildrenR(CloneNode);
        addCloneChildrenL(CloneNode);
    }

    public void addCloneChildrenR(MindNode CloneNode) {
        ObservableList<Node> children = nodesLayer.getChildren();
        for (MindNode node : CloneNode.getChildrenR()) {
            children.add(node);
            addCloneChildrenR(node);
        }
    }

    public void addCloneChildrenL(MindNode CloneNode) {
        ObservableList<Node> children = nodesLayer.getChildren();
        for (MindNode node : CloneNode.getChildrenL()) {
            children.add(node);
            addCloneChildrenL(node);
        }
    }

    public void remove(MindNode node) {
        nodesLayer.getChildren().remove(node);
    }

    //———————————————————————————————————————————连线———————————————————————————————————————————
    public void addLineR(QuadCurve line) {
        linesLayerR.getChildren().add(line);
    }

    public void addLineL(QuadCurve line) {
        linesLayerL.getChildren().add(line);
    }

    public void clearLineR() {
        linesLayerR.getChildren().clear();
    }

    public void clearLineL() {
        linesLayerL.getChildren().clear();
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        //确保始终填满整个 Subject
        double width = getWidth();
        double height = getHeight();
        nodesLayer.resizeRelocate(0, 0, width, height);
        linesLayerR.resizeRelocate(0, 0, width, height);
        linesLayerL.resizeRelocate(0, 0, width, height);
    }
}