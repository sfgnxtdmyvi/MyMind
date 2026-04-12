package myMind.componet;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import lombok.Getter;
import myMind.controller.MindController;
import myMind.constants.SizeConstants;
import org.fxmisc.richtext.InlineCssTextArea;


public class MindNode extends StackPane {
    @Getter
    private final NodeModel model;
    private final InlineCssTextArea textArea;
    private double dragStartX, dragStartY;
    private double mousePressedX;
    private double mousePressedY;
    //用于测量文本实际尺寸
    private Text measureText;

    public MindNode(NodeModel model, MindController controller) {
        this.model = model;
        textArea = new InlineCssTextArea();
        textArea.replaceText(0, 0, model.getText());

        textArea.setWrapText(true);
        textArea.getStyleClass().add("nodeContent");
        textArea.setMinWidth(SizeConstants.MIN_NODE_WIDTH);
        textArea.setMinHeight(SizeConstants.MIN_NODE_HEIGHT);
        textArea.setMaxWidth(SizeConstants.MAX_NODE_WIDTH);
        textArea.setPrefWidth(SizeConstants.MIN_NODE_WIDTH);
        textArea.setPrefHeight(SizeConstants.MIN_NODE_HEIGHT);
        textArea.setPadding(new Insets(10, 10, 10, 10));

        measureText = new Text();
        measureText.setFont(Font.font("System", SizeConstants.NODE_FONT_SIZE));

        // 样式
        getStyleClass().add("nodeBorder");
        getChildren().add(textArea);

        // 模型x、y变化时，改变位置
        model.xProperty().addListener((obs, oldVal, newVal) -> setLayoutX(newVal.doubleValue()));
        model.yProperty().addListener((obs, oldVal, newVal) -> setLayoutY(newVal.doubleValue()));
        setLayoutX(model.getX());
        setLayoutY(model.getY());

        // 初始调整尺寸
        Platform.runLater(this::adjustTextArea);

        addListener(model, controller);
    }

    private void addListener(NodeModel model, MindController controller) {
        // 鼠标拖拽移动结点
        setOnMousePressed(e -> {
            //PRIMARY = 左键
            //SECONDARY = 右键
            //MIDDLE = 滚轮
            if (e.getButton() == MouseButton.PRIMARY) {
                // 获取鼠标按下时的坐标
                mousePressedX = e.getSceneX();
                mousePressedY = e.getSceneY();

                //记录拖拽起始位置
                //鼠标距离节点左边缘的距离
                dragStartX = mousePressedX - getLayoutX();
                dragStartY = mousePressedY - getLayoutY();
                e.consume();
            }

            controller.setSelectedNode(this);
        });

        setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                double newX = e.getSceneX() - dragStartX;
                double newY = e.getSceneY() - dragStartY;
                // 限制边界防止拖出视野外（可选）
                newX = Math.max(20, Math.min(newX, controller.getMindMapPane().getWidth() - getWidth()));
                newY = Math.max(20, Math.min(newY, controller.getMindMapPane().getHeight() - getHeight()));
                model.setX(newX);
                model.setY(newY);

                controller.refreshLines();
                e.consume();
            }
        });

        // 编辑文本
        setOnMouseReleased(e -> {
            if (e.getButton() == MouseButton.PRIMARY
                    // 按下位置与释放位置相同才编辑
                    && mousePressedX == e.getSceneX() && mousePressedY == e.getSceneY()) {
                // 请求获得键盘焦点，光标会出现在输入框中
                textArea.requestFocus();
            }
        });

        textArea.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) finishEdit(textArea.getText(), controller);
        });

        // 监听文本变化，动态调整
        textArea.textProperty().addListener((obs, oldText, newText) -> {
            Platform.runLater(this::adjustTextArea);
        });
    }

    private void finishEdit(String newText, MindController controller) {
        model.setText(newText);

        // 文本变化会改变结点尺寸，刷新连线端点
        controller.refreshLines();
    }

    /**
     * 根据内容动态调整 TextArea 尺寸
     */
    private void adjustTextArea() {
        String text = textArea.getText();
        if (text == null || text.isEmpty()) {
            textArea.setPrefWidth(SizeConstants.MIN_NODE_WIDTH);
            textArea.setPrefHeight(SizeConstants.MIN_NODE_HEIGHT);
            setPrefWidth(SizeConstants.MIN_NODE_WIDTH);
            setPrefHeight(SizeConstants.MIN_NODE_HEIGHT);
            return;
        }

        measureText.setText(text);
        measureText.setWrappingWidth(0);

        double textWidth = measureText.getLayoutBounds().getWidth();

        // + 20px padding + 2px border
        double newWidth = textWidth + 22;

        // 限制宽度范围
        newWidth = Math.max(SizeConstants.MIN_NODE_WIDTH,
                Math.min(newWidth, SizeConstants.MAX_NODE_WIDTH));

        // 设置换行
        measureText.setWrappingWidth(SizeConstants.MAX_NODE_WIDTH - 22);
        double contentHeight = measureText.getLayoutBounds().getHeight();
        System.out.println("文本高度: " + contentHeight);

        // + 20px padding
        double newHeight = contentHeight + 26;
        double height = textArea.getLayoutBounds().getHeight();
        double height1 = getLayoutBounds().getHeight();
        System.out.println("newHeight：" + newHeight);
        System.out.println("文本域高度: " + height);
        System.out.println("MindNode高度: " + height1);
        System.out.println("————————————————————————————————");

        textArea.setPrefWidth(newWidth);
        textArea.setPrefHeight(newHeight);
        setPrefWidth(newWidth);
        setPrefHeight(newHeight+2);
    }

}