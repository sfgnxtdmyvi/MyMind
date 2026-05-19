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
import myMind.controller.SubjectController;
import myMind.util.CopyNodeUtil;
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

//@Data 会自动生成 hashCode() 方法
//循环引用时，会无限递归调用双方的 hashCode() 方法
@Getter
public class MindNode extends StackPane {
    @Setter
    private NodeModel model;
    private final SubjectController subjectController;

    private final VBox contentBox;
    @Setter
    private String imageName;
    private final ImageView image;
    private final StackPane imageContainer;
    private final Button closeButton;

    private final StyleClassedTextArea textArea;
    private final Text measureText = MeasureTextUtil.getMeasureText();

    private Button addButtonR;
    private Button addButtonL;

    // 拖拽缩放相关变量
    private static final double RESIZE_THRESHOLD = 8.0;
    private static final double BUTTON_THRESHOLD = 15.0;
    private boolean isResizing = false;
    private double startX;
    private double startY;
    private double startWidth;
    private double ratio;

    public MindNode(NodeModel model, SubjectController subjectController, String text) {
        this.model = model;
        model.setMindNode(this);
        this.subjectController = subjectController;

        image = new ImageView();
        //当改变宽度或高度时，另一个维度会自动按比例缩放
        image.setPreserveRatio(true);
        image.setSmooth(true);

        closeButton = new Button("✖");
        closeButton.getStyleClass().add("close-button");
        closeButton.setVisible(false);
        closeButton.setTranslateX(8);
        closeButton.setTranslateY(-8);
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
        textArea.replaceText(text);
        textArea.setWrapText(true);
        textArea.getStyleClass().add("nodeTextArea");
        VBox.setVgrow(textArea, Priority.ALWAYS);

        contentBox = new VBox();
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setPadding(new Insets(10, 10, 10, 10));
        contentBox.getStyleClass().add("nodeBorder");
        contentBox.getChildren().addAll(imageContainer, textArea);

        getChildren().add(contentBox);
        byte pos = model.getPos();
        if (pos != PosConstants.LEFT) {
            addButtonR = new Button("✚");
            addButtonR.getStyleClass().add("action-button");
            addButtonR.setVisible(false);
            StackPane.setAlignment(addButtonR, Pos.CENTER_RIGHT);
            addButtonR.setTranslateX(8);
            getChildren().add(addButtonR);
        }

        if (pos != PosConstants.RIGHT) {
            addButtonL = new Button("✚");
            addButtonL.getStyleClass().add("action-button");
            addButtonL.setVisible(false);
            StackPane.setAlignment(addButtonL, Pos.CENTER_LEFT);
            addButtonL.setTranslateX(-8);
            getChildren().add(addButtonL);
        }

        setPrefWidth(SizeConstants.MIN_NODE_WIDTH);
        setPrefHeight(SizeConstants.MIN_NODE_HEIGHT);

        // 模型x、y变化时，改变位置
        model.xProperty()
                .addListener((obs, oldVal, newVal) -> setLayoutX(newVal.doubleValue()));
        model.yProperty()
                .addListener((obs, oldVal, newVal) -> setLayoutY(newVal.doubleValue()));
        setLayoutX(model.getX());
        setLayoutY(model.getY());

        addListener();
    }

    public void loadImage(String imagePath, double imageWidth, double imageHeight) {
        this.imageName = imagePath;
        ratio = imageWidth / imageHeight;

        imageContainer.setVisible(true);
        imageContainer.setManaged(true);
        File file = new File(imagePath);
        image.setImage(new Image(file.toURI().toString()));
        image.setFitWidth(imageWidth);
        image.setFitHeight(imageHeight);
    }

    public void importImage(String imagePath, double imageWidth, double imageHeight) {
        this.imageName = imagePath;
        ratio = imageWidth / imageHeight;

        imageContainer.setVisible(true);
        imageContainer.setManaged(true);
        File file = new File(imagePath);
        image.setImage(new Image(file.toURI().toString()));
        image.setFitWidth(imageWidth / 2.2);
        image.setFitHeight(imageHeight / 2.2);
    }

