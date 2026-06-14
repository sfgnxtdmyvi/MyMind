package myMind.componet;

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
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.Data;
import lombok.EqualsAndHashCode;
import myMind.constants.NodeConstants;
import myMind.constants.NodeEvent;
import myMind.constants.PosConstants;
import myMind.manager.ConfigManager;
import myMind.util.FileUtil;
import myMind.util.MeasureTextUtil;
import org.fxmisc.richtext.StyleClassedTextArea;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@Data
// 从 nodesLayer 中 remove 时，要用到 equals，不能依赖可变的属性
@EqualsAndHashCode(of = "nodeId")
public class MindNode extends StackPane {
    private final String nodeId = UUID.randomUUID().toString();

    //节点之间的关系
    private byte pos;
    private MindNode parentNode;
    private final List<MindNode> childrenR = new ArrayList<>();
    private final List<MindNode> childrenL = new ArrayList<>();

    private Consumer<NodeEvent> onAction;
    private Consumer<Double> setSubjectTranslateY;

    private VBox contentBox;
    private String imageName;
    private ImageView imageView;
    private StackPane imageContainer;
    private Button closeButton;
    private StyleClassedTextArea textArea;

    private Button addButtonR;
    private Button addButtonL;

    private boolean isResizing = false;
    private double startX;
    private double startWidth;
    private double ratio;

    public MindNode(byte pos) {
        this.pos = pos;
        StyleClassedTextArea textArea = new StyleClassedTextArea();
        textArea.getStyleClass().add("text-area");
        textArea.setMaxWidth(NodeConstants.MIN_TEXTAREA_WIDTH);
        textArea.setWrapText(true);

        // 不能用 this()，它必须在第一行
        // 用 this() 或 super() 时，不能使用任何实例字段
        buildNode(textArea);
    }

    public MindNode(byte pos, double x, double y) {
        this(pos);
        setLayoutX(x);
        setLayoutY(y);
    }

    public MindNode(byte pos, StyleClassedTextArea textArea) {
        this.pos = pos;
        buildNode(textArea);
    }

    public MindNode(byte pos, String imageName, double imageWidth, double imageHeight, StyleClassedTextArea textArea) {
        this.pos = pos;
        buildNode(textArea);
        buildImageContainer();
        imageView.setImage(new Image(new File(ConfigManager.DIR_IMAGE + imageName).toURI().toString()));
        imageView.setFitWidth(imageWidth);
        imageView.setFitHeight(imageHeight);
        ratio = imageWidth / imageHeight;
        this.imageName = imageName;
    }

    private void buildImageContainer() {
        imageView = new ImageView();
        imageView.setSmooth(true);

        closeButton = new Button("✖");
        closeButton.getStyleClass().add("close-button");
        closeButton.setVisible(false);
        StackPane.setAlignment(closeButton, Pos.TOP_RIGHT);

        // StackPane 负责显示边框
        // 只有 Region 及其子类才能通过 CSS 设置边框和背景
        imageContainer = new StackPane(imageView, closeButton);
        // 在一个会拉伸子节点的布局容器中，如果子节点没有设置最大尺寸限制，它会填满可用空间
        imageContainer.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        addImageListener();
        contentBox.getChildren().add(0, imageContainer);
    }

    private void buildNode(StyleClassedTextArea textArea) {
        this.textArea = textArea;

        contentBox = new VBox(textArea);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setPadding(new Insets(NodeConstants.PADDING));
        ObservableList<Node> children = getChildren();
        children.add(contentBox);

        // 中间节点会添加两个按钮
        if (pos != PosConstants.LEFT) {
            addButtonR(children);
        }
        if (pos != PosConstants.RIGHT) {
            addButtonL(children);
        }

        getStyleClass().add("node");
        setPrefWidth(NodeConstants.MIN_NODE_WIDTH);
        setPrefHeight(NodeConstants.MIN_NODE_HEIGHT);

        addListener();
    }

    public void addButtonR(ObservableList<Node> children) {
        addButtonR = new Button("✚");
        addButtonR.getStyleClass().addAll("add-button", "add-button-r");
        addButtonR.setVisible(false);
        StackPane.setAlignment(addButtonR, Pos.CENTER_RIGHT);
        children.add(addButtonR);
    }

    public void addButtonL(ObservableList<Node> children) {
        addButtonL = new Button("✚");
        addButtonL.getStyleClass().addAll("add-button", "add-button-l");
        addButtonL.setVisible(false);
        StackPane.setAlignment(addButtonL, Pos.CENTER_LEFT);
        children.add(addButtonL);
    }

