package myMind.componet;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import lombok.Data;

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

    /**
     * 获取所有子节点的中间位置
     */
    public double getMidYR() {
        return (getStartYR() + getEndYR()) / 2.0;
    }

    public double getMidYL() {
        return (getStartYL() + getEndYL()) / 2.0;
    }

    public double getChildrenHeightR() {
        if (childrenR.isEmpty()) {
            return 0;
        }
        return getEndYR() - getStartYR();
    }

    public double getChildrenHeightL() {
        if (childrenL.isEmpty()) {
            return 0;
        }
        return getEndYL() - getStartYL();
    }

    public double getSelfHeight() {
        return mindNode.getPrefHeight();
    }

    public double getSelfWidth() {
        return mindNode.getPrefWidth();
    }

    public double getEndYR() {
        NodeModel lastNodeModel = childrenR.get(childrenR.size() - 1);
        if (!lastNodeModel.childrenR.isEmpty()) {
            return lastNodeModel.getEndYR();
        } else {
            return lastNodeModel.getY() + lastNodeModel.getMindNode().getPrefHeight();
        }
    }

    public double getEndYL() {
        NodeModel lastNodeModel = childrenL.get(childrenL.size() - 1);
        if (!lastNodeModel.childrenL.isEmpty()) {
            return lastNodeModel.getEndYL();
        } else {
            return lastNodeModel.getY() + lastNodeModel.getMindNode().getPrefHeight();
        }
    }

    //———————————————————————————————————————————私有方法———————————————————————————————————————————
    private double getStartYR() {
        NodeModel fistNodeModel = childrenR.get(0);
        if (!fistNodeModel.childrenR.isEmpty()) {
            return fistNodeModel.getStartYR();
        } else {
            return fistNodeModel.getY();
        }
    }

    private double getStartYL() {
        NodeModel fistNodeModel = childrenL.get(0);
        if (!fistNodeModel.childrenL.isEmpty()) {
            return fistNodeModel.getStartYL();
        } else {
            return fistNodeModel.getY();
        }
    }
}