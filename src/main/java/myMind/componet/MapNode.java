package myMind.componet;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.Data;
import lombok.EqualsAndHashCode;
import myMind.common.constants.ConfigConstants;
import myMind.common.constants.NodeConstants;
import myMind.common.constants.NodeEvent;
import myMind.common.constants.PosConstants;
import myMind.common.util.FileUtil;
import myMind.common.util.IdGenerator;
import myMind.common.util.MeasureTextUtil;
import myMind.common.util.MessageUtil;
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
import java.util.function.Consumer;

@Data
// 从 nodesLayer 中 remove 时，要用到 equals，不能依赖可变的属性
@EqualsAndHashCode(of = "nodeId")
public class MapNode extends StackPane {
    private final long nodeId;
    private long subjectId;

    //节点之间的关系
    private byte pos;
    private MapNode parentNode;
    private final List<MapNode> childrenR = new ArrayList<>();
    private final List<MapNode> childrenL = new ArrayList<>();

    private MapNode outgoingReference;
    private List<MapNode> incomingReferences;

    private Consumer<NodeEvent> onAction;
    private Consumer<Double> setSubjectTranslateY;
    private Consumer<Double> setSubjectTranslateX;

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

    public MapNode(byte pos, double x, double y) {
        this(pos);
        setLayoutX(x);
        setLayoutY(y);
    }

    public MapNode(byte pos) {
        this.pos = pos;
        nodeId = IdGenerator.nextId();
        StyleClassedTextArea textArea = new MapTextArea();
        textArea.setMaxWidth(NodeConstants.EMPTY_TEXTAREA_WIDTH);
        // 不能用 this()，它必须在第一行
        // 用 this() 或 super() 时，不能使用任何实例字段
        buildNode(textArea);
    }

    public MapNode(byte pos, long id, StyleClassedTextArea textArea) {
        this.pos = pos;
        nodeId = id;
        buildNode(textArea);
    }

    private void buildNode(StyleClassedTextArea textArea) {
        this.textArea = textArea;

        contentBox = new VBox(textArea);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setPadding(new Insets(NodeConstants.PADDING));
        getChildren().add(contentBox);

        // 中间节点会添加两个按钮
        if (pos != PosConstants.LEFT) {
            addButton(PosConstants.RIGHT);
        }
        if (pos != PosConstants.RIGHT) {
            addButton(PosConstants.LEFT);
        }

        getStyleClass().add("node");
        setPrefWidth(NodeConstants.MIN_NODE_WIDTH);
        setPrefHeight(NodeConstants.MIN_NODE_HEIGHT);

        addListener();
    }

    public void addButton(byte pos) {
        if (pos == PosConstants.RIGHT) {
            addButtonR = new Button(NodeConstants.ADD);
            addButtonR.getStyleClass().addAll("add-button", "add-button-r");
            addButtonR.setVisible(false);
            StackPane.setAlignment(addButtonR, Pos.CENTER_RIGHT);
            getChildren().add(addButtonR);
        } else {
            addButtonL = new Button(NodeConstants.ADD);
            addButtonL.getStyleClass().addAll("add-button", "add-button-l");
            addButtonL.setVisible(false);
            StackPane.setAlignment(addButtonL, Pos.CENTER_LEFT);
            getChildren().add(addButtonL);
        }
    }

    public void removeAddButton(byte pos) {
        if (pos == PosConstants.RIGHT) {
            getChildren().remove(addButtonR);
            addButtonR.setOnAction(null);
            addButtonR = null;
        } else {
            getChildren().remove(addButtonL);
            addButtonL.setOnAction(null);
            addButtonL = null;
        }
    }

    public void setImage(String imageName, double imageWidth, double imageHeight) {
        buildImageContainer();
        imageView.setImage(new Image(new File(ConfigConstants.DIR_IMAGE + imageName).toURI().toString()));
        imageView.setFitWidth(imageWidth);
        imageView.setFitHeight(imageHeight);
        ratio = imageWidth / imageHeight;
        this.imageName = imageName;
    }

