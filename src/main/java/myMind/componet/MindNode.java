package myMind.componet;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import lombok.Getter;
import lombok.Setter;
import myMind.constants.PosConstants;
import myMind.constants.SizeConstants;
import myMind.controller.FileHandler;
import myMind.controller.NodeController;
import myMind.util.MeasureTextUtil;
import org.fxmisc.richtext.StyleClassedTextArea;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

//@Data 会自动生成 hashCode() 方法
//循环引用时，会无限递归调用双方的 hashCode() 方法
@Getter
public class MindNode extends VBox {
    @Setter
    private NodeModel model;
    private StyleClassedTextArea textArea;
    private String imagePath;
    private final NodeController controller;
    private Text measureText = MeasureTextUtil.getMeasureText();
    private final StackPane imageContainer;
    private ImageView image;
    private Button closeButton;

    // 拖拽缩放相关变量
    private static final double RESIZE_THRESHOLD = 8.0;
    private boolean isResizing = false;
    private double startX;
    private double startY;
    private double startWidth;
    private double startHeight;
    private double minImageWidth;
    private double minImageHeight;
    private double ratio;

    public MindNode(NodeModel model, NodeController controller) {
        this.model = model;
        model.setMindNode(this);
        this.controller = controller;

        image = new ImageView();
        //当改变宽度或高度时，另一个维度会自动按比例缩放
        image.setPreserveRatio(true);
        image.setSmooth(true);

        closeButton = new Button("×");
        closeButton.getStyleClass().add("close-button");
        closeButton.setVisible(false);
        StackPane.setAlignment(closeButton, Pos.TOP_RIGHT);

        // StackPane 负责显示边框
        // 只有 Region 及其子类才能通过 CSS 设置边框和背景
        imageContainer = new StackPane(image, closeButton);
        // 在一个会拉伸子节点的布局容器中，如果子节点没有设置最大尺寸限制，它会填满可用空间
        imageContainer.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        if (imagePath == null) {
            imageContainer.setVisible(false);
            //true：组件会参与布局计算
            //false：组件脱离布局管理
            imageContainer.setManaged(false);
        } else {
            File file = new File(imagePath);
            image.setImage(new Image(file.toURI().toString()));
            // todo 图片缩放
        }

        textArea = new StyleClassedTextArea();
        textArea.replaceText(model.getText());
        textArea.setWrapText(true);
        textArea.getStyleClass().add("nodeTextArea");
        // 让绘制连线时，能获取节点位置
        textArea.setPrefWidth(SizeConstants.MIN_TEXTAREA_WIDTH);
        textArea.setPrefHeight(SizeConstants.MIN_TEXTAREA_HEIGHT);

        setAlignment(Pos.CENTER);
        setPrefWidth(SizeConstants.MIN_NODE_WIDTH);
        setPrefHeight(SizeConstants.MIN_NODE_HEIGHT);
        getStyleClass().add("nodeBorder");
        setPadding(new Insets(10, 10, 10, 10));

        getChildren().addAll(imageContainer, textArea);
        VBox.setVgrow(textArea, Priority.ALWAYS);

        // 模型x、y变化时，改变位置
        textArea.textProperty()
                .addListener((obs, oldText, newText) -> model.setText(newText));
        model.xProperty()
                .addListener((obs, oldVal, newVal) -> setLayoutX(newVal.doubleValue()));
        model.yProperty()
                .addListener((obs, oldVal, newVal) -> setLayoutY(newVal.doubleValue()));
        setLayoutX(model.getX());
        setLayoutY(model.getY());

        adjustSize();
        addListener();
    }

