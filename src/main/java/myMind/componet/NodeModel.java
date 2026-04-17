package myMind.componet;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class NodeModel {
    //节点内部属性
    private final int id;
    private MindNode mindNode;
    private byte pos;
    private final StringProperty text = new SimpleStringProperty();
    private final DoubleProperty x = new SimpleDoubleProperty();
    private final DoubleProperty y = new SimpleDoubleProperty();

    //节点之间的关系
    private NodeModel parent;
    private final List<NodeModel> rightChildren = new ArrayList<>();
    private final List<NodeModel> leftChildren = new ArrayList<>();

    public NodeModel(int id, String text, double x, double y, byte pos) {
        this.id = id;
        this.text.set(text);
        this.x.set(x);
        this.y.set(y);
        this.pos = pos;
    }

    public String getText() {
        return text.get();
    }

    public void setText(String text) {
        this.text.set(text);
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

    public void addRightChild(NodeModel child) {
        rightChildren.add(child);
        child.setParent(this);
    }

    public void addLeftChild(NodeModel child) {
        leftChildren.add(child);
        child.setParent(this);
    }

    public void removeChildren() {
        for (int i = 0; i < rightChildren.size(); i++) {
            NodeModel remove = rightChildren.remove(i);
            remove.setParent(null);
        }
    }

    public void removeChild(NodeModel child) {
        rightChildren.remove(child);
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
        if (rightChildren.isEmpty()) {
            return 0;
        }
        return getEndYR() - getStartYR();
    }

    public double getChildrenHeightL() {
        if (leftChildren.isEmpty()) {
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
        NodeModel lastNodeModel = rightChildren.get(rightChildren.size() - 1);
        if (!lastNodeModel.rightChildren.isEmpty()) {
            return lastNodeModel.getEndYR();
        } else {
            return lastNodeModel.getY() + lastNodeModel.getMindNode().getHeight();
        }
    }

    public double getEndYL() {
        NodeModel lastNodeModel = leftChildren.get(leftChildren.size() - 1);
        if (!lastNodeModel.leftChildren.isEmpty()) {
            return lastNodeModel.getEndYL();
        } else {
            return lastNodeModel.getY() + lastNodeModel.getMindNode().getHeight();
        }
    }

    //———————————————————————————————————————————私有方法———————————————————————————————————————————
    private double getStartYR() {
        NodeModel fistNodeModel = rightChildren.get(0);
        if (!fistNodeModel.rightChildren.isEmpty()) {
            return fistNodeModel.getStartYR();
        } else {
            return fistNodeModel.getY();
        }
    }

    private double getStartYL() {
        NodeModel fistNodeModel = leftChildren.get(0);
        if (!fistNodeModel.leftChildren.isEmpty()) {
            return fistNodeModel.getStartYL();
        } else {
            return fistNodeModel.getY();
        }
    }
}