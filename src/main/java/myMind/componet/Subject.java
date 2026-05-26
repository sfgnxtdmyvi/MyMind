package myMind.componet;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.QuadCurve;
import lombok.Getter;
import myMind.controller.SubjectController;
import myMind.util.MessageUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * 主题，放节点和连线
 */
@Getter
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
    private final Map<NodeModel, MindNode> modelToView = new HashMap<>();
    private final SubjectController subjectController;

    private double dragStartX;
    private double dragStartY;

    public Subject(SubjectController subjectController) {
        this.subjectController = subjectController;

        // 让连线不干扰鼠标事件
        linesLayerL.setMouseTransparent(true);
        linesLayerR.setMouseTransparent(true);
        nodesLayer.setMouseTransparent(false);

        getChildren().addAll(linesLayerL, linesLayerR, nodesLayer);

        addListener();
    }

    private void addListener() {
        // EventFilter 能让节点不干扰鼠标滚动
        addEventFilter(ScrollEvent.SCROLL, e -> {
            double deltaY = e.getDeltaY();
            if (e.isShortcutDown()) {
                double scale = getScaleX() + (deltaY > 0 ? 0.1 : -0.1);
                changeScale(scale);
            } else {
                for (int i = 0; i < 3; i++) {
                    setTranslateY(getTranslateY() + deltaY);
                }
            }
        });

        //拖拽画布
        setOnMousePressed(e -> {
            //PRIMARY = 左键
            //SECONDARY = 右键
            //MIDDLE = 滚轮
            if (e.getButton() == MouseButton.PRIMARY && e.getTarget() == nodesLayer) {
                subjectController.setSelectedModel(null);
                dragStartX = e.getSceneX();
                dragStartY = e.getSceneY();
            }
            e.consume();
        });

        setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY && isFocused()) {
                double translateX = getTranslateX() + e.getSceneX() - dragStartX;
                double translateY = getTranslateY() + e.getSceneY() - dragStartY;

                // 应用偏移量到图层
                setTranslateX(translateX);
                setTranslateY(translateY);

                dragStartX = e.getSceneX();
                dragStartY = e.getSceneY();
            }
            e.consume();
        });

        // 键盘快捷键
        setOnKeyPressed(e -> {
            KeyCode code = e.getCode();
            boolean shortcutDown = e.isShortcutDown();

            if (code == KeyCode.PAGE_UP) {
                setTranslateY(getTranslateY() + 400);
                return;
            }

            if (code == KeyCode.PAGE_DOWN || code == KeyCode.SPACE) {
                setTranslateY(getTranslateY() - 400);
                return;
            }

            // 回到中心
            if (shortcutDown && code == KeyCode.G) {
                setTranslateX(0);
                setTranslateY(0);
                return;
            }

            if (shortcutDown) {
                if (code == KeyCode.DIGIT0) {
                    changeScale(1.0);
                } else if (code == KeyCode.MINUS) {
                    changeScale(getScaleX() - 0.1);
                } else if (code == KeyCode.EQUALS) {
                    changeScale(getScaleX() + 0.1);
                }
            }
        });
    }

    /**
     * 改变缩放比例
     *
     * @param scale
     */
    private void changeScale(Double scale) {
        scale = Math.round(scale * 10.0) / 10.0;
        scale = Math.max(0.1, Math.min(scale, 3.0));
        setScaleX(scale);
        setScaleY(scale);

        MessageUtil.showScale(scale);
    }

    //———————————————————————————————————————————节点———————————————————————————————————————————
    public void addNode(MindNode node) {
        nodesLayer.getChildren().add(node);
        modelToView.put(node.getModel(), node);
    }

    public void addClone(Map<NodeModel, MindNode> cloneMap) {
        ObservableList<Node> children = nodesLayer.getChildren();

        for (Map.Entry<NodeModel, MindNode> entry : cloneMap.entrySet()) {
            MindNode node = entry.getValue();
            children.add(node);
            modelToView.put(entry.getKey(), node);
        }
    }

    public void remove(NodeModel model) {
        MindNode node = modelToView.remove(model);
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