    private void addListener() {
        // 使用 addEventFilter 的话，OnMouseClicked 的默认行为会让 MindNode 获得焦点，TextArea 就会失去焦点
        // 添加 e.consume() 的话，能阻止 OnMouseClicked 的默认行为，但是 addButton 就不会触发
        // 使用 setOnMouseClicked 的话，由于 addButton 是一个独立的 Button 组件，它会消费鼠标事件，事件不会冒泡到父节点 MindNode，
        // 需要在 addButton 的事件处理逻辑中添加 setselectedNode(model);
        contentBox.setOnMouseClicked(e -> {
            onAction.accept(NodeEvent.SELECT);
            onAction.accept(NodeEvent.PASTE_SIBLING);
        });

        addButtonListen();

        // 粘贴图片
        setOnKeyReleased(e -> {
            if (e.isControlDown() && e.getCode() == KeyCode.V) {
                // javafx 的剪贴板获取不了图片，只能用 awt 的
                Transferable transferable = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
                if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                    try {
                        BufferedImage bufferedImage = (BufferedImage) transferable.getTransferData(DataFlavor.imageFlavor);
                        Image clipboardImage = SwingFXUtils.toFXImage(bufferedImage, null);

                        if (imageContainer == null) {
                            buildImageContainer();
                        }
                        imageView.setImage(clipboardImage);
                        double imageWidth = clipboardImage.getWidth();
                        double imageHeight = clipboardImage.getHeight();
                        //如果开启了 150% 缩放
                        //截图时，系统记录的是逻辑像素，比如 100x100，按 150% 缩放渲染出来是 150x150
                        //但 awt 剪贴板拿到的是物理像素，就是 150x150，再按 150% 缩放渲染出来是 225x225
                        imageView.setFitWidth(imageWidth / NodeConstants.SCALE);
                        imageView.setFitHeight(imageHeight / NodeConstants.SCALE);
                        ratio = imageWidth / imageHeight;
                        imageName = FileUtil.saveImage(bufferedImage, imageName);

                        adjust();
                    } catch (UnsupportedFlavorException | IOException ex) {
                        ex.printStackTrace();
                    }
                }

                e.consume();
            }
        });

        // 文本变化调整节点大小
        textArea.textProperty()
                .addListener((obs, oldText, newText) -> {
                    adjust();
                });

