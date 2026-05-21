package myMind.componet;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import lombok.Getter;
import myMind.controller.SubjectController;
import myMind.util.MessageUtil;

import java.util.HashMap;
import java.util.Map;

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
    private Map<NodeModel, MindNode> modelToView = new HashMap<>();
    private final SubjectController subjectController;

    private double paneStartX;
    private double paneStartY;
    private double currentTranslateX = 0;
    private double currentTranslateY = 0;

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
                double scale = nodesLayer.getScaleX() + (deltaY > 0 ? 0.1 : -0.1);
                changeScale(scale);
            } else {
                for (int i = 0; i < 3; i++) {
                    currentTranslateY += deltaY;
                    translateY(currentTranslateY);
                }
            }
        });

        //拖拽画布
        setOnMousePressed(e -> {
            //PRIMARY = 左键
            //SECONDARY = 右键
            //MIDDLE = 滚轮
            if (e.getButton() == MouseButton.PRIMARY) {
                requestFocus();
                paneStartX = e.getSceneX();
                paneStartY = e.getSceneY();
                e.consume();
            }
        });

        setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                double deltaX = e.getSceneX() - paneStartX;
                double deltaY = e.getSceneY() - paneStartY;

                currentTranslateX += deltaX;
                currentTranslateY += deltaY;

                // 应用偏移量到图层
                translateX(currentTranslateX);
                translateY(currentTranslateY);

                paneStartX = e.getSceneX();
                paneStartY = e.getSceneY();
            }

            e.consume();
        });

        // 键盘快捷键
        setOnKeyPressed(e -> {
            KeyCode code = e.getCode();
            boolean shortcutDown = e.isShortcutDown();

            if (code == KeyCode.PAGE_UP) {
                currentTranslateY += 400;
                translateY(currentTranslateY);
                return;
            }

            if (code == KeyCode.PAGE_DOWN || code == KeyCode.SPACE) {
                currentTranslateY -= 400;
                translateY(currentTranslateY);
                return;
            }

            if (shortcutDown && code == KeyCode.DIGIT0) {
                changeScale(1.0);
                return;
            }

            if (shortcutDown && code == KeyCode.MINUS) {
                double scale = nodesLayer.getScaleX() - 0.1;
                changeScale(scale);
                return;
            }

            if (shortcutDown && code == KeyCode.EQUALS) {
                double scale = nodesLayer.getScaleX() + 0.1;
                changeScale(scale);
                return;
            }

            // 回到中心
            if (shortcutDown && code == KeyCode.G) {
                currentTranslateX = 0;
                currentTranslateY = 0;

                translateX(0);
                translateY(0);
            }
        });
    }

    //———————————————————————————————————————————画布———————————————————————————————————————————

    /**
     * 左右移动画布
     *
     * @param currentTranslateX
     */
    private void translateX(double currentTranslateX) {
        nodesLayer.setTranslateX(currentTranslateX);
        linesLayerR.setTranslateX(currentTranslateX);
        linesLayerL.setTranslateX(currentTranslateX);
    }

    /**
     * 上下移动画布
     *
     * @param currentTranslateY
     */
    private void translateY(double currentTranslateY) {
        nodesLayer.setTranslateY(currentTranslateY);
        linesLayerR.setTranslateY(currentTranslateY);
        linesLayerL.setTranslateY(currentTranslateY);
    }

    /**
     * 改变缩放比例
     *
     * @param scale
     */
    private void changeScale(Double scale) {
        scale = Math.round(scale * 10.0) / 10.0;
        scale = Math.max(0.1, Math.min(scale, 3.0));
        nodesLayer.setScaleX(scale);
        nodesLayer.setScaleY(scale);
        linesLayerR.setScaleX(scale);
        linesLayerR.setScaleY(scale);
        linesLayerL.setScaleX(scale);
        linesLayerL.setScaleY(scale);

        MessageUtil.show(scale);
    }

    //———————————————————————————————————————————增删———————————————————————————————————————————
    public void add(MindNode node) {
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

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        //确保始终填满整个 Subject
        nodesLayer.resizeRelocate(0, 0, getWidth(), getHeight());
        linesLayerR.resizeRelocate(0, 0, getWidth(), getHeight());
        linesLayerL.resizeRelocate(0, 0, getWidth(), getHeight());
    }
}