    private void buildImageContainer() {
        imageView = new ImageView();
        imageView.setSmooth(true);

        closeButton = new Button(NodeConstants.CLOSE);
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

    private void addListener() {
        // 由于 addButton 是一个独立的 Button 组件，它会消费鼠标事件，事件不会冒泡到父节点 MapNode，
        // 需要在 addButton 的事件处理逻辑中添加 setSelectedNode(model)
        // 如果在选中文本时，拖到节点外面，不会触发点击事件，因为“按下”一个节点后，拖到到其他地方再“释放”，不会触发 MOUSE_CLICKED
        // MOUSE_PRESSED 保证在按下时，就切换选中节点
        contentBox.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.isShortcutDown()) {
                onAction.accept(NodeEvent.JUMP);
            } else {
                // 让光标定位先执行，避免样式切换导致后续算出来的字符索引比实际点击位置靠后
                Platform.runLater(() -> onAction.accept(NodeEvent.CLICK));
            }
        });

        addButtonListen();

        // 粘贴图片
        setOnKeyReleased(event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.V) {
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
                        imageView.setFitWidth(imageWidth / ConfigConstants.SCALE);
                        imageView.setFitHeight(imageHeight / ConfigConstants.SCALE);
                        ratio = imageWidth / imageHeight;
                        imageName = FileUtil.saveImage(bufferedImage, imageName);

                        adjust(true);
                    } catch (UnsupportedFlavorException | IOException ex) {
                        MessageUtil.showMessage("粘贴失败：" + ex.getMessage());
                    }
                }

                event.consume();
            }
        });

        // 文本变化调整节点大小
        textArea.textProperty()
                .addListener((obs, oldText, newText) -> adjust(false));

        textArea.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                // 清除选区，恢复背景色
                textArea.deselect();
                adjust(true);
            }
        });