    private void addListener() {
        // 选中节点
        addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            subjectController.setSelectedModel(model);
        });

        contentBox.setOnMouseClicked(e -> {
            MindNode copyNode = CopyNodeUtil.get();
            if (copyNode != null) {
                subjectController.pasteSibling(copyNode, model.getPos());
            }
        });

        addAddBtnListener();

        addImageListener();

        // 文本变化动态调整
        textArea.textProperty().addListener((obs, oldText, newText) ->
                Platform.runLater(this::adjustSize));
    }

    private void addAddBtnListener() {
        // 鼠标移入左右中心点时，显示添加按钮
        byte pos = model.getPos();
        if (pos == PosConstants.RIGHT) {
            setOnMouseMoved(e -> {
                double midHeight = getBoundsInLocal().getHeight() / 2;
                double y = e.getY();
                if (e.getX() > getBoundsInLocal().getWidth() - BUTTON_THRESHOLD &&
                        y < midHeight + BUTTON_THRESHOLD && y > midHeight - BUTTON_THRESHOLD) {
                    addButtonR.setVisible(true);
                }
            });

            setOnMouseExited(e -> addButtonR.setVisible(false));

            addButtonR.setOnAction(e -> {
                MindNode mindNode = CopyNodeUtil.get();
                if (mindNode == null) {
                    subjectController.addChildR();
                } else {
                    subjectController.pasteChild(mindNode, PosConstants.RIGHT);
                }
            });
        }

        if (pos == PosConstants.LEFT) {
            setOnMouseMoved(e -> {
                double midHeight = getBoundsInLocal().getHeight() / 2;
                double y = e.getY();
                if (e.getX() < BUTTON_THRESHOLD &&
                        y < midHeight + BUTTON_THRESHOLD && y > midHeight - BUTTON_THRESHOLD) {
                    addButtonL.setVisible(true);
                }
            });
            setOnMouseExited(e -> addButtonL.setVisible(false));

            addButtonL.setOnAction(e -> {
                MindNode mindNode = CopyNodeUtil.get();
                if (mindNode == null) {
                    subjectController.addChildL();
                } else {
                    subjectController.pasteChild(mindNode, PosConstants.LEFT);
                }
            });
        }

        // 用 pos != PosConstants.LEFT 写法添加事件的话，根节点添加左按钮的事件时，会覆盖右按钮的事件
        if (pos == PosConstants.MIDDLE) {
            setOnMouseMoved(e -> {
                double midHeight = getBoundsInLocal().getHeight() / 2;
                double y = e.getY();

                if (e.getX() > getBoundsInLocal().getWidth() - BUTTON_THRESHOLD &&
                        y < midHeight + BUTTON_THRESHOLD && y > midHeight - BUTTON_THRESHOLD) {
                    addButtonR.setVisible(true);
                }
                if (e.getX() < BUTTON_THRESHOLD &&
                        y < midHeight + BUTTON_THRESHOLD && y > midHeight - BUTTON_THRESHOLD) {
                    addButtonL.setVisible(true);
                }
            });

            setOnMouseExited(e -> {
                addButtonR.setVisible(false);
                addButtonL.setVisible(false);
            });

            addButtonR.setOnAction(e -> {
                MindNode mindNode = CopyNodeUtil.get();
                if (mindNode == null) {
                    subjectController.addChildR();
                } else {
                    subjectController.pasteChild(mindNode, PosConstants.RIGHT);
                }
            });
            addButtonL.setOnAction(e -> {
                MindNode mindNode = CopyNodeUtil.get();
                if (mindNode == null) {
                    subjectController.addChildL();
                } else {
                    subjectController.pasteChild(mindNode, PosConstants.LEFT);
                }
            });
        }
    }

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
                double y = e.getY();
                if (y > imageHeight - RESIZE_THRESHOLD) {
                    imageContainer.setCursor(Cursor.SE_RESIZE);
                } else if (y < BUTTON_THRESHOLD) {
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
            FileHandler.deleteImage(imageName);
            imageName = null;
            adjustSize();
        });

        // 缩放
        imageContainer.setOnMousePressed(e -> {
            if (image.isVisible()) {
                startX = e.getSceneX();
                startY = e.getSceneY();
                startWidth = image.getFitWidth();

                if (e.getX() > image.getBoundsInLocal().getWidth() - RESIZE_THRESHOLD
                        && e.getY() > image.getBoundsInLocal().getHeight() - RESIZE_THRESHOLD) {
                    isResizing = true;
                    image.setCursor(Cursor.SE_RESIZE);
                }
            }
        });

        imageContainer.setOnMouseDragged(e -> {
            if (isResizing) {
                // 根据宽度的变化量，按宽高比计算高度
                double imageWidth = startWidth + e.getSceneX() - startX;
                double imageHeight = imageWidth / ratio;

                image.setFitWidth(imageWidth);
                image.setFitHeight(imageHeight);

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
            // MindNode 高度 = border(2px) + padding(20px) + image 高度 + textArea 高度
            nodeHeight = textHeight + 22;
            if (imageVisible) {
                nodeHeight += image.getFitHeight() + 2;
            }
        }

        // y 轴 - 高度变动的一半，让中心保持不变
        double beforeHeight = getPrefHeight();
        double delta = nodeHeight - beforeHeight;
        model.setY(model.getY() - delta / 2.0);

        double originalWidth = getPrefWidth();

        setPrefWidth(nodeWidth);
        setPrefHeight(nodeHeight);

        if (model.getPos() == PosConstants.LEFT) {
            model.setX(model.getX() - (nodeWidth - originalWidth));
            subjectController.adjustChildrenXL(model);
            subjectController.adjustChildrenYL();
            subjectController.refreshLinesL();
        } else {
            subjectController.adjustChildrenXR(model);
            subjectController.adjustChildrenYR();
            subjectController.refreshLinesR();
        }
    }

    public MindNode clone() {
        NodeModel copyModel = new NodeModel(
                0,
                0,
                model.getPos()
        );

        MindNode copyNode = new MindNode(copyModel, subjectController, textArea.getText());
        if (imageName != null) {
            copyNode.loadImage(imageName, image.getFitWidth(), image.getFitHeight());
        }
        copyStyles(copyNode);

        if (model.getPos() == PosConstants.RIGHT) {
            for (NodeModel childModel : model.getChildrenR()) {
                NodeModel clone = childModel.getMindNode().clone().getModel();
                copyModel.addChildR(clone);
                clone.setParent(copyModel);
            }
        } else {
            for (NodeModel childModel : model.getChildrenL()) {
                NodeModel clone = childModel.getMindNode().clone().getModel();
                copyModel.addChildL(clone);
                clone.setParent(copyModel);
            }
        }

        return copyNode;
    }

    public void copyStyles(MindNode copyNode) {
        int length = textArea.getLength();
        if (length == 0) {
            return;
        }

        StyleClassedTextArea copyTextArea = copyNode.getTextArea();
        int start = 0;
        Collection<String> lastStyles = textArea.getStyleOfChar(0);

        for (int i = 1; i < length; i++) {
            Collection<String> currentStyles = textArea.getStyleOfChar(i);

            // 样式变化时保存前一段
            if (!currentStyles.equals(lastStyles)) {
                if (!lastStyles.isEmpty()) {
                    copyTextArea.setStyle(start, i, lastStyles);
                }

                lastStyles = currentStyles;
                start = i;
            }
        }

        // 由于最后一段不会变化，额外处理
        if (!lastStyles.isEmpty()) {
            copyTextArea.setStyle(start, length, lastStyles);
        }
    }
}