    private void addListener() {
        // 选中节点
        addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            controller.setSelectedNode(this);
        });

        // 文本变化动态调整
        textArea.textProperty().addListener((obs, oldText, newText) -> {
            Platform.runLater(this::adjustSize);
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

                        //如果开启了 150% 缩放
                        //截图时，系统记录的是逻辑像素，比如 100x100，按 150% 缩放渲染出来是 150x150
                        //但 awt 剪贴板拿到的是物理像素，就是 150x150，再按 150% 缩放渲染出来是 225x225
                        double scale = this.getScene().getWindow().getOutputScaleX();
                        image.setImage(clipboardImage);
                        double width = clipboardImage.getWidth() / scale;
                        double height = clipboardImage.getHeight() / scale;
                        minImageWidth = width / 2;
                        minImageHeight = height / 2;
                        image.setFitWidth(width);
                        image.setFitHeight(height);
                        ratio = minImageWidth / minImageHeight;
                        imagePath = FileHandler.saveImage(bufferedImage, imagePath);

                        imageContainer.setVisible(true);
                        imageContainer.setManaged(true);

                    } catch (UnsupportedFlavorException | IOException ex) {
                        ex.printStackTrace();
                    }
                    image.setVisible(true);
                    image.setManaged(true);
                    adjustSize();
                }

                e.consume();
            }
        });

        // 鼠标移入时，显示边框
        imageContainer.setOnMouseEntered(e -> imageContainer.getStyleClass().add("nodeImage"));

        imageContainer.setOnMouseExited(e -> {
            if (!isResizing) {
                imageContainer.getStyleClass().remove("nodeImage");
                closeButton.setVisible(false);
            }
        });

        // 鼠标悬浮在右下角显示缩放图标，在右上角显示关闭图标
        imageContainer.setOnMouseMoved(e -> {
            double imageWidth = image.getBoundsInLocal().getWidth();
            double imageHeight = image.getBoundsInLocal().getHeight();

            if (e.getX() > imageWidth - RESIZE_THRESHOLD) {
                if (e.getY() > imageHeight - RESIZE_THRESHOLD) {
                    imageContainer.setCursor(Cursor.SE_RESIZE);
                } else if (e.getY() < RESIZE_THRESHOLD) {
                    imageContainer.setCursor(Cursor.HAND);
                    closeButton.setVisible(true);
                }
            } else {
                imageContainer.setCursor(Cursor.DEFAULT);
                closeButton.setVisible(false);
            }
        });

        closeButton.setOnAction(e -> {
            image.setImage(null);
            imageContainer.setVisible(false);
            imageContainer.setManaged(false);
            FileHandler.deleteImage(imagePath);
            imagePath = null;
            adjustSize();
        });

        imageContainer.setOnMousePressed(e -> {
            if (image.isVisible()) {
                startX = e.getSceneX();
                startY = e.getSceneY();
                startWidth = image.getFitWidth();
                startHeight = image.getFitHeight();

                if (e.getX() > image.getBoundsInLocal().getWidth() - RESIZE_THRESHOLD
                        && e.getY() > image.getBoundsInLocal().getHeight() - RESIZE_THRESHOLD) {
                    isResizing = true;
                    image.setCursor(Cursor.SE_RESIZE);
                }
            }
        });

        imageContainer.setOnMouseDragged(e -> {
            if (isResizing) {
                double deltaX = e.getSceneX() - startX;

                // 根据宽度的变化量，按宽高比计算高度
                double newWidth = Math.max(minImageWidth, startWidth + deltaX);
                double newHeight = Math.max(minImageHeight, startHeight + deltaX / ratio);

                image.setFitWidth(newWidth);
                image.setFitHeight(newHeight);

                adjustSize();
            }
        });

        imageContainer.setOnMouseReleased(e -> {
            isResizing = false;
            image.setCursor(Cursor.DEFAULT);
        });
    }

    /**
     * 根据内容动态调整尺寸
     */
    public void adjustSize() {
        String text = textArea.getText();
        boolean imageVisible = imageContainer.isVisible();
        double textWidth;
        double textHeight;
        double nodeWidth;
        double nodeHeight;

        if (!imageVisible && text.isEmpty()) {
            textWidth = SizeConstants.MIN_TEXTAREA_WIDTH;
            textHeight = SizeConstants.MIN_TEXTAREA_HEIGHT;
            nodeWidth = SizeConstants.MIN_NODE_WIDTH;
            nodeHeight = SizeConstants.MIN_NODE_HEIGHT;
        } else {
            measureText.setText(text);
            measureText.setWrappingWidth(0);
            // textArea 左右无内边距，宽度 = 文本宽度
            textWidth = measureText.getLayoutBounds().getWidth();

            // MindNode 宽度 = border(2px) + padding(20px) + textArea 宽度
            nodeWidth = textWidth + 22;
            // MIN_NODE_WIDTH <= 宽度 <= MAX_NODE_WIDTH
            nodeWidth = Math.max(SizeConstants.MIN_NODE_WIDTH,
                    Math.min(nodeWidth, SizeConstants.MAX_NODE_WIDTH));
            if (imageVisible) {
                nodeWidth = Math.max(nodeWidth + 2, image.getFitWidth() + 24);
            }

            // 设置换行
            measureText.setWrappingWidth(SizeConstants.MAX_NODE_WIDTH - 22);
            double contentHeight = measureText.getLayoutBounds().getHeight();

            double totalPadding = (contentHeight / 25.4) * 2.6;
            textHeight = contentHeight + totalPadding;
            // MindNode 高度 = border(2px) + padding(20px) + image高度 + textArea 高度
            nodeHeight = textHeight + 22;
            if (imageVisible) {
                nodeHeight += image.getFitHeight() + 2;
            }
        }

        // y 轴 - 高度变动的一半，让中心保持不变
        if (model.getPos() == PosConstants.MIDDLE) {
            double beforeHeight = getPrefHeight();
            double delta = nodeHeight - beforeHeight;
            model.setY(model.getY() - delta / 2.0);
        }

        textArea.setPrefWidth(textWidth);
        textArea.setPrefHeight(textHeight);
        setPrefWidth(nodeWidth);
        setPrefHeight(nodeHeight);

        if (model.getPos() == PosConstants.LEFT) {
            double originalWidth = getPrefWidth();
            model.setX(model.getX() - (nodeWidth - originalWidth));
            controller.adjustChildrenXL(model);
            controller.adjustChildrenYL();
            controller.refreshLinesL();
        } else {
            controller.adjustChildrenXR(model);
            controller.adjustChildrenYR();
            controller.refreshLinesR();
        }
    }

    public MindNode clone() {
        NodeModel originalModel = this.getModel();
        NodeModel newModel = new NodeModel(
                controller.nextId(),
                originalModel.getText(),
                0,
                0,
                originalModel.getPos()
        );

        MindNode mindNode = new MindNode(newModel, controller);
        // todo 复制子节点

        return mindNode;
    }

}