//        textArea.selectedTextProperty().addListener((obs, oldVal, newVal) -> {
//            onAction.accept(NodeEvent.CLICK);
//        });
    }

    /**
     * 按钮监听
     */
    private void addButtonListen() {
        // 鼠标移入左右中心点时，显示添加按钮
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
        }else {
            addButtonListen(pos);
        }
    }

    public void addButtonListen(byte pos) {
        if (pos == PosConstants.RIGHT) {
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
        } else {
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
            adjust(true);
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
                adjust(true);
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
                textArea.setMaxWidth(NodeConstants.EMPTY_TEXTAREA_WIDTH);
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
    public void adjust(boolean exact) {
        double oldWidth = getPrefWidth();
        double oldHeight = getPrefHeight();
        adjustSize(exact);
        setSubjectTranslateY.accept(-(getPrefHeight() - oldHeight) * 0.5);

        // 调整位置
        if (pos == PosConstants.LEFT) {
            setLayoutX(getLayoutX() - (getPrefWidth() - oldWidth));
            // 与 scene 左边的距离
            Point2D sceneCoords = localToScene(0, 0);
            double nodeX = sceneCoords.getX();
            if (nodeX < 0) {
                setSubjectTranslateX.accept(-nodeX);
            }
            onAction.accept(NodeEvent.ADJUST_L);
        } else {
            Point2D sceneCoords = localToScene(0, 0);
            double nodeX = sceneCoords.getX();
            if (getScene().getWidth() < nodeX + getPrefWidth()) {
                double dx = nodeX + getPrefWidth() - getScene().getWidth();
                setSubjectTranslateX.accept(-dx);
            }
            onAction.accept(NodeEvent.ADJUST_R);
        }
    }

    /**
     * 根据内容调整尺寸
     *
     * @param exact 是否精确调整宽度，新增文本时不精准调整，使得增加微小宽度时，不改变宽度
     */
    public void adjustSize(boolean exact) {
        String text = textArea.getText();
        boolean imageVisible = imageName != null;
        double nodeWidth;
        double nodeHeight;
        double textWidth;
        double textHeight;

        boolean textEmpty = text.isEmpty();
        if (!imageVisible && textEmpty) {
            textWidth = NodeConstants.EMPTY_TEXTAREA_WIDTH;
            textHeight = NodeConstants.MIN_TEXTAREA_HEIGHT;
            nodeWidth = NodeConstants.MIN_NODE_WIDTH;
            nodeHeight = NodeConstants.MIN_NODE_HEIGHT;
        } else {
            // textArea 宽度 + border + padding
            textWidth = MeasureTextUtil.getTextWidth(text);
            // 节点在最小宽度时，文本保持居中
            if (!exact && textArea.getMaxWidth() > NodeConstants.MIN_TEXTAREA_WIDTH) {
                textWidth = textWidth > textArea.getMaxWidth() ? textWidth + 50 : textArea.getMaxWidth();
            }
            textWidth = Math.min(textWidth, NodeConstants.MAX_TEXTAREA_WIDTH);
            nodeWidth = Math.max(NodeConstants.MIN_NODE_WIDTH, (textWidth + NodeConstants.BORDER_AND_PADDING) * 1.01);
            if (imageVisible) {
                // 文本宽度 < 图片宽度时，宽度 = 图片宽度 + border + padding
                nodeWidth = Math.max(nodeWidth, imageView.getFitWidth() + NodeConstants.BORDER_AND_PADDING + 2.6);
            }

            // textArea 高度 + border + padding [+ image 高度 + border]
            if (getPrefWidth() >= NodeConstants.MAX_TEXTAREA_WIDTH && !text.contains("\n")) {
                textHeight = MeasureTextUtil.getTextHeight() * 1.06;
            } else {
                textHeight = MeasureTextUtil.getTextHeight() * 1.022;
            }
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

    //—————————————————————————————————————————增—————————————————————————————————————————
    public void addChild(MapNode child, byte pos) {
        if (pos == PosConstants.RIGHT) {
            childrenR.add(child);
        } else {
            childrenL.add(child);
        }
        child.setParentNode(this);
    }

    public void addChildAt(int index, MapNode child, byte pos) {
        if (pos == PosConstants.RIGHT) {
            childrenR.add(index, child);
        } else {
            childrenL.add(index, child);
        }
        child.setParentNode(this);
    }

    //—————————————————————————————————————————删—————————————————————————————————————————

    /**
     * 在子节点 List 中删除 child，设置 child 的 parentNode 为 null
     *
     */
    public void removeChild(MapNode child, byte pos) {
        removeChild(child, null, pos);
    }

    /**
     * 在子节点 List 中删除 child，设置 child 的 parentNode
     *
     */
    public void removeChild(MapNode child, MapNode parentNode, byte pos) {
        if (pos == PosConstants.RIGHT) {
            childrenR.remove(child);
        } else {
            childrenL.remove(child);
        }
        child.setParentNode(parentNode);
    }

    //———————————————————————————————————————————宽高计算———————————————————————————————————————————

    /**
     * 子节点的总高度
     * 所有子节点的高度 + 间隔
     *
     */
    public double getChildrenHeight(byte pos) {
        double totalHeight = 0;
        int size = 0;
        for (MapNode child : getChildren(pos)) {
            if (child.isVisible()) {
                totalHeight += child.getHeight(pos);
                size++;
            }
        }
        totalHeight += size != 0 ? NodeConstants.GAP_Y * (size - 1) : 0;
        return totalHeight;
    }

    /**
     * 节点的高度
     * Math.max（当前节点的高度，子节点的总高度）
     *
     */
    public double getHeight(byte pos) {
        // 每个调用的地方都可见，不用判断 isVisible
        if (getChildren(pos).isEmpty()) {
            return getPrefHeight();
        }
        return Math.max(getPrefHeight(), getChildrenHeight(pos));
    }

    //———————————————————————————————————————————位置计算———————————————————————————————————————————
    public double getStartY(byte pos) {
        List<MapNode> children = getChildren(pos);
        // 找到第一个可见的子节点
        MapNode fistNode = children.get(0);
        for (int i = 1; i < children.size() && !fistNode.isVisible(); i++) {
            fistNode = children.get(i);
        }
        // 从 adjustChildrenY 调用这个方法时，必然有可见的子节点，
        // 递归时，可能从头找到尾都是收起的节点，返回到上层的 Math.min(selfEndY, Integer.MAX_VALUE)
        if (!fistNode.isVisible()) {
            return Integer.MAX_VALUE;
        }
        if (!fistNode.getChildren(pos).isEmpty()) {
            // 当前节点可能比子节节点的总高度更高
            return Math.min(fistNode.getLayoutY(), fistNode.getStartY(pos));
        } else {
            return fistNode.getLayoutY();
        }
    }

    public double getEndY(byte pos) {
        MapNode lastNode = getLastChild(pos);
        List<MapNode> children = getChildren(pos);
        for (int i = children.size() - 2; i >= 0 && !lastNode.isVisible(); i--) {
            lastNode = children.get(i);
        }
        double selfEndY = lastNode.getLayoutY() + lastNode.getPrefHeight();
        if (!lastNode.isVisible()) {
            return Integer.MIN_VALUE;
        }
        if (!lastNode.getChildren(pos).isEmpty()) {
            return Math.max(selfEndY, lastNode.getEndY(pos));
        } else {
            return selfEndY;
        }
    }

    //———————————————————————————————————————————其他———————————————————————————————————————————

    public boolean isEmpty() {
        return childrenR.isEmpty() && childrenL.isEmpty() &&
                textArea.getText().isEmpty() && imageName == null;
    }

    @Override
    public String toString() {
        return textArea.getText();
    }

    public void addIncomingReference(MapNode node) {
        if (incomingReferences == null) {
            incomingReferences = new ArrayList<>();
        }
        incomingReferences.add(node);
    }

    public List<MapNode> getChildren(byte pos) {
        if (pos == PosConstants.RIGHT) {
            return childrenR;
        } else {
            return childrenL;
        }
    }

    public void setAddButtonText(String text, byte pos) {
        if (pos == PosConstants.RIGHT) {
            addButtonR.setText(text);
        } else {
            addButtonL.setText(text);
        }
    }

    public MapNode getLastChild(byte pos) {
        List<MapNode> children = getChildren(pos);
        return children.get(children.size() - 1);
    }
}