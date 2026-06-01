package myMind.model;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import myMind.constants.SizeConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@EqualsAndHashCode(of = "id")
@ToString(of = "id")
public class NodeModel {
    private final String id = UUID.randomUUID().toString();
    // 属性
    private final DoubleProperty x = new SimpleDoubleProperty();
    private final DoubleProperty y = new SimpleDoubleProperty();
    private DoubleProperty nodeWidth = new SimpleDoubleProperty();
    private DoubleProperty nodeHeight = new SimpleDoubleProperty();

    //节点之间的关系
    private byte pos;
    private NodeModel parent;
    private final List<NodeModel> childrenR = new ArrayList<>();
    private final List<NodeModel> childrenL = new ArrayList<>();

    public NodeModel(double x, double y, byte pos) {
        this.x.set(x);
        this.y.set(y);
        this.pos = pos;
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

    public void addChildAtR(int index, NodeModel child) {
        childrenR.add(index, child);
        child.setParent(this);
    }

    public void addChildAtL(int index, NodeModel child) {
        childrenL.add(index, child);
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
     * 子节点的总高度
     */
    public double getChildrenHeightR() {
        double totalHeight = 0;
        for (NodeModel child : childrenR) {
            totalHeight += child.getHeightR();
        }
        totalHeight += SizeConstants.NODE_GAP_Y * (childrenR.size() - 1);
        return totalHeight;
    }

    public double getChildrenHeightL() {
        double totalHeight = 0;
        for (NodeModel child : childrenL) {
            totalHeight += child.getHeightL();
        }
        totalHeight += SizeConstants.NODE_GAP_Y * (childrenL.size() - 1);
        return totalHeight;
    }

    /**
     * 节点的高度
     *
     * @return Math.max(当前节点的高度, 子节点的总高度)
     */
    private double getHeightR() {
        if (childrenR.isEmpty()) {
            return getNodeHeight();
        }
        return Math.max(getNodeHeight(), getChildrenHeightR());
    }

    private double getHeightL() {
        if (childrenL.isEmpty()) {
            return getNodeHeight();
        }
        return Math.max(getNodeHeight(), getChildrenHeightL());
    }

    //———————————————————————————————————————————位置计算———————————————————————————————————————————
    public double getStartYR() {
        NodeModel fistNodeModel = childrenR.get(0);
        if (!fistNodeModel.childrenR.isEmpty()) {
            // 当前节点可能比子节节点的总高度更高
            return Math.min(fistNodeModel.getY(), fistNodeModel.getStartYR());
        } else {
            return fistNodeModel.getY();
        }
    }

    public double getStartYL() {
        NodeModel fistNodeModel = childrenL.get(0);
        if (!fistNodeModel.childrenL.isEmpty()) {
            return Math.min(fistNodeModel.getY(), fistNodeModel.getStartYL());
        } else {
            return fistNodeModel.getY();
        }
    }

    public double getEndYR() {
        NodeModel lastNodeModel = childrenR.get(childrenR.size() - 1);
        double selfEndY = lastNodeModel.getY() + lastNodeModel.getNodeHeight();
        if (!lastNodeModel.childrenR.isEmpty()) {
            return Math.max(selfEndY, lastNodeModel.getEndYR());
        } else {
            return selfEndY;
        }
    }

    public double getEndYL() {
        NodeModel lastNodeModel = childrenL.get(childrenL.size() - 1);
        double selfEndY = lastNodeModel.getY() + lastNodeModel.getNodeHeight();
        if (!lastNodeModel.childrenL.isEmpty()) {
            return Math.max(selfEndY, lastNodeModel.getEndYL());
        } else {
            return selfEndY;
        }
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

    public double getNodeWidth() {
        return nodeWidth.get();
    }

    public void setNodeWidth(double nodeWidth) {
        this.nodeWidth.set(nodeWidth);
    }

    public DoubleProperty nodeWidthProperty() {
        return nodeWidth;
    }

    public double getNodeHeight() {
        return nodeHeight.get();
    }

    public void setNodeHeight(double nodeHeight) {
        this.nodeHeight.set(nodeHeight);
    }

    public DoubleProperty nodeHeightProperty() {
        return nodeHeight;
    }
}