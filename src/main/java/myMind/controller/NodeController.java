package myMind.controller;

import javafx.geometry.Point2D;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import lombok.Data;
import myMind.componet.MindNode;
import myMind.componet.NodeModel;
import myMind.componet.Subject;
import myMind.constants.PosConstants;
import myMind.constants.SizeConstants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Data
public class NodeController {
    private final Subject subject = new Subject(this);
    private final Map<Integer, MindNode> nodeMap = new HashMap<>();
    private NodeModel rootModel;
    private MindNode selectedNode = null;
    private final AtomicInteger idGenerator = new AtomicInteger(1);

    public void initRootNode(double centerX, double centerY) {
        rootModel = new NodeModel(nextId(), "", centerX, centerY, PosConstants.MIDDLE);
        addNode(rootModel);
    }

    public void addChild(){
        addChildR();
        addChildL();
    }
    public void addChildR() {
        if (selectedNode == null) {
            return;
        }
        NodeModel parentModel = selectedNode.getModel();
        if (parentModel.getPos() != PosConstants.RIGHT && parentModel != rootModel) {
            return;
        }

        double childX = parentModel.getX() + selectedNode.getWidth() + SizeConstants.NODE_GAP_X;
        double childY;

        List<NodeModel> children = parentModel.getRightChildren();
        if (children.isEmpty()) {
            childY = parentModel.getY();
        }
        // 基于最后一个子节点的位置加上偏移
        else {
            childY = parentModel.getEndYR() + SizeConstants.NODE_GAP_Y;
        }

        NodeModel childModel = new NodeModel(nextId(), "", childX, childY, PosConstants.RIGHT);
        parentModel.addRightChild(childModel);
        addNode(childModel);

        adjustChildrenYR();
        refreshLinesR();
    }

    public void addChildL() {
        if (selectedNode == null) {
            return;
        }
        NodeModel parentModel = selectedNode.getModel();
        if (parentModel.getPos() != PosConstants.LEFT && parentModel != rootModel) {
            return;
        }

        double childX = parentModel.getX() - SizeConstants.ADD_LEFT_NODE_GAP_X;
        double childY;

        List<NodeModel> children = parentModel.getLeftChildren();
        if (children.isEmpty()) {
            childY = parentModel.getY();
        }
        // 基于最后一个子节点的位置加上偏移
        else {
            childY = parentModel.getEndYR() + SizeConstants.NODE_GAP_Y;
        }

        NodeModel childModel = new NodeModel(nextId(), "", childX, childY, PosConstants.LEFT);
        parentModel.addLeftChild(childModel);
        addNode(childModel);

        adjustChildrenYL();
        refreshLinesL();
    }

    public void addSibling() {
        if (selectedNode == null) {
            return;
        }

        NodeModel nodeModel = selectedNode.getModel();
        if (nodeModel.getPos() == PosConstants.RIGHT) {
            addSiblingR();
        } else {
            addSiblingL();
        }
    }

    public void addSiblingR() {
        NodeModel nodeModel = selectedNode.getModel();
        NodeModel parentModel = nodeModel.getParent();
        if (parentModel == null) {
            return;
        }

        // 基于当前节点位置加上偏移
        double siblingX = nodeModel.getX();
        double siblingY = nodeModel.getY() + selectedNode.getLayoutBounds().getHeight() + SizeConstants.NODE_GAP_Y;

        NodeModel siblingModel = new NodeModel(nextId(), "", siblingX, siblingY, PosConstants.RIGHT);
        parentModel.addRightChild(siblingModel);
        addNode(siblingModel);

        adjustChildrenYR();
        refreshLinesR();
    }

    public void addSiblingL() {
        NodeModel nodeModel = selectedNode.getModel();
        NodeModel parentModel = nodeModel.getParent();
        if (parentModel == null) {
            return;
        }

        // 基于当前节点位置加上偏移
        double siblingX = nodeModel.getX();
        double siblingY = nodeModel.getY() + selectedNode.getLayoutBounds().getHeight() + SizeConstants.NODE_GAP_Y;

        NodeModel siblingModel = new NodeModel(nextId(), "", siblingX, siblingY, PosConstants.LEFT);
        parentModel.addLeftChild(siblingModel);
        addNode(siblingModel);

        adjustChildrenYL();
        refreshLinesL();
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
        List<NodeModel> children = parent.getRightChildren();
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

        adjustChildrenYR();
        if (toDelete.getPos() == PosConstants.RIGHT) {
            refreshLinesR();
        } else {
            refreshLinesL();
        }
    }

