package myMind.controller;

import javafx.geometry.Point2D;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import myMind.componet.MindPane;
import myMind.componet.MindNode;
import myMind.componet.NodeModel;
import myMind.constants.SizeConstants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Data
public class NodeController {
    private final MindPane mindPane = new MindPane(this);
    private final Map<Integer, MindNode> nodeMap = new HashMap<>();
    private NodeModel rootModel;
    private MindNode selectedNode = null;
    private final AtomicInteger idGenerator = new AtomicInteger(1);

    public void initRootNode(double centerX, double centerY) {
        rootModel = new NodeModel(nextId(), "", centerX, centerY);
        addNode(rootModel);
    }

    public void addChild() {
        if (selectedNode == null) {
            return;
        }
        NodeModel parentModel = selectedNode.getModel();

        // 基于已有最后一个子节点位置加上偏移
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

        adjustChildrenY();
    }

    public void addSibling() {
        if (selectedNode == null) {
            return;
        }
        NodeModel nodeModel = selectedNode.getModel();
        NodeModel parentModel = nodeModel.getParent();
        if (parentModel == null ) {
            return;
        }

        // 基于当前节点位置加上偏移
        double siblingX = nodeModel.getX();
        double siblingY = nodeModel.getY() + selectedNode.getLayoutBounds().getHeight() + SizeConstants.NODE_GAP_Y;

        NodeModel siblingModel = new NodeModel(nextId(), "", siblingX, siblingY);
        parentModel.addChild(siblingModel);
        addNode(siblingModel);

        adjustChildrenY();
    }

    public void delete() {
        if (selectedNode == null) {
            return;
        }
        NodeModel toDelete = selectedNode.getModel();
        if (toDelete == rootModel) {
            return;
        }
        NodeModel parent = toDelete.getParent();

        //变化选中节点
        List<NodeModel> children = parent.getChildren();
        if (children.size() == 1) {
            setSelectedNode(parent.getMindNode());
        } else {
            int index = children.indexOf(toDelete);
            if (index != children.size() - 1) {
                setSelectedNode(children.get(index + 1).getMindNode());
            } else {
                setSelectedNode(children.get(index - 1).getMindNode());
            }
        }

        // 从父节点中移除
        parent.removeChild(toDelete);
        // 从nodeMap和mindPane中删除
        deleteChildren(toDelete);
        deleteNode(toDelete);

        adjustChildrenY();
    }

    public void adjustChildrenY() {
        adjustChildrenY(rootModel, null);
    }

    public void refreshLines() {
        Pane linesLayer = mindPane.getLinesLayer();
        linesLayer.getChildren().clear();

        // 找到每一个子节点的父节点，创建连接线
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

    public void setSelectedNode(MindNode node) {
        this.selectedNode = node;
        if (node != null) {
            selectedNode.getTextArea().requestFocus();
        }
    }

    /**
     * 调整父节点位置
     */
    public void adjustParent(NodeModel model) {
        model.setY(model.getMidY() - model.getMindNode().getHeight() / 2.0);

        NodeModel parentModel = model.getParent();
        if (parentModel != null) {
            adjustParent(parentModel);
        } else {
            refreshLines();
        }
    }

    public void clearAll() {
        mindPane.getNodesLayer().getChildren().clear();
        mindPane.getLinesLayer().getChildren().clear();
        nodeMap.clear();
        selectedNode = null;
    }

    public void rebuildViewFromModel(NodeModel node) {
        addNode(node);
        for (NodeModel child : node.getChildren()) {
            rebuildViewFromModel(child);
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

    private void deleteChildren(NodeModel parentNodeModel) {
        for (NodeModel childNodeModel : parentNodeModel.getChildren()) {
            deleteNode(childNodeModel);
            deleteChildren(childNodeModel);
        }

        parentNodeModel.removeChildren();
    }

    /**
     * 从nodeMap和mindPane中删除
     *
     * @param childNodeModel
     */
    private void deleteNode(NodeModel childNodeModel) {
        MindNode childNode = nodeMap.remove(childNodeModel.getId());
        mindPane.getNodesLayer().getChildren().remove(childNode);
    }

    /**
     * 调整子节点的Y轴位置，父节点在所有子节点的中间
     */
    private void adjustChildrenY(NodeModel parentModel, Double y) {
        List<NodeModel> children = parentModel.getChildren();
        if (children.isEmpty()) {
            return;
        }

        // 递归时，y 不为空，以传入的 y 为第一个子节点的Y坐标
        double childY;
        if (y == null) {
            double totalHeight = calculateTotalHeight(children);
            double parentMidY = parentModel.getY() + parentModel.getMindNode().getHeight() / 2.0;
            childY = parentMidY - totalHeight / 2.0;
        } else {
            childY = y;
        }

        // 依次调整所有子节点
        for (NodeModel child : children) {
            MindNode childNode = child.getMindNode();
            List<NodeModel> childrenOfChild = child.getChildren();
            // Todo 父结点的高度大于子结点高度时
            if (!childrenOfChild.isEmpty()) {
                adjustChildrenY(child, childY);
                double midY = child.getMidY();
                child.setY(midY - childNode.getHeight() / 2.0);

                // 下一个子节点的Y坐标 = 最后一个子节点的底部 + 间距
                childY = child.getEndY() + SizeConstants.NODE_GAP_Y;
            } else {
                child.setY(childY);
                // 下一个子节点的Y坐标 = 当前Y + 当前节点高度 + 间距
                childY += childNode.getPrefHeight() + SizeConstants.NODE_GAP_Y;
            }
        }

        refreshLines();
    }

    /**
     * 计算所有子孙节点的总高度，
     * 每个子节点高度 + 间距 * (子节点数量 - 1)
     */
    private double calculateTotalHeight(List<NodeModel> children) {
        double totalHeight = 0;
        for (NodeModel child : children) {
            totalHeight += child.getTotalHeight();
        }
        totalHeight += SizeConstants.NODE_GAP_Y * (children.size() - 1);

        return totalHeight;
    }

    private Point2D getRightPoint(MindNode node) {
        double x = node.getLayoutX() + node.getPrefWidth();
        double y = node.getLayoutY() + node.getPrefHeight() / 2;
        return new Point2D(x, y);
    }

    private Point2D getLeftPoint(MindNode node) {
        double x = node.getLayoutX();
        double y = node.getLayoutY() + node.getPrefHeight() / 2;
        return new Point2D(x, y);
    }

    private int nextId() {
        return idGenerator.getAndIncrement();
    }
}