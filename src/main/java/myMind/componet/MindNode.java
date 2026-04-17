package myMind.componet;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import lombok.Data;
import lombok.Getter;
import myMind.constants.PosConstants;
import myMind.constants.SizeConstants;
import myMind.controller.NodeController;
import org.fxmisc.richtext.InlineCssTextArea;

import java.util.List;

//@Data 会自动生成 hashCode() 方法
//循环引用时，会无限递归调用双方的 hashCode() 方法
@Getter
public class MindNode extends VBox{
    private final NodeModel model;
    private final InlineCssTextArea textArea;
    private final NodeController controller;
    //用于测量文本尺寸
    private Text measureText;
    private ImageView imageView; // 新增图片视图

    // 拖拽缩放相关变量
    private static final double RESIZE_THRESHOLD = 5.0;
    private boolean isResizing = false;
    private double startX, startY, startWidth, startHeight;

    public MindNode(NodeModel model, NodeController controller) {
        this.model = model;
        model.setMindNode(this);
        this.controller = controller;

        measureText = new Text();
        measureText.setFont(Font.font("System", SizeConstants.NODE_FONT_SIZE));

        // 初始化图片视图
        imageView = new ImageView();
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setVisible(false);
        imageView.setManaged(false);

        textArea = new InlineCssTextArea();
        textArea.replaceText(0, 0, model.getText());
        textArea.setWrapText(true);
        textArea.getStyleClass().add("nodeContent");
        // 让绘制连线时，能获取节点位置
        textArea.setPrefWidth(SizeConstants.MIN_TEXTAREA_WIDTH);
        textArea.setPrefHeight(SizeConstants.MIN_TEXTAREA_HEIGHT);

        setPrefWidth(SizeConstants.MIN_NODE_WIDTH);
        setPrefHeight(SizeConstants.MIN_NODE_HEIGHT);
        getStyleClass().add("nodeBorder");
        setPadding(new Insets(10, 10, 10, 10));
        // 图片和文本之间的间距
        setSpacing(5);

        // 将图片和文本加入 VBox
        getChildren().addAll(imageView, textArea);
//        VBox.setVgrow(textArea, Priority.ALWAYS);

        // 模型x、y变化时，改变位置
        model.xProperty().addListener((obs, oldVal, newVal) -> setLayoutX(newVal.doubleValue()));
        model.yProperty().addListener((obs, oldVal, newVal) -> setLayoutY(newVal.doubleValue()));
        setLayoutX(model.getX());
        setLayoutY(model.getY());

        // 初始调整尺寸
        Platform.runLater(this::adjustSize);

        addListener();
    }