    public void adjustChildrenYR() {
        adjustChildrenYR(rootModel, null);
    }

    public void adjustChildrenYL() {
        adjustChildrenYL(rootModel, null);
    }

    public void refreshLines() {
        refreshLinesR();
        refreshLinesL();
    }

    public void refreshLinesR() {
        Pane linesLayerR = subject.getLinesLayerR();
        linesLayerR.getChildren().clear();
        refreshLinesR(linesLayerR, rootModel);
    }

    public void refreshLinesL() {
        Pane linesLayerL = subject.getLinesLayerL();
        linesLayerL.getChildren().clear();
        refreshLinesL(linesLayerL, rootModel);
    }

    private void refreshLinesR(Pane linesLayer, NodeModel parentModel) {
        List<NodeModel> children = parentModel.getRightChildren();
        if (!children.isEmpty()) {
            for (NodeModel childModel : children) {
                MindNode childNode = childModel.getMindNode();
                Point2D start = getRightPoint(parentModel.getMindNode());
                Point2D end = getLeftPoint(childNode);

                Line line = new Line(start.getX(), start.getY(), end.getX(), end.getY());
                line.setStroke(Color.rgb(100, 100, 100));
                line.setStrokeWidth(2.5);
                line.setStrokeDashOffset(0);

                linesLayer.getChildren().add(line);
                refreshLinesR(linesLayer, childModel);
            }
        }
    }

