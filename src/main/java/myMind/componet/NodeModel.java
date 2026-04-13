package myMind.componet;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.Setter;
import myMind.constants.SizeConstants;

import java.util.ArrayList;
import java.util.List;

public class NodeModel {
	//结点内部属性
	@Getter
	private final int id;
	@Getter
	@Setter
	private MindNode mindNode;
	private final StringProperty text = new SimpleStringProperty();
	private final DoubleProperty x = new SimpleDoubleProperty();
	private final DoubleProperty y = new SimpleDoubleProperty();

	//结点之间的关系
	@Getter
	private NodeModel parent;
	@Getter
	private final List<NodeModel> children = new ArrayList<>();

	public NodeModel(int id, String text, double x, double y) {
		this.id = id;
		this.text.set(text);
		this.x.set(x);
		this.y.set(y);
	}

	public String getText() {
		return text.get();
	}

	public void setText(String text) {
		this.text.set(text);
	}

	public StringProperty textProperty() {
		return text;
	}

	public double getX() {
		return x.get();
	}

	public void setX(double x) {
		this.x.set(x);
	}

	public DoubleProperty xProperty() {
		return x;
	}

	public double getY() {
		return y.get();
	}

	public void setY(double y) {
		this.y.set(y);
	}

	public DoubleProperty yProperty() {
		return y;
	}

	public void setParent(NodeModel parent) {
		this.parent = parent;
	}

	public void addChild(NodeModel child) {
		children.add(child);
		child.setParent(this);
	}

	public void removeChild(NodeModel child) {
		children.remove(child);
		child.setParent(null);
	}

	/**
	 * 获取所有子结点的中间位置
	 */
	public double getMidY() {
		double totalChildrenHeight = 0;
		for (NodeModel child : children) {
			MindNode childNode = child.getMindNode();
			if (childNode != null) {
				totalChildrenHeight += childNode.getHeight();
			}
		}
		totalChildrenHeight += SizeConstants.NODE_GAP_Y * (children.size() - 1);

		double y1 = children.get(0).getY();
		return y1 + totalChildrenHeight / 2;
	}
}