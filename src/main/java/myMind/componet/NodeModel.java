package myMind.componet;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Data
public class NodeModel {
    //节点内部属性
    private final int id;
    private MindNode mindNode;
    private final StringProperty text = new SimpleStringProperty();
    private final DoubleProperty x = new SimpleDoubleProperty();
    private final DoubleProperty y = new SimpleDoubleProperty();

    //节点之间的关系
    private NodeModel parent;
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

    public void addChild(NodeModel child) {
        children.add(child);
        child.setParent(this);
    }

    public void removeChildren() {
        for (int i = 0; i < children.size(); i++) {
            NodeModel remove = children.remove(i);
            remove.setParent(null);
        }
    }

    public void removeChild(NodeModel child) {
        children.remove(child);
        child.setParent(null);
    }

    /**
     * 获取所有子节点的中间位置
     */
    public double getMidY() {
        return (getStartY() + getEndY()) / 2.0;
    }

    public double getTotalHeight() {
        if (children.isEmpty()) {
            return getMindNode().getHeight();
        } else {
            return getEndY() - getStartY();
        }
    }

    public double getEndY() {
        NodeModel lastNodeModel = children.get(children.size() - 1);
        if (!lastNodeModel.children.isEmpty()) {
            return lastNodeModel.getEndY();
        } else {
            return lastNodeModel.getY() + lastNodeModel.getMindNode().getHeight();
        }
    }

    //———————————————————————————————————————————私有方法———————————————————————————————————————————
    private double getStartY() {
        NodeModel fistNodeModel = children.get(0);
        if (!fistNodeModel.children.isEmpty()) {
            return fistNodeModel.getStartY();
        } else {
            return fistNodeModel.getY();
        }
    }
}