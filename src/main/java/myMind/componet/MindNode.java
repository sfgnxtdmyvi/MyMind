package myMind.componet;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import lombok.Getter;
import myMind.constants.PosConstants;
import myMind.constants.SizeConstants;
import myMind.controller.NodeController;
import org.fxmisc.richtext.InlineCssTextArea;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

//@Data 会自动生成 hashCode() 方法
//循环引用时，会无限递归调用双方的 hashCode() 方法
@Getter
public class MindNode extends VBox {
    private final NodeModel model;
    private final InlineCssTextArea textArea;
    private final NodeController controller;
    //用于测量文本尺寸
    private Text measureText;
    private ImageView image; // 新增图片视图

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

        image = new ImageView();
        //当改变宽度或高度时，另一个维度会自动按比例缩放
        image.setPreserveRatio(true);
        image.setSmooth(true);
        image.setVisible(false);
        //true：组件会参与布局计算
        //false：组件脱离布局管理
        image.setManaged(false);

        textArea = new InlineCssTextArea();
        textArea.replaceText(0, 0, model.getText());
        textArea.setWrapText(true);
        textArea.getStyleClass().add("nodeContent");
        // 让绘制连线时，能获取节点位置
        textArea.setPrefWidth(SizeConstants.MIN_TEXTAREA_WIDTH);
        textArea.setPrefHeight(SizeConstants.MIN_TEXTAREA_HEIGHT);

        setAlignment(Pos.CENTER);
        setPrefWidth(SizeConstants.MIN_NODE_WIDTH);
        setPrefHeight(SizeConstants.MIN_NODE_HEIGHT);
        getStyleClass().add("nodeBorder");
        setPadding(new Insets(10, 10, 10, 10));

        getChildren().addAll(image, textArea);
        VBox.setVgrow(textArea, Priority.ALWAYS);

        // 模型x、y变化时，改变位置
        textArea.textProperty().addListener((obs, oldText, newText) -> model.setText(newText));
        model.xProperty().addListener((obs, oldVal, newVal) -> setLayoutX(newVal.doubleValue()));
        model.yProperty().addListener((obs, oldVal, newVal) -> setLayoutY(newVal.doubleValue()));
        setLayoutX(model.getX());
        setLayoutY(model.getY());

        addListener();
    }

    private void addListener() {
        // 选中节点
        addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            controller.setSelectedNode(this);
        });

        // 文本变化动态调整
        textArea.textProperty().addListener((obs, oldText, newText) -> {
            Platform.runLater(this::adjustSizeR);
        });

        // 粘贴图片
        textArea.setOnKeyReleased(e -> {
            if (e.isControlDown() && e.getCode() == KeyCode.V) {
                // javafx 的剪贴板获取不了图片，只能用 awt 的
                Transferable transferable = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
                if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                    try {
                        BufferedImage bufferedImage = (BufferedImage) transferable.getTransferData(DataFlavor.imageFlavor);
                        Image clipboardImage = SwingFXUtils.toFXImage(bufferedImage, null);
                        image.setImage(clipboardImage);
//                        saveImage(bufferedImage);
                    } catch (UnsupportedFlavorException | IOException ex) {
                        ex.printStackTrace();
                    }
                    image.setFitWidth(100);
                    image.setFitHeight(100);
                    image.setVisible(true);
                    image.setManaged(true);
                    adjustSizeR();
                }

                e.consume();
            }
        });

        // --- 图片缩放逻辑 ---
//        image.setOnMousePressed(e -> {
//            if (image.isVisible()) {
//                startX = e.getSceneX();
//                startY = e.getSceneY();
//                startWidth = image.getFitWidth();
//                startHeight = image.getFitHeight();
//
//                // 判断是否点击在右下角区域用于缩放
//                if (e.getX() > image.getBoundsInLocal().getWidth() - RESIZE_THRESHOLD &&
//                        e.getY() > image.getBoundsInLocal().getHeight() - RESIZE_THRESHOLD) {
//                    isResizing = true;
//                    image.setCursor(Cursor.SE_RESIZE);
//                }
//            }
//        });
//
//        image.setOnMouseDragged(e -> {
//            if (isResizing) {
//                double deltaX = e.getSceneX() - startX;
//                double deltaY = e.getSceneY() - startY;
//
//                // 简单的等比例缩放或自由缩放，这里演示自由缩放
//                double newWidth = Math.max(20, startWidth + deltaX);
//                double newHeight = Math.max(20, startHeight + deltaY);
//
//                image.setFitWidth(newWidth);
//                image.setFitHeight(newHeight);
//
//                // 图片大小改变后，需要重新计算节点整体尺寸
//                adjustSizeR();
//            }
//        });
//
//        image.setOnMouseReleased(e -> {
//            isResizing = false;
//            image.setCursor(Cursor.DEFAULT);
//        });
//
//        // 鼠标悬停在右下角显示缩放光标
//        image.setOnMouseMoved(e -> {
//            if (e.getX() > image.getBoundsInLocal().getWidth() - RESIZE_THRESHOLD &&
//                    e.getY() > image.getBoundsInLocal().getHeight() - RESIZE_THRESHOLD) {
//                image.setCursor(Cursor.SE_RESIZE);
//            } else {
//                image.setCursor(Cursor.DEFAULT);
//            }
//        });
    }

    private static void saveImage(BufferedImage bufferedImage) {
        File output = new File("C:\\Users\\k8255\\Desktop", "clipboard_image.png");
        try {
            ImageIO.write(bufferedImage, "png", output);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * 根据内容动态调整尺寸
     */
    private void adjustSizeR() {
        String text = textArea.getText();
        if (!image.isVisible() && text.isEmpty()) {
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
        // MindNode 高度 = border(2px) + padding(20px) + image高度 + textArea 高度
        double imageHeight = image.getFitHeight();
        double nodeHeight = imageHeight + textHeight + 22;

        // y轴 - 高度变动的一半，让中心保持不变
        if (model.getPos() == PosConstants.MIDDLE) {
            double beforeHeight = getPrefHeight();
            double delta = nodeHeight - beforeHeight;
            model.setY(model.getY() - delta / 2.0);
        }

        textArea.setPrefWidth(textWidth);
        textArea.setPrefHeight(textHeight);
        setPrefWidth(nodeWidth);
        setPrefHeight(nodeHeight);

        adjustChildrenXR(model);
        controller.adjustChildrenYR();
        controller.refreshLinesR();
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
        double parentWidth = nodeModel.getSelfWidth();
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
        double parentWidth = nodeModel.getSelfWidth();
        double childX = parentX + parentWidth + SizeConstants.NODE_GAP_X;

        for (NodeModel child : children) {
            child.setX(childX);
            adjustChildrenXL(child);
        }
    }
}