package myMind.controller;

import javafx.geometry.Point2D;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.stage.FileChooser;
import lombok.Getter;
import lombok.Setter;
import myMind.componet.MindPane;
import myMind.componet.MindNode;
import myMind.componet.NodeModel;
import myMind.constants.SizeConstants;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class NodeController {
	private final MindPane mindPane = new MindPane(this);
	@Getter
	private final Map<Integer, MindNode> nodeMap = new HashMap<>();
	@Setter
	private NodeModel rootModel;
	@Setter
	private MindNode selectedNode = null;
	private final AtomicInteger idGenerator = new AtomicInteger(1);

	public MindPane getMindMapPane() {
		return mindPane;
	}

	public void initRootNode(double centerX, double centerY) {
		rootModel = new NodeModel(nextId(), "", centerX, centerY);
		addNode(rootModel);
	}

	public void addChild() {
		NodeModel parentModel = selectedNode.getModel();

		// 基于已有最后一个子结点位置加上偏移
		List<NodeModel> children = parentModel.getChildren();
		double childX = parentModel.getX() + selectedNode.getWidth() + SizeConstants.NODE_GAP_X;
		double childY;
		if (children == null || children.isEmpty()) {
			childY = parentModel.getY();
		} else {
			NodeModel lastModel = children.get(children.size() - 1);
			double height = lastModel.getMindNode().getLayoutBounds().getHeight();
			double y = lastModel.getY();
			childY = lastModel.getY() + height + SizeConstants.NODE_GAP_Y;
		}

		NodeModel childModel = new NodeModel(nextId(), "", childX, childY);
		parentModel.addChild(childModel);
		addNode(childModel);

		adjustParent(parentModel);
	}

	public void addSibling() {
		NodeModel nodeModel = selectedNode.getModel();

		// 基于当前结点位置加上偏移
		double siblingX = nodeModel.getX();
		double siblingY = nodeModel.getY() + selectedNode.getLayoutBounds().getHeight() + SizeConstants.NODE_GAP_Y;

		NodeModel siblingModel = new NodeModel(nextId(), "", siblingX, siblingY);
		NodeModel parentModel = nodeModel.getParent();
		parentModel.addChild(siblingModel);
		addNode(siblingModel);

		adjustParent(parentModel);
	}

	public void delete() {
		NodeModel toDelete = selectedNode.getModel();
		if (toDelete == rootModel) {
			return;
		}

		// 递归删除所有子结点
		deleteNodeAndChildren(toDelete);
		// 从父结点中移除
		NodeModel parent = toDelete.getParent();
		if (parent != null) {
			parent.removeChild(toDelete);
		}
		removeNode(toDelete);
		selectedNode = null;
		refreshLines();
	}

	public void refreshLines() {
		Pane linesLayer = mindPane.getLinesLayer();
		linesLayer.getChildren().clear();

		// 找到每一个子结点的父结点，创建连接线
		for (MindNode childNode : nodeMap.values()) {
			NodeModel parentModel = childNode.getModel().getParent();

			if (parentModel != null) {
				MindNode parentNode = parentModel.getMindNode();
				if (parentNode != null) {
					Point2D start = getRightPoint(parentNode);
					Point2D end = getLeftPoint(childNode);

					Line line = new Line(start.getX(), start.getY(), end.getX(), end.getY());
					line.setStroke(Color.rgb(100, 100, 100));
					line.setStrokeWidth(2.5);
					line.setStrokeDashOffset(0);

					linesLayer.getChildren().add(line);
				}
			}
		}
	}

	//———————————————————————————————————————————私有方法———————————————————————————————————————————
	private void addNode(NodeModel model) {
		MindNode node = new MindNode(model, this);
		nodeMap.put(model.getId(), node);
		mindPane.getNodesLayer().getChildren().add(node);
		setSelectedNode(node);

		// 强制刷新布局，确保尺寸计算正确，否则node.getHeight()返回0
		mindPane.applyCss();
		mindPane.layout();
	}

	private void removeNode(NodeModel model) {
		MindNode node = nodeMap.remove(model.getId());
		if (node != null) {
			mindPane.getNodesLayer().getChildren().remove(node);
		}
	}

	private void deleteNodeAndChildren(NodeModel node) {
		for (NodeModel child : new ArrayList<>(node.getChildren())) {
			deleteNodeAndChildren(child);
			removeNode(child);
		}
		node.getChildren().clear();
	}

	/**
	 * 调整父结点位置
	 */
	private void adjustParent(NodeModel parentModel) {
		parentModel.setY(parentModel.getMidY() - parentModel.getMindNode().getHeight() / 2.0);
		refreshLines();
	}

	private Point2D getRightPoint(MindNode node) {
		double x = node.getLayoutX() + node.getWidth();
		double y = node.getLayoutY() + node.getHeight() / 2;
		return new Point2D(x, y);
	}

	private Point2D getLeftPoint(MindNode node) {
		double x = node.getLayoutX();
		double y = node.getLayoutY() + node.getHeight() / 2;
		return new Point2D(x, y);
	}

	private int nextId() {
		return idGenerator.getAndIncrement();
	}

	void clearAll() {
		mindPane.getNodesLayer().getChildren().clear();
		mindPane.getLinesLayer().getChildren().clear();
		nodeMap.clear();
		selectedNode = null;
	}

	void rebuildViewFromModel(NodeModel node) {
		addNode(node);
		for (NodeModel child : node.getChildren()) {
			rebuildViewFromModel(child);
		}
	}
}