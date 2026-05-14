package myMind.componet;

import javafx.animation.PauseTransition;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.util.Duration;
import lombok.Getter;
import myMind.controller.SubjectController;

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
    // 缩放提示标签
    private Label scaleLabel;

    private double dragStartX, dragStartY;
    private double mousePressedX;
    private double mousePressedY;
    private double paneStartX;
    private double paneStartY;
    private double currentTranslateX = 0;
    private double currentTranslateY = 0;

    public Subject(SubjectController subjectController) {
        this.subjectController = subjectController;

        scaleLabel = new Label("100%");
        scaleLabel.getStyleClass().add("scale-label");
        scaleLabel.setVisible(false);
        scaleLabel.setManaged(false);

        // 让连线不干扰鼠标事件
        linesLayerL.setMouseTransparent(true);
        linesLayerR.setMouseTransparent(true);
        nodesLayer.setMouseTransparent(false);

        getChildren().addAll(linesLayerL, linesLayerR, nodesLayer, scaleLabel);

        addListener();
    }

    private void addListener() {
        setOnScroll(e -> {
            double deltaY = e.getDeltaY();
            if (e.isShortcutDown()) {
                double scale = nodesLayer.getScaleX() + (deltaY > 0 ? 0.1 : -0.1);

                changeScale(scale);
            } else {
                currentTranslateY += deltaY;

                nodesLayer.setTranslateY(currentTranslateY);
                linesLayerR.setTranslateY(currentTranslateY);
                linesLayerL.setTranslateY(currentTranslateY);
            }
        });

        setOnMousePressed(e -> {
            //PRIMARY = 左键
            //SECONDARY = 右键
            //MIDDLE = 滚轮
            if (e.getButton() == MouseButton.PRIMARY) {
                //拖拽画布
                if (e.getTarget() == this || e.getTarget() == nodesLayer) {
                    subjectController.setSelectedNode(null);
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
                MindNode selectedNode = subjectController.getSelectedNode();
                if (selectedNode != null) {
                    dragStartX = mousePressedX - selectedNode.getLayoutX();
                    dragStartY = mousePressedY - selectedNode.getLayoutY();
                }
                e.consume();
            }
        });

        setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY && subjectController.getSelectedNode() != null) {
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
                double deltaX = e.getSceneX() - paneStartX;
                double deltaY = e.getSceneY() - paneStartY;

                currentTranslateX += deltaX;
                currentTranslateY += deltaY;

                // 应用偏移量到图层
                nodesLayer.setTranslateX(currentTranslateX);
                linesLayerR.setTranslateX(currentTranslateX);
                linesLayerL.setTranslateX(currentTranslateX);
                nodesLayer.setTranslateY(currentTranslateY);
                linesLayerR.setTranslateY(currentTranslateY);
                linesLayerL.setTranslateY(currentTranslateY);

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
                currentTranslateY += 300;

                nodesLayer.setTranslateY(currentTranslateY);
                linesLayerR.setTranslateY(currentTranslateY);
                linesLayerL.setTranslateY(currentTranslateY);
                return;
            }

            if (code == KeyCode.PAGE_DOWN || code == KeyCode.SPACE) {
                currentTranslateY -= 300;

                nodesLayer.setTranslateY(currentTranslateY);
                linesLayerR.setTranslateY(currentTranslateY);
                linesLayerL.setTranslateY(currentTranslateY);
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

                nodesLayer.setTranslateX(0);
                linesLayerR.setTranslateX(0);
                linesLayerL.setTranslateX(0);
                nodesLayer.setTranslateY(0);
                linesLayerR.setTranslateY(0);
                linesLayerL.setTranslateY(0);
            }
        });
    }

    private void changeScale(double scale) {
        nodesLayer.setScaleX(scale);
        nodesLayer.setScaleY(scale);
        linesLayerR.setScaleX(scale);
        linesLayerR.setScaleY(scale);
        linesLayerL.setScaleX(scale);
        linesLayerL.setScaleY(scale);

        int percentage = (int) (scale * 100);
        scaleLabel.setText(percentage + "%");
        scaleLabel.setVisible(true);
        scaleLabel.setManaged(true);

        // 定位到中间上方
        scaleLabel.setLayoutX((getWidth() - scaleLabel.prefWidth(-1)) / 2);
        scaleLabel.setLayoutY(scaleLabel.prefHeight(-1));

        // 3秒后隐藏
        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(event -> {
            scaleLabel.setVisible(false);
            scaleLabel.setManaged(false);
        });
        pause.play();
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        //确保两个图层始终填满整个 Subject
        nodesLayer.resizeRelocate(0, 0, getWidth(), getHeight());
        linesLayerR.resizeRelocate(0, 0, getWidth(), getHeight());
    }
}