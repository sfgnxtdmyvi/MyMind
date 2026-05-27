package myMind.componet;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.Data;
import lombok.EqualsAndHashCode;
import myMind.constants.MindNodeEvent;
import myMind.constants.PosConstants;
import myMind.constants.SizeConstants;
import myMind.controller.FileHandler;
import myMind.util.MeasureTextUtil;
import org.fxmisc.richtext.StyleClassedTextArea;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.function.Consumer;

@Data
// 从 nodesLayer 中 remove 时，要用到 equals，不能依赖可变的属性
@EqualsAndHashCode(of = "model")
public class MindNode extends StackPane {
    private NodeModel model;
    private Consumer<MindNodeEvent> onAction;

    private final VBox contentBox;
    private String imageName;
    private final ImageView image;
    private final StackPane imageContainer;
    private final Button closeButton;
    private final StyleClassedTextArea textArea;

    private Button addButtonR;
    private Button addButtonL;

    // 拖拽缩放相关变量
    private static final double RESIZE_THRESHOLD = 8.0;
    private static final double BUTTON_THRESHOLD = 15.0;
    private boolean isResizing = false;
    private double startX;
    private double startWidth;
    private double ratio;

    public MindNode(NodeModel model) {
        this.model = model;

        image = new ImageView();
        image.setSmooth(true);

        closeButton = new Button("✖");
        closeButton.getStyleClass().add("close-button");
        closeButton.setVisible(false);
        StackPane.setAlignment(closeButton, Pos.TOP_RIGHT);

        // StackPane 负责显示边框
        // 只有 Region 及其子类才能通过 CSS 设置边框和背景
        imageContainer = new StackPane(image, closeButton);
        // 在一个会拉伸子节点的布局容器中，如果子节点没有设置最大尺寸限制，它会填满可用空间
        imageContainer.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        imageContainer.setVisible(false);
        //true：组件会参与布局计算
        //false：组件脱离布局管理
        imageContainer.setManaged(false);

        textArea = new StyleClassedTextArea();
        textArea.setStyle("-fx-font-family: 'Microsoft YaHei'; -fx-font-size: 20px;");
        VBox.setVgrow(textArea, Priority.ALWAYS);

        contentBox = new VBox();
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setPadding(new Insets(10, 10, 10, 10));
        contentBox.getChildren().addAll(imageContainer, textArea);
        ObservableList<Node> children = getChildren();
        children.add(contentBox);

        byte pos = model.getPos();
        if (pos != PosConstants.LEFT) {
            addButtonR = new Button("✚");
            addButtonR.getStyleClass().add("action-button");
            addButtonR.setVisible(false);
            StackPane.setAlignment(addButtonR, Pos.CENTER_RIGHT);
            addButtonR.setTranslateX(8);
            children.add(addButtonR);
        }

        if (pos != PosConstants.RIGHT) {
            addButtonL = new Button("✚");
            addButtonL.getStyleClass().add("action-button");
            addButtonL.setVisible(false);
            StackPane.setAlignment(addButtonL, Pos.CENTER_LEFT);
            addButtonL.setTranslateX(-8);
            children.add(addButtonL);
        }

        getStyleClass().add("node");
        model.setNodeWidth(SizeConstants.MIN_NODE_WIDTH);
        model.setNodeHeight(SizeConstants.MIN_NODE_HEIGHT);

        // view 绑定 model
        layoutXProperty().bind(model.xProperty());
        layoutYProperty().bind(model.yProperty());
        prefWidthProperty().bind(model.nodeWidthProperty());
        prefHeightProperty().bind(model.nodeHeightProperty());

        addListener();
    }

    public void loadImage(String imageName, double imageWidth, double imageHeight) {
        this.imageName = imageName;
        ratio = imageWidth / imageHeight;

        imageContainer.setVisible(true);
        imageContainer.setManaged(true);
        image.setImage(new Image(new File(FileHandler.getDirImage() + imageName).toURI().toString()));
        image.setFitWidth(imageWidth);
        image.setFitHeight(imageHeight);
    }

    public void importImage(String imageName, double imageWidth, double imageHeight) {
        this.imageName = imageName;
        ratio = imageWidth / imageHeight;

        imageContainer.setVisible(true);
        imageContainer.setManaged(true);
        image.setImage(new Image(new File("C:\\Users\\k8255\\AppData\\Roaming\\MindLine\\Images\\" + imageName).toURI().toString()));
        image.setFitWidth(imageWidth / 2.2);
        image.setFitHeight(imageHeight / 2.2);
    }

    private void addListener() {
        // 选中节点
        addEventFilter(MouseEvent.MOUSE_PRESSED, e -> onAction.accept(MindNodeEvent.SELECT));

        // 粘贴到选中节点上方
        contentBox.setOnMouseClicked(e -> onAction.accept(MindNodeEvent.PASTE_SIBLING));

        addAddBtnListener();

        addImageListener();

        // 文本变化调整节点大小
        textArea.textProperty().addListener((obs, oldText, newText) ->
                Platform.runLater(this::adjust));
    }

