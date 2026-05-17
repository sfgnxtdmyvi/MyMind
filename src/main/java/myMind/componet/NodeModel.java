package myMind.componet;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import lombok.Data;
import myMind.constants.SizeConstants;

import java.util.ArrayList;
import java.util.List;

@Data
public class NodeModel {
    //节点内部属性
    private MindNode mindNode;
    private byte pos;
    private final DoubleProperty x = new SimpleDoubleProperty();
    private final DoubleProperty y = new SimpleDoubleProperty();

    //节点之间的关系
    private NodeModel parent;
    private final List<NodeModel> childrenR = new ArrayList<>();
    private final List<NodeModel> childrenL = new ArrayList<>();

    public NodeModel(double x, double y, byte pos) {
        this.x.set(x);
        this.y.set(y);
        this.pos = pos;
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

    //———————————————————————————————————————————增删———————————————————————————————————————————
    public void addChildR(NodeModel child) {
        childrenR.add(child);
        child.setParent(this);
    }

    public void addChildL(NodeModel child) {
        childrenL.add(child);
        child.setParent(this);
    }

    public void removeChildR(NodeModel child) {
        childrenR.remove(child);
        child.setParent(null);
    }

    public void removeChildL(NodeModel child) {
        childrenL.remove(child);
        child.setParent(null);
    }

    //———————————————————————————————————————————宽高计算———————————————————————————————————————————
    /**
     * 计算子节点的总高度
     */
    public double getChildrenHeightR() {
        double totalHeight = 0;
        for (NodeModel child :childrenR) {
            totalHeight += child.getHeightR();
        }
        totalHeight += SizeConstants.NODE_GAP_Y * (childrenR.size() - 1);
        return totalHeight;
    }

    public double getChildrenHeightL() {
        double totalHeight = 0;
        for (NodeModel child :childrenL) {
            totalHeight += child.getHeightL();
        }
        totalHeight += SizeConstants.NODE_GAP_Y * (childrenL.size() - 1);
        return totalHeight;
    }

    /**
     * 计算节点的高度
     * @return  Math.max(当前节点的高度, 子节点的总高度)
     */
    private double getHeightR() {
        int size = childrenR.size();
        if (size == 0) {
            return getSelfHeight();
        }

        double totalHeight = 0;
        for (NodeModel child : childrenR) {
            totalHeight += child.getHeightR();
        }
        totalHeight += SizeConstants.NODE_GAP_Y * (size - 1);
        return Math.max(getSelfHeight(), totalHeight);
    }

    private double getHeightL() {
        int size = childrenL.size();
        if (size == 0) {
            return getSelfHeight();
        }

        double totalHeight = 0;
        for (NodeModel child : childrenL) {
            totalHeight += child.getHeightL();
        }
        totalHeight += SizeConstants.NODE_GAP_Y * (size - 1);
        return Math.max(getSelfHeight(), totalHeight);
    }

    public double getSelfHeight() {
        return mindNode.getPrefHeight();
    }

    public double getSelfWidth() {
        return mindNode.getPrefWidth();
    }

    //———————————————————————————————————————————位置计算———————————————————————————————————————————
    public double getStartYR() {
        NodeModel fistNodeModel = childrenR.get(0);
        if (!fistNodeModel.childrenR.isEmpty()) {
            return fistNodeModel.getStartYR();
        } else {
            return fistNodeModel.getY();
        }
    }

    public double getStartYL() {
        NodeModel fistNodeModel = childrenL.get(0);
        if (!fistNodeModel.childrenL.isEmpty()) {
            return fistNodeModel.getStartYL();
        } else {
            return fistNodeModel.getY();
        }
    }

    public double getEndYR() {
        NodeModel lastNodeModel = childrenR.get(childrenR.size() - 1);
        if (!lastNodeModel.childrenR.isEmpty()) {
            return lastNodeModel.getEndYR();
        } else {
            return lastNodeModel.getY() + lastNodeModel.getSelfHeight();
        }
    }

    public double getEndYL() {
        NodeModel lastNodeModel = childrenL.get(childrenL.size() - 1);
        if (!lastNodeModel.childrenL.isEmpty()) {
            return lastNodeModel.getEndYL();
        } else {
            return lastNodeModel.getY() + lastNodeModel.getSelfHeight();
        }
    }
}