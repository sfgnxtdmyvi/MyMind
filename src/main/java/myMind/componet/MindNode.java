package myMind.componet;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import lombok.Getter;
import myMind.constants.SizeConstants;
import myMind.controller.NodeController;
import org.fxmisc.richtext.InlineCssTextArea;

import java.util.List;


@Getter
public class MindNode extends StackPane {
    private final NodeModel model;
    private final InlineCssTextArea textArea;
    private final NodeController controller;
    //用于测量文本尺寸
    private Text measureText;

    public MindNode(NodeModel model, NodeController controller) {
        this.model = model;
        model.setMindNode(this);
        this.controller = controller;

        textArea = new InlineCssTextArea();
        textArea.replaceText(0, 0, model.getText());
        textArea.setWrapText(true);
        textArea.getStyleClass().add("nodeContent");
        // 让绘制连线时，能获取节点位置
        textArea.setPrefWidth(SizeConstants.MIN_TEXTAREA_WIDTH);
        textArea.setPrefHeight(SizeConstants.MIN_TEXTAREA_HEIGHT);

        measureText = new Text();
        measureText.setFont(Font.font("System", SizeConstants.NODE_FONT_SIZE));

        // 样式
        setPrefWidth(SizeConstants.MIN_NODE_WIDTH);
        setPrefHeight(SizeConstants.MIN_NODE_HEIGHT);
        getStyleClass().add("nodeBorder");
        setPadding(new Insets(10, 10, 10, 10));
        getChildren().add(textArea);

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
        //InlineCssTextArea 内部调用了 e.consume()，MindNode的setOnMousePressed不会触发
        //addEventFilter 在捕获阶段执行
        //捕获阶段（Capturing）：从根节点一路向下直到事件源节点
        //冒泡阶段（Bubbling）：从事件源节点向上传播，如果中间有一个节点调用了 e.consume()，事件将不会继续传播
        addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            controller.setSelectedNode(this);
        });

        textArea.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                model.setText(textArea.getText());
                controller.refreshLines();
            }
        });

        // 监听文本变化，动态调整
        textArea.textProperty().addListener((obs, oldText, newText) -> {
            Platform.runLater(this::adjustSize);
        });
    }


    /**
     * 根据内容动态调整 TextArea 尺寸
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

        textArea.setPrefWidth(textWidth);
        textArea.setPrefHeight(textHeight);
        setPrefWidth(nodeWidth);
        setPrefHeight(nodeHeight);

        adjustChildrenX(model);
        if (!model.getChildren().isEmpty()) {
            controller.adjustParent(model);
        }
        controller.adjustChildrenY();
        controller.refreshLines();
    }

    /**
     * 调整子节点x轴
     */
    private void adjustChildrenX(NodeModel nodeModel) {
        List<NodeModel> children = nodeModel.getChildren();
        if (children.isEmpty()) {
            return;
        }

        double parentX = nodeModel.getX();
        double parentWidth = nodeModel.getMindNode().getPrefWidth();
        double childX = parentX + parentWidth + SizeConstants.NODE_GAP_X;

        for (NodeModel child : children) {
            child.setX(childX);
            adjustChildrenX(child);
        }
    }

}