    /**
     * 按钮监听
     */
    private void addAddBtnListener() {
        // 鼠标移入左右中心点时，显示添加按钮
        byte pos = model.getPos();
        if (pos == PosConstants.RIGHT) {
            setOnMouseMoved(e -> {
                double midHeight = getBoundsInLocal().getHeight() / 2;
                double y = e.getY();
                if (getBoundsInLocal().getWidth() - BUTTON_THRESHOLD < e.getX() &&
                        midHeight - BUTTON_THRESHOLD < y && y < midHeight + BUTTON_THRESHOLD) {
                    addButtonR.setVisible(true);
                }
            });
            setOnMouseExited(e -> addButtonR.setVisible(false));

            addButtonR.setOnAction(e -> onAction.accept(MindNodeEvent.ADD_BUTTON_R));
        }

        if (pos == PosConstants.LEFT) {
            setOnMouseMoved(e -> {
                double midHeight = getBoundsInLocal().getHeight() / 2;
                double y = e.getY();
                if (e.getX() < BUTTON_THRESHOLD &&
                        midHeight - BUTTON_THRESHOLD < y && y < midHeight + BUTTON_THRESHOLD) {
                    addButtonL.setVisible(true);
                }
            });
            setOnMouseExited(e -> addButtonL.setVisible(false));

            addButtonL.setOnAction(e -> onAction.accept(MindNodeEvent.ADD_BUTTON_L));
        }

        // 用 pos != PosConstants.LEFT 写法添加事件的话，根节点添加左按钮的事件时，会覆盖右按钮的事件
        if (pos == PosConstants.MIDDLE) {
            setOnMouseMoved(e -> {
                double midHeight = getBoundsInLocal().getHeight() / 2;
                double y = e.getY();

                if (getBoundsInLocal().getWidth() - BUTTON_THRESHOLD < e.getX() &&
                        midHeight - BUTTON_THRESHOLD < y && y < midHeight + BUTTON_THRESHOLD) {
                    addButtonR.setVisible(true);
                }
                if (e.getX() < BUTTON_THRESHOLD &&
                        midHeight - BUTTON_THRESHOLD < y && y < midHeight + BUTTON_THRESHOLD) {
                    addButtonL.setVisible(true);
                }
            });

            setOnMouseExited(e -> {
                addButtonR.setVisible(false);
                addButtonL.setVisible(false);
            });

            addButtonR.setOnAction(e -> onAction.accept(MindNodeEvent.ADD_BUTTON_R));
            addButtonL.setOnAction(e -> onAction.accept(MindNodeEvent.ADD_BUTTON_L));
        }
    }

    /**
     * 图片监听
     */
    private void addImageListener() {
        // 粘贴图片
        setOnKeyReleased(e -> {
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
                        image.setImage(clipboardImage);
                        double imageWidth = clipboardImage.getWidth();
                        double imageHeight = clipboardImage.getHeight();
                        image.setFitWidth(imageWidth / SizeConstants.SCALE);
                        image.setFitHeight(imageHeight / SizeConstants.SCALE);
                        ratio = imageWidth / imageHeight;
                        imageName = FileHandler.saveImage(bufferedImage, imageName);

                        imageContainer.setVisible(true);
                        imageContainer.setManaged(true);

                        adjust();
                    } catch (UnsupportedFlavorException | IOException ex) {
                        ex.printStackTrace();
                    }
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

            if (imageWidth - RESIZE_THRESHOLD < e.getX()) {
                double y = e.getY();
                if (imageHeight - RESIZE_THRESHOLD < y) {
                    imageContainer.setCursor(Cursor.SE_RESIZE);
                } else if (y < BUTTON_THRESHOLD) {
                    imageContainer.setCursor(Cursor.HAND);
                    closeButton.setVisible(true);
                }
            } else {
                imageContainer.setCursor(Cursor.DEFAULT);
            }
        });

        closeButton.setOnAction(e -> {
            image.setImage(null);
            imageContainer.setVisible(false);
            imageContainer.setManaged(false);
            FileHandler.deleteImage(imageName);
            imageName = null;
            adjust();
        });

        // 缩放
        imageContainer.setOnMousePressed(e -> {
            startX = e.getSceneX();
            startWidth = image.getFitWidth();

            if (image.getBoundsInLocal().getWidth() - RESIZE_THRESHOLD < e.getX()
                    && image.getBoundsInLocal().getHeight() - RESIZE_THRESHOLD < e.getY()) {
                isResizing = true;
                image.setCursor(Cursor.SE_RESIZE);
            }
        });

        imageContainer.setOnMouseDragged(e -> {
            if (isResizing) {
                double imageWidth = startWidth + e.getSceneX() - startX;
                image.setFitWidth(imageWidth);
                // 根据宽度的变化量，按宽高比计算高度
                image.setFitHeight(imageWidth / ratio);

                adjust();
            }
        });