        textArea.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                // 清除选区，恢复背景色
                textArea.deselect();
                adjustSize();
            }
        });
    }

    /**
     * 按钮监听
     */
    private void addButtonListen() {
        // 鼠标移入左右中心点时，显示添加按钮
        if (pos == PosConstants.RIGHT) {
            addButtonListenR();
        }

        if (pos == PosConstants.LEFT) {
            addButtonListenL();
        }

        // 用 pos != PosConstants.LEFT 写法添加事件的话，根节点添加左按钮的事件时，会覆盖右按钮的事件
        if (pos == PosConstants.MIDDLE) {
            setOnMouseMoved(e -> {
                double midHeight = getBoundsInLocal().getHeight() / 2;
                double y = e.getY();

                if (getBoundsInLocal().getWidth() - NodeConstants.BUTTON_THRESHOLD < e.getX() &&
                        midHeight - NodeConstants.BUTTON_THRESHOLD < y && y < midHeight + NodeConstants.BUTTON_THRESHOLD) {
                    addButtonR.setVisible(true);
                }
                if (e.getX() < NodeConstants.BUTTON_THRESHOLD &&
                        midHeight - NodeConstants.BUTTON_THRESHOLD < y && y < midHeight + NodeConstants.BUTTON_THRESHOLD) {
                    addButtonL.setVisible(true);
                }
            });

            setOnMouseExited(e -> {
                addButtonR.setVisible(false);
                addButtonL.setVisible(false);
            });

            addButtonR.setOnAction(e -> onAction.accept(NodeEvent.ADD_BUTTON_R));
            addButtonL.setOnAction(e -> onAction.accept(NodeEvent.ADD_BUTTON_L));
        }
    }

    public void addButtonListenR() {
        setOnMouseMoved(e -> {
            double midHeight = getBoundsInLocal().getHeight() / 2;
            double y = e.getY();
            if (getBoundsInLocal().getWidth() - NodeConstants.BUTTON_THRESHOLD < e.getX() &&
                    midHeight - NodeConstants.BUTTON_THRESHOLD < y && y < midHeight + NodeConstants.BUTTON_THRESHOLD) {
                addButtonR.setVisible(true);
            }
        });
        setOnMouseExited(e -> addButtonR.setVisible(false));

        addButtonR.setOnAction(e -> onAction.accept(NodeEvent.ADD_BUTTON_R));
    }

    public void addButtonListenL() {
        setOnMouseMoved(e -> {
            double midHeight = getBoundsInLocal().getHeight() / 2;
            double y = e.getY();
            if (e.getX() < NodeConstants.BUTTON_THRESHOLD &&
                    midHeight - NodeConstants.BUTTON_THRESHOLD < y && y < midHeight + NodeConstants.BUTTON_THRESHOLD) {
                addButtonL.setVisible(true);
            }
        });
        setOnMouseExited(e -> addButtonL.setVisible(false));

        addButtonL.setOnAction(e -> onAction.accept(NodeEvent.ADD_BUTTON_L));
    }

    /**
     * 图片监听
     */
    private void addImageListener() {
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
            double imageWidth = imageView.getBoundsInLocal().getWidth();
            double imageHeight = imageView.getBoundsInLocal().getHeight();

            // 不能合并两个 x 轴的判断，当在右下角出现了缩放图标后，往上移动，x 轴不变时，会进入 x 轴的分支，导致缩放图标不恢复
            if (imageWidth - NodeConstants.RESIZE_THRESHOLD < e.getX() && imageHeight - NodeConstants.RESIZE_THRESHOLD < e.getY()) {
                imageContainer.setCursor(Cursor.SE_RESIZE);
            } else if (imageWidth - NodeConstants.RESIZE_THRESHOLD < e.getX() && e.getY() < NodeConstants.BUTTON_THRESHOLD) {
                imageContainer.setCursor(Cursor.HAND);
                closeButton.setVisible(true);
            } else {
                imageContainer.setCursor(Cursor.DEFAULT);
            }
        });

        closeButton.setOnAction(e -> {
            imageView.setImage(null);
            imageContainer.setVisible(false);
            imageContainer.setManaged(false);
            FileUtil.deleteImage(imageName);
            imageName = null;
            adjust();
        });

        // 缩放
        imageContainer.setOnMousePressed(e -> {
            startX = e.getSceneX();
            startWidth = imageView.getFitWidth();

            if (imageView.getBoundsInLocal().getWidth() - NodeConstants.RESIZE_THRESHOLD < e.getX()
                    && imageView.getBoundsInLocal().getHeight() - NodeConstants.RESIZE_THRESHOLD < e.getY()) {
                isResizing = true;
                imageView.setCursor(Cursor.SE_RESIZE);
            }
        });

        imageContainer.setOnMouseDragged(e -> {
            if (isResizing) {
                double imageWidth = startWidth + e.getSceneX() - startX;
                imageView.setFitWidth(imageWidth);
                // 根据宽度的变化量，按宽高比计算高度
                imageView.setFitHeight(imageWidth / ratio);

                adjust();
            }
        });

        imageContainer.setOnMouseReleased(e -> {
            isResizing = false;
            imageView.setCursor(Cursor.DEFAULT);
        });

        // 点击有图片没有文字的节点显示 textArea
        imageContainer.setOnMouseClicked(e -> {
            if (!textArea.isVisible()) {
                // setVisible(false) 后，maxWidth 就变成0了
                textArea.setMaxWidth(NodeConstants.MIN_TEXTAREA_WIDTH);
                textArea.setVisible(true);
                textArea.requestFocus();
                setPrefHeight(getPrefHeight() + NodeConstants.MIN_TEXTAREA_HEIGHT);
                setLayoutY(getLayoutY() - NodeConstants.HALF_MIN_TEXTAREA_HEIGHT);

                if (pos == PosConstants.RIGHT) {
                    onAction.accept(NodeEvent.ADJUST_YR);
                } else {
                    onAction.accept(NodeEvent.ADJUST_YL);
                }
            }
        });
    }

    /**
     * 调整尺寸和位置
     */
    public void adjust() {
        double oldWidth = getPrefWidth();
        double oldHeight = getPrefHeight();
        adjustSize();
        double newHeight = getPrefHeight();
        setSubjectTranslateY.accept(-(newHeight - oldHeight) * 0.5);

        // 调整位置
        if (pos == PosConstants.LEFT) {
            setLayoutX(getLayoutX() - (getPrefWidth() - oldWidth));
            onAction.accept(NodeEvent.ADJUST_L);
        } else {
            onAction.accept(NodeEvent.ADJUST_R);
        }
    }

    /**
     * 根据内容调整尺寸
     */
    public void adjustSize() {
        String text = textArea.getText();
        boolean imageVisible = imageName != null;
        double nodeWidth;
        double nodeHeight;
        double textWidth;
        double textHeight;

        boolean textEmpty = text.isEmpty();
        if (!imageVisible && textEmpty) {
            textWidth = NodeConstants.MIN_TEXTAREA_WIDTH;
            textHeight = NodeConstants.MIN_TEXTAREA_HEIGHT;
            nodeWidth = NodeConstants.MIN_NODE_WIDTH;
            nodeHeight = NodeConstants.MIN_NODE_HEIGHT;
        } else {
            // todo 宽度bug
            // todo 增加微小宽度时，不改变宽度
            // textArea 宽度 + border + padding
            textWidth = Math.min(MeasureTextUtil.getTextWidth(text), NodeConstants.MAX_TEXTAREA_WIDTH);
            nodeWidth = Math.max(NodeConstants.MIN_NODE_WIDTH, (textWidth + NodeConstants.BORDER_AND_PADDING) * 1.01);
            if (imageVisible) {
                // 文本宽度 < 图片宽度时，宽度 = 图片宽度 + border + padding
                nodeWidth = Math.max(nodeWidth, imageView.getFitWidth() + NodeConstants.BORDER_AND_PADDING + 2.6);
            }

            // textArea 高度 + border + padding [+ image 高度 + border]
            textHeight = MeasureTextUtil.getTextHeight() * 1.023;
            nodeHeight = textHeight + NodeConstants.BORDER_AND_PADDING;
            if (imageVisible) {
                nodeHeight += imageView.getFitHeight() + 2.6;
                if (textEmpty) {
                    textArea.setVisible(false);
                    nodeHeight -= textHeight;
                }
            }
        }
        // y 轴 - 高度变动的一半，让中心保持不变
        setLayoutY(getLayoutY() - (nodeHeight - getPrefHeight()) / 2.0);
        textArea.setMaxWidth(textWidth);
        textArea.setPrefHeight(textHeight);
        setPrefWidth(nodeWidth);
        setPrefHeight(nodeHeight);
    }

    //—————————————————————————————————————————增删—————————————————————————————————————————
    public void addChildR(MindNode child) {
        childrenR.add(child);
        child.setParentNode(this);
    }

    public void addChildL(MindNode child) {
        childrenL.add(child);
        child.setParentNode(this);
    }

    public void addChildRAt(int index, MindNode child) {
        childrenR.add(index, child);
        child.setParentNode(this);
    }

    public void addChildLAt(int index, MindNode child) {
        childrenL.add(index, child);
        child.setParentNode(this);
    }

    public void removeChildR(MindNode child) {
        childrenR.remove(child);
        child.setParentNode(null);
    }

    public void removeChildL(MindNode child) {
        childrenL.remove(child);
        child.setParentNode(null);
    }

    //———————————————————————————————————————————宽高计算———————————————————————————————————————————

    /**
     * 子节点的总高度
     * 所有子节点的高度 + 间隔
     */
    public double getChildrenHeightR() {
        double totalHeight = 0;
        for (MindNode child : childrenR) {
            totalHeight += child.getHeightR();
        }
        totalHeight += NodeConstants.GAP_Y * (childrenR.size() - 1);
        return totalHeight;
    }

    public double getChildrenHeightL() {
        double totalHeight = 0;
        for (MindNode child : childrenL) {
            totalHeight += child.getHeightL();
        }
        totalHeight += NodeConstants.GAP_Y * (childrenL.size() - 1);
        return totalHeight;
    }

    /**
     * 节点的高度
     * Math.max（当前节点的高度，子节点的总高度）
     */
    public double getHeightR() {
        if (childrenR.isEmpty()) {
            return getPrefHeight();
        }
        return Math.max(getPrefHeight(), getChildrenHeightR());
    }

    public double getHeightL() {
        if (childrenL.isEmpty()) {
            return getPrefHeight();
        }
        return Math.max(getPrefHeight(), getChildrenHeightL());
    }

    //———————————————————————————————————————————位置计算———————————————————————————————————————————
    public double getStartYR() {
        MindNode fistNode = childrenR.get(0);
        if (!fistNode.childrenR.isEmpty()) {
            // 当前节点可能比子节节点的总高度更高
            return Math.min(fistNode.getLayoutY(), fistNode.getStartYR());
        } else {
            return fistNode.getLayoutY();
        }
    }

    public double getStartYL() {
        MindNode fistNode = childrenL.get(0);
        if (!fistNode.childrenL.isEmpty()) {
            return Math.min(fistNode.getLayoutY(), fistNode.getStartYL());
        } else {
            return fistNode.getLayoutY();
        }
    }

    public double getEndYR() {
        MindNode lastNode = childrenR.get(childrenR.size() - 1);
        double selfEndY = lastNode.getLayoutY() + lastNode.getPrefHeight();
        if (!lastNode.childrenR.isEmpty()) {
            return Math.max(selfEndY, lastNode.getEndYR());
        } else {
            return selfEndY;
        }
    }

    public double getEndYL() {
        MindNode lastNode = childrenL.get(childrenL.size() - 1);
        double selfEndY = lastNode.getLayoutY() + lastNode.getPrefHeight();
        if (!lastNode.childrenL.isEmpty()) {
            return Math.max(selfEndY, lastNode.getEndYL());
        } else {
            return selfEndY;
        }
    }

    public boolean isEmpty() {
        return childrenR.isEmpty() && childrenL.isEmpty() &&
                textArea.getText().isEmpty() && imageName == null;
    }

}