    private void refreshLinesL(Pane linesLayer, NodeModel parentModel) {
        List<NodeModel> children = parentModel.getLeftChildren();
        if (!children.isEmpty()) {
            for (NodeModel childModel : children) {
                MindNode childNode = childModel.getMindNode();
                Point2D start = getLeftPoint(parentModel.getMindNode());
                Point2D end = getRightPoint(childNode);

                Line line = new Line(start.getX(), start.getY(), end.getX(), end.getY());
                line.setStroke(Color.rgb(100, 100, 100));
                line.setStrokeWidth(2.5);
                line.setStrokeDashOffset(0);

                linesLayer.getChildren().add(line);
                refreshLinesL(linesLayer, childModel);
            }
        }
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

    public void setSelectedNode(MindNode node) {
        this.selectedNode = node;
        if (node != null) {
            selectedNode.getTextArea().requestFocus();
        }
    }

    public void clearAll() {
        subject.getNodesLayer().getChildren().clear();
        subject.getLinesLayerR().getChildren().clear();
        nodeMap.clear();
        selectedNode = null;
    }

    public void rebuildViewFromModel(NodeModel node) {
        addNode(node);
        for (NodeModel child : node.getRightChildren()) {
            rebuildViewFromModel(child);
        }
    }

    //———————————————————————————————————————————私有方法———————————————————————————————————————————
    private void addNode(NodeModel model) {
        MindNode node = new MindNode(model, this);
        nodeMap.put(model.getId(), node);
        subject.getNodesLayer().getChildren().add(node);
        setSelectedNode(node);

        // 强制刷新布局，确保尺寸计算正确，否则node.getHeight()返回0
        subject.applyCss();
        subject.layout();
    }

    private void deleteChildren(NodeModel parentNodeModel) {
        for (NodeModel childNodeModel : parentNodeModel.getRightChildren()) {
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
        subject.getNodesLayer().getChildren().remove(childNode);
    }

    /**
     * 调整子节点的Y轴位置，父节点在所有子节点的中间
     */
    private void adjustChildrenYR(NodeModel parentModel, Double y) {
        List<NodeModel> children = parentModel.getRightChildren();
        if (children.isEmpty()) {
            return;
        }

        // 递归时，y 不为空，以传入的 y 为第一个子节点的Y坐标
        double childY;
        if (y == null) {
            double totalHeight = calculateTotalHeightR(children);
            double parentMidY = parentModel.getY() + parentModel.getMindNode().getPrefHeight() / 2.0;
            childY = parentMidY - totalHeight / 2.0;
        } else {
            childY = y;
        }

        // 依次调整所有子节点
        for (NodeModel childModel : children) {
            MindNode childNode = childModel.getMindNode();
            List<NodeModel> childrenOfChild = childModel.getRightChildren();

            double selfHeight = childNode.getPrefHeight();
            if (childrenOfChild.isEmpty()) {
                childModel.setY(childY);
                // 当前Y + 当前节点高度 + 间距
                childY += selfHeight + SizeConstants.NODE_GAP_Y;
            } else {
                double childrenHeight = childModel.getTotalHeightR();
                if (selfHeight < childrenHeight) {
                    adjustChildrenYR(childModel, childY);
                    childModel.setY(childModel.getMidYR() - childNode.getPrefHeight() / 2.0);
                    // 下一个子节点的Y坐标 = 最后一个子节点的底部 + 间距
                    childY = childModel.getEndYR() + SizeConstants.NODE_GAP_Y;
                }
                //当前节点的高度 > 子节点的总高度
                else {
                    childModel.setY(childY);
                    // 当前Y + 当前节点高度 + 间距
                    childY += selfHeight + SizeConstants.NODE_GAP_Y;
                    adjustChildrenYR(childModel, null);
                }
            }
        }
    }

    private void adjustChildrenYL(NodeModel parentModel, Double y) {
        List<NodeModel> children = parentModel.getLeftChildren();
        if (children.isEmpty()) {
            return;
        }

        // 递归时，y 不为空，以传入的 y 为第一个子节点的Y坐标
        double childY;
        if (y == null) {
            double totalHeight = calculateTotalHeightL(children);
            double parentMidY = parentModel.getY() + parentModel.getMindNode().getPrefHeight() / 2.0;
            childY = parentMidY - totalHeight / 2.0;
        } else {
            childY = y;
        }

        // 依次调整所有子节点
        for (NodeModel childModel : children) {
            MindNode childNode = childModel.getMindNode();
            List<NodeModel> childrenOfChild = childModel.getLeftChildren();

            double selfHeight = childNode.getPrefHeight();
            if (childrenOfChild.isEmpty()) {
                childModel.setY(childY);
                // 当前Y + 当前节点高度 + 间距
                childY += selfHeight + SizeConstants.NODE_GAP_Y;
            } else {
                double childrenHeight = childModel.getTotalHeightL();
                if (selfHeight < childrenHeight) {
                    adjustChildrenYL(childModel, childY);
                    childModel.setY(childModel.getMidYL() - childNode.getPrefHeight() / 2.0);
                    // 下一个子节点的Y坐标 = 最后一个子节点的底部 + 间距
                    childY = childModel.getEndYL() + SizeConstants.NODE_GAP_Y;
                }
                //当前节点的高度 > 子节点的总高度
                else {
                    childModel.setY(childY);
                    // 当前Y + 当前节点高度 + 间距
                    childY += selfHeight + SizeConstants.NODE_GAP_Y;
                    adjustChildrenYL(childModel, null);
                }
            }
        }
    }

    /**
     * 计算所有子孙节点的总高度，
     * 每个子节点高度 + 间距 * (子节点数量 - 1)
     */
    private double calculateTotalHeightR(List<NodeModel> children) {
        double totalHeight = 0;
        for (NodeModel child : children) {
            totalHeight += child.getTotalHeightR();
        }
        totalHeight += SizeConstants.NODE_GAP_Y * (children.size() - 1);

        return totalHeight;
    }

    private double calculateTotalHeightL(List<NodeModel> children) {
        double totalHeight = 0;
        for (NodeModel child : children) {
            totalHeight += child.getTotalHeightL();
        }
        totalHeight += SizeConstants.NODE_GAP_Y * (children.size() - 1);

        return totalHeight;
    }

    private int nextId() {
        return idGenerator.getAndIncrement();
    }
}