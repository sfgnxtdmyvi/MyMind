package myMind.componet;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import lombok.Getter;
import myMind.constants.SizeConstants;
import myMind.controller.MindController;
import org.fxmisc.richtext.InlineCssTextArea;

import java.util.List;


public class MindNode extends StackPane {
	@Getter
	private final NodeModel model;
	private final InlineCssTextArea textArea;
	private final MindController controller;
	private double dragStartX, dragStartY;
	private double mousePressedX;
	private double mousePressedY;
	//用于测量文本尺寸
	private Text measureText;

	public MindNode(NodeModel model, MindController controller) {
		this.model = model;
		model.setMindNode(this);
		this.controller = controller;

		textArea = new InlineCssTextArea();
		textArea.replaceText(0, 0, model.getText());
		textArea.setWrapText(true);
		textArea.getStyleClass().add("nodeContent");
		// 让绘制连线时，能获取结点位置
		textArea.setPrefWidth(SizeConstants.MIN_TEXTAREA_WIDTH);
		textArea.setPrefHeight(SizeConstants.MIN_TEXTAREA_HEIGHT);

		measureText = new Text();
		measureText.setFont(Font.font("System", SizeConstants.NODE_FONT_SIZE));

		// 样式
		getStyleClass().add("nodeBorder");
		setPadding(new Insets(10, 10, 10, 10));
		getChildren().add(textArea);

		// 模型x、y变化时，改变位置
		model.xProperty().addListener((obs, oldVal, newVal) -> setLayoutX(newVal.doubleValue()));
		model.yProperty().addListener((obs, oldVal, newVal) -> setLayoutY(newVal.doubleValue()));
		setLayoutX(model.getX());
		setLayoutY(model.getY());

		// 初始调整尺寸
		Platform.runLater(this::adjustTextArea);

		addListener(model);
	}

	private void addListener(NodeModel model) {
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

//		System.out.println("contentHeight：" + contentHeight);
//		System.out.println("textAreaHeight：" + textHeight);
//		System.out.println("nodeHeight：" + nodeHeight);
//		System.out.println("——————————————————————————————————————————————");

		textArea.setPrefWidth(textWidth);
		textArea.setPrefHeight(textHeight);
		setPrefWidth(nodeWidth);
		setPrefHeight(nodeHeight);

		adjustChildren(nodeWidth);
	}

	private void adjustChildren(double parentWidth) {
		List<NodeModel> children = model.getChildren();
		if (children == null || children.isEmpty()) {
			return;
		}

		// 子结点X坐标
		double parentX = model.getX();
		double childX = parentX + parentWidth + SizeConstants.NODE_GAP_X;

		// 子结点Y坐标
		double totalChildrenHeight = model.getMidY();
		double parentY = model.getY();
		double gapBetweenChildren = SizeConstants.NODE_GAP_Y;
		double childY = parentY - totalChildrenHeight / 2;

		for (NodeModel child : children) {
			MindNode childNode = child.getMindNode();
			if (childNode != null) {
				child.setX(childX);
				child.setY(childY);
				childY += childNode.getHeight() + gapBetweenChildren;
			}
		}

		controller.refreshLines();
	}

}