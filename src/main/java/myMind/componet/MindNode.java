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
import myMind.constants.FileConstants;
import myMind.constants.MindNodeEvent;
import myMind.constants.PosConstants;
import myMind.constants.SizeConstants;
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
    private MindNode nodeParent;
    private final List<MindNode> childrenR = new ArrayList<>();
    private final List<MindNode> childrenL = new ArrayList<>();

    private Consumer<MindNodeEvent> onAction;

    private VBox contentBox;
    private String imageName;
    private ImageView image;
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
        buildImageContainer();
        StyleClassedTextArea textArea = new StyleClassedTextArea();
        textArea.getStyleClass().add("text-area");
        textArea.setMaxWidth(SizeConstants.MIN_TEXTAREA_WIDTH);
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
        buildImageContainer();
        buildNode(textArea);
    }

    public MindNode(byte pos, String imageName, double imageWidth, double imageHeight, StyleClassedTextArea textArea) {
        this.pos = pos;
        image = new ImageView(new Image(new File(FileConstants.DIR_IMAGE + imageName).toURI().toString()));
        image.setSmooth(true);
        image.setFitWidth(imageWidth);
        image.setFitHeight(imageHeight);
        ratio = imageWidth / imageHeight;
        this.imageName = imageName;
        buildCloseButton();

        imageContainer = new StackPane(image, closeButton);
        imageContainer.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        imageContainer.setVisible(true);
        imageContainer.setManaged(true);

        buildNode(textArea);
    }

    private void buildNode(StyleClassedTextArea textArea) {
        this.textArea = textArea;

        contentBox = new VBox();
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setPadding(new Insets(10, 10, 10, 10));
        contentBox.getChildren().addAll(imageContainer, textArea);
        ObservableList<Node> children = getChildren();
        children.add(contentBox);

        if (pos != PosConstants.LEFT) {
            addButtonR(children);
        }
        if (pos != PosConstants.RIGHT) {
            addButtonL(children);
        }

        getStyleClass().add("node");
        setPrefWidth(SizeConstants.MIN_NODE_WIDTH);
        setPrefHeight(SizeConstants.MIN_NODE_HEIGHT);

        addListener();
    }

    private void buildImageContainer() {
        image = new ImageView();
        image.setSmooth(true);
        buildCloseButton();

        // StackPane 负责显示边框
        // 只有 Region 及其子类才能通过 CSS 设置边框和背景
        imageContainer = new StackPane(image, closeButton);
        // 在一个会拉伸子节点的布局容器中，如果子节点没有设置最大尺寸限制，它会填满可用空间
        imageContainer.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        imageContainer.setVisible(false);
        //true：组件会参与布局计算
        //false：组件脱离布局管理
        imageContainer.setManaged(false);
    }

    private void buildCloseButton() {
        closeButton = new Button("✖");
        closeButton.getStyleClass().add("close-button");
        closeButton.setVisible(false);
        StackPane.setAlignment(closeButton, Pos.TOP_RIGHT);
    }

    public void addButtonL(ObservableList<Node> children) {
        addButtonL = new Button("✚");
        addButtonL.getStyleClass().add("action-button");
        addButtonL.setVisible(false);
        StackPane.setAlignment(addButtonL, Pos.CENTER_LEFT);
        addButtonL.setTranslateX(-8);
        children.add(addButtonL);
    }

    public void addButtonR(ObservableList<Node> children) {
        addButtonR = new Button("✚");
        addButtonR.getStyleClass().add("action-button");
        addButtonR.setVisible(false);
        StackPane.setAlignment(addButtonR, Pos.CENTER_RIGHT);
        addButtonR.setTranslateX(8);
        children.add(addButtonR);
    }

    private void addListener() {
        // 使用 addEventFilter 的话，OnMouseClicked 的默认行为会让 MindNode 获得焦点，TextArea 就会失去焦点
        // 添加 e.consume() 的话，能阻止 OnMouseClicked 的默认行为，但是 addButton 就不会触发
        // 使用 setOnMouseClicked 的话，由于 addButton 是一个独立的 Button 组件，它会消费鼠标事件，事件不会冒泡到父节点 MindNode，
        // 需要在 addButton 的事件处理逻辑中添加 setselectedNode(model);
        contentBox.setOnMouseClicked(e -> {
            onAction.accept(MindNodeEvent.SELECT);
            onAction.accept(MindNodeEvent.PASTE_SIBLING);
        });

        addAddBtnListener();

        addImageListener();

        // 文本变化调整节点大小
        textArea.textProperty()
                .addListener((obs, oldText, newText) -> adjust());
    }

    /**
     * 按钮监听
     */
    private void addAddBtnListener() {
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

                if (getBoundsInLocal().getWidth() - SizeConstants.BUTTON_THRESHOLD < e.getX() &&
                        midHeight - SizeConstants.BUTTON_THRESHOLD < y && y < midHeight + SizeConstants.BUTTON_THRESHOLD) {
                    addButtonR.setVisible(true);
                }
                if (e.getX() < SizeConstants.BUTTON_THRESHOLD &&
                        midHeight - SizeConstants.BUTTON_THRESHOLD < y && y < midHeight + SizeConstants.BUTTON_THRESHOLD) {
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

    public void addButtonListenR() {
        setOnMouseMoved(e -> {
            double midHeight = getBoundsInLocal().getHeight() / 2;
            double y = e.getY();
            if (getBoundsInLocal().getWidth() - SizeConstants.BUTTON_THRESHOLD < e.getX() &&
                    midHeight - SizeConstants.BUTTON_THRESHOLD < y && y < midHeight + SizeConstants.BUTTON_THRESHOLD) {
                addButtonR.setVisible(true);
            }
        });
        setOnMouseExited(e -> addButtonR.setVisible(false));

        addButtonR.setOnAction(e -> onAction.accept(MindNodeEvent.ADD_BUTTON_R));
    }

    public void addButtonListenL() {
        setOnMouseMoved(e -> {
            double midHeight = getBoundsInLocal().getHeight() / 2;
            double y = e.getY();
            if (e.getX() < SizeConstants.BUTTON_THRESHOLD &&
                    midHeight - SizeConstants.BUTTON_THRESHOLD < y && y < midHeight + SizeConstants.BUTTON_THRESHOLD) {
                addButtonL.setVisible(true);
            }
        });
        setOnMouseExited(e -> addButtonL.setVisible(false));

        addButtonL.setOnAction(e -> onAction.accept(MindNodeEvent.ADD_BUTTON_L));
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
                        imageName = FileUtil.saveImage(bufferedImage, imageName);

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

            // 不能合并两个 x 轴的判断，当在右下角出现了缩放图标后，往上移动，x 轴不变时，会进入 x 轴的分支，导致缩放图标不恢复
            if (imageWidth - SizeConstants.RESIZE_THRESHOLD < e.getX() && imageHeight - SizeConstants.RESIZE_THRESHOLD < e.getY()) {
                imageContainer.setCursor(Cursor.SE_RESIZE);
            } else if (imageWidth - SizeConstants.RESIZE_THRESHOLD < e.getX() && e.getY() < SizeConstants.BUTTON_THRESHOLD) {
                imageContainer.setCursor(Cursor.HAND);
                closeButton.setVisible(true);
            } else {
                imageContainer.setCursor(Cursor.DEFAULT);
            }
        });

        closeButton.setOnAction(e -> {
            image.setImage(null);
            imageContainer.setVisible(false);
            imageContainer.setManaged(false);
            FileUtil.deleteImage(imageName);
            imageName = null;
            adjust();
        });

        // 缩放
        imageContainer.setOnMousePressed(e -> {
            startX = e.getSceneX();
            startWidth = image.getFitWidth();

            if (image.getBoundsInLocal().getWidth() - SizeConstants.RESIZE_THRESHOLD < e.getX()
                    && image.getBoundsInLocal().getHeight() - SizeConstants.RESIZE_THRESHOLD < e.getY()) {
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
                // setVisible(false) 后，maxWidth 就变成0了
                textArea.setMaxWidth(SizeConstants.MIN_TEXTAREA_WIDTH);
                textArea.setVisible(true);
                textArea.requestFocus();
                setPrefHeight(getPrefHeight() + SizeConstants.MIN_TEXTAREA_HEIGHT);
                setLayoutY(getLayoutY() - SizeConstants.HALF_MIN_TEXTAREA_HEIGHT);

                if (pos == PosConstants.RIGHT) {
                    onAction.accept(MindNodeEvent.ADJUST_YR);
                } else {
                    onAction.accept(MindNodeEvent.ADJUST_YL);
                }
            }
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
     * 调整尺寸和位置
     */
    public void adjust() {
        double oldWidth = getPrefWidth();
        adjustSize();

        // 调整位置
        if (pos == PosConstants.LEFT) {
            setLayoutX(getLayoutX() - (getPrefWidth() - oldWidth));
            onAction.accept(MindNodeEvent.ADJUST_L);
        } else {
            onAction.accept(MindNodeEvent.ADJUST_R);
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
        double textWidth;
        double textHeight;

        boolean textEmpty = text.isEmpty();
        if (!imageVisible && textEmpty) {
            textWidth = SizeConstants.MIN_TEXTAREA_WIDTH;
            textHeight = SizeConstants.MIN_TEXTAREA_HEIGHT;
            nodeWidth = SizeConstants.MIN_NODE_WIDTH;
            nodeHeight = SizeConstants.MIN_NODE_HEIGHT;
        } else {
            // todo 宽度
            textWidth = Math.min(MeasureTextUtil.getTextWidth(text), SizeConstants.MAX_TEXTAREA_WIDTH);
            // textArea 宽度 + border(2px) + padding(20px)
            nodeWidth = Math.max(SizeConstants.MIN_NODE_WIDTH, textWidth + 22) * 1.01;
            if (imageVisible) {
                // 文本宽度 < 图片宽度时，宽度 = 图片宽度 + border(4px) + padding(20px)
                nodeWidth = Math.max(nodeWidth, image.getFitWidth() + 24);
            }
            // textArea 高度 + border(2px) + padding(20px) [+ image 高度 + border(2px)]
            textHeight = MeasureTextUtil.getTextHeight() * 1.023;
            nodeHeight = textHeight + 22;
            if (imageVisible) {
                nodeHeight += image.getFitHeight() + 2;
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
        child.setNodeParent(this);
    }

    public void addChildL(MindNode child) {
        childrenL.add(child);
        child.setNodeParent(this);
    }

    public void addChildRAt(int index, MindNode child) {
        childrenR.add(index, child);
        child.setNodeParent(this);
    }

    public void addChildLAt(int index, MindNode child) {
        childrenL.add(index, child);
        child.setNodeParent(this);
    }

    public void removeChildR(MindNode child) {
        childrenR.remove(child);
        child.setNodeParent(null);
    }

    public void removeChildL(MindNode child) {
        childrenL.remove(child);
        child.setNodeParent(null);
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
        totalHeight += SizeConstants.NODE_GAP_Y * (childrenR.size() - 1);
        return totalHeight;
    }

    public double getChildrenHeightL() {
        double totalHeight = 0;
        for (MindNode child : childrenL) {
            totalHeight += child.getHeightL();
        }
        totalHeight += SizeConstants.NODE_GAP_Y * (childrenL.size() - 1);
        return totalHeight;
    }

    /**
     * 节点的高度
     * Math.max（当前节点的高度，子节点的总高度）
     */
    private double getHeightR() {
        if (childrenR.isEmpty()) {
            return getPrefHeight();
        }
        return Math.max(getPrefHeight(), getChildrenHeightR());
    }

    private double getHeightL() {
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

}