    private void addListener() {
        // 选中节点
        addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            controller.setSelectedNode(this);
        });

        // 粘贴图片
        textArea.setOnKeyReleased(e -> {
            if (e.isControlDown() && e.getCode() == KeyCode.V) {
                Clipboard clipboard = Clipboard.getSystemClipboard();
                if (clipboard.hasImage()) {
                    Image image = clipboard.getImage();
                    setImage(image);
                    e.consume();
                }
            }
        });

        // 文本变化动态调整
        textArea.textProperty().addListener((obs, oldText, newText) -> {
            Platform.runLater(this::adjustSize);
        });

        textArea.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) finishEdit(textArea.getText(), controller);
        });

        // --- 图片缩放逻辑 ---
        imageView.setOnMousePressed(e -> {
            if (imageView.isVisible()) {
                startX = e.getSceneX();
                startY = e.getSceneY();
                startWidth = imageView.getFitWidth();
                startHeight = imageView.getFitHeight();

                // 判断是否点击在右下角区域用于缩放
                if (e.getX() > imageView.getBoundsInLocal().getWidth() - RESIZE_THRESHOLD &&
                        e.getY() > imageView.getBoundsInLocal().getHeight() - RESIZE_THRESHOLD) {
                    isResizing = true;
                    imageView.setCursor(Cursor.SE_RESIZE);
                }
            }
        });

        imageView.setOnMouseDragged(e -> {
            if (isResizing) {
                double deltaX = e.getSceneX() - startX;
                double deltaY = e.getSceneY() - startY;

                // 简单的等比例缩放或自由缩放，这里演示自由缩放
                double newWidth = Math.max(20, startWidth + deltaX);
                double newHeight = Math.max(20, startHeight + deltaY);

                imageView.setFitWidth(newWidth);
                imageView.setFitHeight(newHeight);

                // 图片大小改变后，需要重新计算节点整体尺寸
                adjustSize();
            }
        });

        imageView.setOnMouseReleased(e -> {
            isResizing = false;
            imageView.setCursor(Cursor.DEFAULT);
        });

        // 鼠标悬停在右下角显示缩放光标
        imageView.setOnMouseMoved(e -> {
            if (e.getX() > imageView.getBoundsInLocal().getWidth() - RESIZE_THRESHOLD &&
                    e.getY() > imageView.getBoundsInLocal().getHeight() - RESIZE_THRESHOLD) {
                imageView.setCursor(Cursor.SE_RESIZE);
            } else {
                imageView.setCursor(Cursor.DEFAULT);
            }
        });
    }

    /**
     * 设置节点图片
     */
    public void setImage(Image image) {
        if (image != null) {
            imageView.setImage(image);
            imageView.setVisible(true);
            imageView.setManaged(true);
            imageView.setFitWidth(100);
            imageView.setFitHeight(100);
            adjustSize();
        }
    }

    private void finishEdit(String newText, NodeController controller) {
        model.setText(newText);
        controller.refreshLines();
    }

    /**
     * 根据内容动态调整尺寸
     */
    private void adjustSize() {
        String text = textArea.getText();
        if (text == null || text.isEmpty()) {
            textArea.setPrefWidth(SizeConstants.MIN_TEXTAREA_WIDTH);
            textArea.setPrefHeight(SizeConstants.MIN_TEXTAREA_HEIGHT);
            setPrefWidth(SizeConstants.MIN_NODE_WIDTH);
            setPrefHeight(SizeConstants.MIN_NODE_HEIGHT);
            return;
        }

        measureText.setText(text);
        measureText.setWrappingWidth(0);
        // textArea 左右无内边距，宽度 = 文本宽度
        double textWidth = measureText.getLayoutBounds().getWidth();

        // MindNode 宽度 = border(2px) + padding(20px) + textArea 宽度
        double nodeWidth = textWidth + 22;
        // MIN_NODE_WIDTH <= 宽度 <= MAX_NODE_WIDTH
        nodeWidth = Math.max(SizeConstants.MIN_NODE_WIDTH,
                Math.min(nodeWidth, SizeConstants.MAX_NODE_WIDTH));

        // 设置换行
        measureText.setWrappingWidth(SizeConstants.MAX_NODE_WIDTH - 22);
        double contentHeight = measureText.getLayoutBounds().getHeight();

        double totalPadding = (contentHeight / 25.4) * 2.6;
        double textHeight = contentHeight + totalPadding;
        // MindNode 高度 = border(2px) + padding(20px) + textArea 高度
        double nodeHeight = textHeight + 22;

        // y轴 - 高度变动的一半，让中心保持不变
//        double beforeHeight = getPrefHeight();
//        double delta = nodeHeight - beforeHeight;
//        model.setY(model.getY() - delta / 2.0);

        textArea.setPrefWidth(textWidth);
        textArea.setPrefHeight(textHeight);
        setPrefWidth(nodeWidth);
        setPrefHeight(nodeHeight);

        if (model.getPos() == PosConstants.RIGHT) {
            adjustChildrenXR(model);
            controller.adjustChildrenYR();
            controller.refreshLinesR();
        } else {
            adjustChildrenXL(model);
            controller.adjustChildrenYL();
            controller.refreshLinesL();
        }
    }

    /**
     * 调整子节点x轴
     */
    private void adjustChildrenXR(NodeModel nodeModel) {
        List<NodeModel> children = nodeModel.getRightChildren();
        if (children.isEmpty()) {
            return;
        }

        double parentX = nodeModel.getX();
        double parentWidth = nodeModel.getMindNode().getPrefWidth();
        double childX = parentX + parentWidth + SizeConstants.NODE_GAP_X;

        for (NodeModel child : children) {
            child.setX(childX);
            adjustChildrenXR(child);
        }
    }

    private void adjustChildrenXL(NodeModel nodeModel) {
        List<NodeModel> children = nodeModel.getLeftChildren();
        if (children.isEmpty()) {
            return;
        }

        double parentX = nodeModel.getX();
        double parentWidth = nodeModel.getMindNode().getPrefWidth();
        double childX = parentX + parentWidth + SizeConstants.NODE_GAP_X;

        for (NodeModel child : children) {
            child.setX(childX);
            adjustChildrenXL(child);
        }
    }
}