        imageContainer.setOnMouseReleased(e -> {
            isResizing = false;
            image.setCursor(Cursor.DEFAULT);
        });

        // 点击有图片没有文字的节点显示 textArea
        imageContainer.setOnMouseClicked(e -> {
            if (!textArea.isVisible()) {
                textArea.setVisible(true);
                textArea.requestFocus();
                model.setNodeHeight(model.getNodeHeight() + SizeConstants.MIN_TEXTAREA_HEIGHT);
                model.setY(model.getY() - SizeConstants.HALF_MIN_TEXTAREA_HEIGHT);

                if (model.getPos() == PosConstants.RIGHT) {
                    onAction.accept(MindNodeEvent.ADJUST_YR);
                } else {
                    onAction.accept(MindNodeEvent.ADJUST_YL);
                }
            }
        });

        // 有图片没有文字的节点失去焦点时隐藏 textArea
        textArea.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                // 清除选区，恢复背景色
                textArea.deselect();

                if (imageContainer.isVisible() && textArea.getText().isEmpty()) {
                    textArea.setVisible(false);
                    model.setNodeHeight(model.getNodeHeight() - SizeConstants.MIN_TEXTAREA_HEIGHT);
                    model.setY(model.getY() + SizeConstants.HALF_MIN_TEXTAREA_HEIGHT);

                    if (model.getPos() == PosConstants.RIGHT) {
                        onAction.accept(MindNodeEvent.ADJUST_YR);
                    } else {
                        onAction.accept(MindNodeEvent.ADJUST_YL);
                    }
                }
            }
        });
    }

    /**
     * 调整尺寸和位置
     */
    public void adjust() {
        double oldWidth = getPrefWidth();
        adjustSize();

        // 调整位置
        if (model.getPos() == PosConstants.LEFT) {
            model.setX(model.getX() - (getPrefWidth() - oldWidth));
            onAction.accept(MindNodeEvent.ADJUST_R);
        } else {
            onAction.accept(MindNodeEvent.ADJUST_L);
        }
    }

    /**
     * 根据内容调整尺寸
     */
    public void adjustSize() {
        String text = textArea.getText();
        boolean imageVisible = imageContainer.isVisible();
        double nodeWidth;
        double nodeHeight;

        if (!imageVisible && text.isEmpty()) {
            nodeWidth = SizeConstants.MIN_NODE_WIDTH;
            nodeHeight = SizeConstants.MIN_NODE_HEIGHT;
        } else {
            // textArea 宽度 + border(2px) + padding(20px)
            nodeWidth = MeasureTextUtil.getTextWidth(text) + 22;
            // MIN_NODE_WIDTH <= 宽度 <= MAX_NODE_WIDTH
            nodeWidth = Math.max(SizeConstants.MIN_NODE_WIDTH,
                    Math.min(nodeWidth, SizeConstants.MAX_NODE_WIDTH));
            if (imageVisible) {
                // 文本宽度 < 图片宽度时，宽度 = 图片宽度 + border(4px) + padding(20px)
                nodeWidth = Math.max(nodeWidth, image.getFitWidth() + 24);
            }

            // textArea 行数 * 行高 + border(2px) + padding(20px) [+ image 高度 + border(2px)]
            nodeHeight = text.split("\n", -1).length * SizeConstants.LINE_HEIGHT + 22;
            if (imageVisible) {
                nodeHeight += image.getFitHeight() + 2;
            }
        }

        // y 轴 - 高度变动的一半，让中心保持不变
        model.setY(model.getY() - (nodeHeight - getPrefHeight()) / 2.0);

        model.setNodeWidth(nodeWidth * 1.01);
        model.setNodeHeight(nodeHeight);
    }

    public void copyStyles(MindNode cloneNode, MindNode originalNode) {
        StyleClassedTextArea originalTextArea = originalNode.getTextArea();
        int length = originalTextArea.getLength();
        if (length == 0) {
            return;
        }

        StyleClassedTextArea cloneTextArea = cloneNode.getTextArea();
        cloneTextArea.replaceText(originalTextArea.getText());
        int start = 0;
        Collection<String> lastStyles = originalTextArea.getStyleOfChar(0);

        for (int i = 1; i < length; i++) {
            Collection<String> currentStyles = originalTextArea.getStyleOfChar(i);

            // 样式变化时保存前一段
            if (!currentStyles.equals(lastStyles)) {
                if (!lastStyles.isEmpty()) {
                    cloneTextArea.setStyle(start, i, lastStyles);
                }

                lastStyles = currentStyles;
                start = i;
            }
        }

        // 由于最后一段不会变化，额外处理
        if (!lastStyles.isEmpty()) {
            cloneTextArea.setStyle(start, length, lastStyles);
        }
    }
}