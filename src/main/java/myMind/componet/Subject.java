package myMind.componet;

import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import lombok.Getter;
import myMind.controller.SubjectController;
import myMind.util.MessageUtil;

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
    private final SubjectController subjectController;

    private double dragStartX, dragStartY;
    private double mousePressedX;
    private double mousePressedY;
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

        setOnMousePressed(e -> {
            //PRIMARY = 左键
            //SECONDARY = 右键
            //MIDDLE = 滚轮
            if (e.getButton() == MouseButton.PRIMARY) {
                //拖拽画布
                if (e.getTarget() == this || e.getTarget() == nodesLayer) {
                    subjectController.setSelectedModel(null);
                    paneStartX = e.getSceneX();
                    paneStartY = e.getSceneY();
                    e.consume();
                    return;
                }

                // 拖拽节点
                // 获取鼠标按下时的坐标
                mousePressedX = e.getSceneX();
                mousePressedY = e.getSceneY();

                //记录拖拽起始位置
                //鼠标距离节点左上角的距离
                NodeModel selectedModel = subjectController.getSelectedModel();
                if (selectedModel != null) {
                    dragStartX = mousePressedX - selectedModel.getX();
                    dragStartY = mousePressedY - selectedModel.getMindNode().getLayoutY();
                }
                e.consume();
            }
        });

        setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY && subjectController.getSelectedModel() != null) {
//                MindNode selectedNode = controller.getSelectedNode();
//                double newX = e.getSceneX() - dragStartX;
//                double newY = e.getSceneY() - dragStartY;
//                // 限制边界防止拖出视野外
//                newX = Math.max(20, Math.min(newX, getWidth() - selectedNode.getWidth()));
//                newY = Math.max(20, Math.min(newY, getHeight() - selectedNode.getHeight()));
//                selectedNode.getModel().setX(newX);
//                selectedNode.setLayoutY(newY);
//                controller.refreshLines();
            }
            // 移动画布
            else if (e.getButton() == MouseButton.PRIMARY) {
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

        setOnMouseReleased(e -> {
            // Todo 拖拽移动节点
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

    public void add(MindNode node) {
        nodesLayer.getChildren().add(node);
    }

    public void remove(MindNode node) {
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