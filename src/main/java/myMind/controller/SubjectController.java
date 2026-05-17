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

import java.util.Iterator;
import java.util.List;

@Data
public class SubjectController {
    private final Subject subject = new Subject(this);
    private NodeModel rootModel;
    private NodeModel selectedModel = null;

    //———————————————————————————————————————————新增———————————————————————————————————————————
    public void initRootNode(double centerX, double centerY) {
        rootModel = new NodeModel(centerX, centerY, PosConstants.MIDDLE);
        addNode(rootModel);
    }

    public void addChild() {
        addChildR();
        addChildL();
    }

    public void addChildR() {
        if (selectedModel == null || selectedModel.getPos() == PosConstants.LEFT) {
            return;
        }

        Point2D pointR = calculateChildPointR();
        NodeModel childModel = new NodeModel(pointR.getX(), pointR.getY(), PosConstants.RIGHT);
        selectedModel.addChildR(childModel);
        addNode(childModel);

        adjustChildrenYR();
        refreshLinesR();
    }

    public void addChildL() {
        if (selectedModel == null || selectedModel.getPos() == PosConstants.RIGHT) {
            return;
        }

        Point2D pointL = calculateChildPointL();
        NodeModel childModel = new NodeModel(pointL.getX(), pointL.getY(), PosConstants.LEFT);
        selectedModel.addChildL(childModel);
        addNode(childModel);

        adjustChildrenYL();
        refreshLinesL();
    }

    public void addSibling() {
        if (selectedModel == null) {
            return;
        }

        if (selectedModel.getPos() == PosConstants.LEFT) {
            addSiblingL();
        } else {
            addSiblingR();
        }
    }

    public void addSiblingR() {
        NodeModel parentModel = selectedModel.getParent();
        if (parentModel == null) {
            return;
        }

        double siblingX = selectedModel.getX();
        // 当前节点的 Y 轴 + 当前节点高度 + 节点间隔
        double siblingY = selectedModel.getY() + selectedModel.getMindNode().getPrefHeight() + SizeConstants.NODE_GAP_Y;

        NodeModel siblingModel = new NodeModel(siblingX, siblingY, PosConstants.RIGHT);
        parentModel.addChildR(siblingModel);
        addNode(siblingModel);

        adjustChildrenYR();
        refreshLinesR();
    }

    public void addSiblingL() {
        NodeModel parentModel = selectedModel.getParent();
        if (parentModel == null) {
            return;
        }

        MindNode selectedNode = selectedModel.getMindNode();
        // 父节点 X 轴 - 节点间隔 - 节点最小宽度
        double siblingX = parentModel.getX() - SizeConstants.ADD_LEFT_NODE_GAP_X;
        // 当前节点的 Y 轴 + 当前节点高度 + 节点间隔
        double siblingY = selectedModel.getY() + selectedNode.getPrefHeight() + SizeConstants.NODE_GAP_Y;

        NodeModel siblingModel = new NodeModel(siblingX, siblingY, PosConstants.LEFT);
        parentModel.addChildL(siblingModel);
        addNode(siblingModel);

        adjustChildrenYL();
        refreshLinesL();
    }

    public void addNode(NodeModel model) {
        MindNode node = new MindNode(model, this, "");
        subject.add(node);
        setSelectedModel(model);
    }

    public void addNode(MindNode node) {
        subject.add(node);
        setSelectedModel(node.getModel());
    }

    public void addNodeWithChildrenR(MindNode node) {
        subject.add(node);

        node.getModel().getChildrenR().forEach(child -> {
            addNodeWithChildrenR(child.getMindNode());
        });
    }

    public void addNodeWithChildrenL(MindNode node) {
        subject.add(node);

        node.getModel().getChildrenL().forEach(child -> {
            addNodeWithChildrenL(child.getMindNode());
        });
    }

    //———————————————————————————————————————————复制粘贴———————————————————————————————————————————
    public MindNode copy() {
        if (selectedModel == null) {
            return null;
        }

        return selectedModel.getMindNode();
    }

    public MindNode cut() {
        if (selectedModel == null || selectedModel == rootModel) {
            return null;
        }

        NodeModel model = selectedModel;
        if (selectedModel.getPos() == PosConstants.RIGHT) {
            deleteChildrenFromSubjectR(model);
            deleteR();
        } else {
            deleteChildrenFromSubjectL(model);
            deleteL();
        }

        return model.getMindNode();
    }

    public void paste(MindNode copyNode) {
        if (selectedModel == null || copyNode == null) {
            return;
        }

        NodeModel cloneModel = copyNode.getModel();
        if (selectedModel.getPos() == PosConstants.LEFT) {
            Point2D pointL = calculateChildPointL();
            cloneModel.setX(pointL.getX());
            cloneModel.setY(pointL.getY());

            if (cloneModel.getPos() == PosConstants.RIGHT) {
                cloneModel.setParent(selectedModel);
                transRToL(cloneModel);
            } else {
                selectedModel.addChildL(cloneModel);
            }
            addNodeWithChildrenL(copyNode);

            adjustChildrenXL(cloneModel);
            adjustChildrenYL();
            refreshLinesL();
        } else {
            Point2D pointR = calculateChildPointR();
            cloneModel.setX(pointR.getX());
            cloneModel.setY(pointR.getY());

            if (cloneModel.getPos() == PosConstants.LEFT) {
                cloneModel.setParent(selectedModel);
                transLToR(cloneModel);
            } else {
                selectedModel.addChildR(cloneModel);
            }
            addNodeWithChildrenR(copyNode);

            adjustChildrenXR(cloneModel);
            adjustChildrenYR();
            refreshLinesR();
        }
        setSelectedModel(cloneModel);
    }

    private void transLToR(NodeModel cloneModel) {
        // 节点改到右边
        cloneModel.setPos(PosConstants.RIGHT);
        cloneModel.getParent().addChildR(cloneModel);

        // 从左边删除
        Iterator<NodeModel> iterator = cloneModel.getChildrenL().iterator();
        while (iterator.hasNext()) {
            NodeModel child = iterator.next();
            iterator.remove();
            transLToR(child);
        }
    }

    private void transRToL(NodeModel cloneModel) {
        cloneModel.setPos(PosConstants.LEFT);
        cloneModel.getParent().addChildL(cloneModel);

        Iterator<NodeModel> iterator = cloneModel.getChildrenR().iterator();
        while (iterator.hasNext()) {
            NodeModel child = iterator.next();
            iterator.remove();
            transRToL(child);
        }
    }

    private Point2D calculateChildPointR() {
        // 父节点 X 轴 +父节点宽度 + 节点间隔
        double childX = selectedModel.getX() + selectedModel.getMindNode().getPrefWidth() + SizeConstants.NODE_GAP_X;
        double childY;
        List<NodeModel> children = selectedModel.getChildrenR();
        if (children.isEmpty()) {
            childY = selectedModel.getY();
        }
        // 最后一个子节点底部 Y 轴 + 节点间隔
        else {
            childY = selectedModel.getEndYR() + SizeConstants.NODE_GAP_Y;
        }

        return new Point2D(childX, childY);
    }

    private Point2D calculateChildPointL() {
        // 父节点 X 轴 - 节点间隔 - 节点最小宽度
        double childX = selectedModel.getX() - SizeConstants.ADD_LEFT_NODE_GAP_X;
        double childY;

        List<NodeModel> children = selectedModel.getChildrenL();
        if (children.isEmpty()) {
            childY = selectedModel.getY();
        }
        // 最后一个子节点底部 Y 轴 + 节点间隔
        else {
            childY = selectedModel.getEndYL() + SizeConstants.NODE_GAP_Y;
        }

        return new Point2D(childX, childY);
    }

    //———————————————————————————————————————————删除———————————————————————————————————————————

    /**
     * 删除节点及其子节点
     */
    public void delete() {
        if (selectedModel == null || selectedModel == rootModel) {
            return;
        }

        if (selectedModel.getPos() == PosConstants.RIGHT) {
            deleteChildrenR(selectedModel);
            deleteR();
        } else {
            deleteChildrenL(selectedModel);
            deleteL();
        }
    }

    /**
     * 删除节点
     */
    private void deleteR() {
        NodeModel parent = selectedModel.getParent();

        // 改变选中节点，记录之前选中的要删除的节点
        NodeModel toDelete = selectedModel;
        changeSelectedModel(toDelete, parent, parent.getChildrenR());

        parent.removeChildR(toDelete);
        subject.remove(toDelete.getMindNode());

        adjustChildrenYR();
        refreshLinesR();
    }

    private void deleteL() {
        NodeModel parent = selectedModel.getParent();

        NodeModel toDelete = selectedModel;
        changeSelectedModel(toDelete, parent, parent.getChildrenL());

        parent.removeChildL(toDelete);
        subject.remove(toDelete.getMindNode());

        adjustChildrenYL();
        refreshLinesL();
    }

    /**
     * 从 subject 和父节点的子节点数组中删除子节点
     */
    private void deleteChildrenR(NodeModel parentModel) {
        Iterator<NodeModel> iterator = parentModel.getChildrenR().iterator();
        while (iterator.hasNext()) {
            NodeModel childModel = iterator.next();
            subject.remove(childModel.getMindNode());
            iterator.remove();
            childModel.setParent(null);

            deleteChildrenR(childModel);
        }
    }

    private void deleteChildrenL(NodeModel parentModel) {
        Iterator<NodeModel> iterator = parentModel.getChildrenL().iterator();
        while (iterator.hasNext()) {
            NodeModel childModel = iterator.next();
            subject.remove(childModel.getMindNode());
            iterator.remove();
            childModel.setParent(null);

            deleteChildrenR(childModel);
        }
    }

    /**
     * 仅从 subject 删除子节点
     */
    private void deleteChildrenFromSubjectR(NodeModel parentModel) {
        List<NodeModel> childrenR = parentModel.getChildrenR();
        for (NodeModel childModel : childrenR) {
            subject.remove(childModel.getMindNode());

            deleteChildrenFromSubjectR(childModel);
        }
    }

    private void deleteChildrenFromSubjectL(NodeModel parentModel) {
        List<NodeModel> childrenL = parentModel.getChildrenL();
        for (NodeModel childModel : childrenL) {
            subject.remove(childModel.getMindNode());

            deleteChildrenFromSubjectL(childModel);
        }
    }

    /**
     * 改变选中节点
     */
    private void changeSelectedModel(NodeModel toDelete, NodeModel parent, List<NodeModel> children) {
        if (children.size() == 1) {
            setSelectedModel(parent);
        } else {
            int index = children.indexOf(toDelete);
            if (index != children.size() - 1) {
                setSelectedModel(children.get(index + 1));
            } else {
                setSelectedModel(children.get(index - 1));
            }
        }
    }

    //———————————————————————————————————————————调整———————————————————————————————————————————
    // 调整子节点X坐标
    public void adjustChildrenX() {
        adjustChildrenXR(rootModel);
        adjustChildrenXL(rootModel);
    }

    public void adjustChildrenXR(NodeModel parentModel) {
        List<NodeModel> children = parentModel.getChildrenR();
        if (children.isEmpty()) {
            return;
        }

        // 父节点的 X 坐标 + 父节点的宽度 + 节点间隔
        double childX = parentModel.getX() + parentModel.getSelfWidth() + SizeConstants.NODE_GAP_X;
        for (NodeModel child : children) {
            child.setX(childX);
            adjustChildrenXR(child);
        }
    }

    public void adjustChildrenXL(NodeModel parentModel) {
        List<NodeModel> children = parentModel.getChildrenL();
        if (children.isEmpty()) {
            return;
        }

        // 父节点的 X 坐标 - 节点间隔 - 子节点的宽度
        double childX = parentModel.getX() - SizeConstants.NODE_GAP_X;
        for (NodeModel child : children) {
            child.setX(childX - child.getSelfWidth());
            adjustChildrenXL(child);
        }
    }

    public void adjustChildrenY() {
        adjustChildrenYR(rootModel, null);
        adjustChildrenYL(rootModel, null);
    }

    // 调整子节点Y坐标
    public void adjustChildrenYR() {
        adjustChildrenYR(rootModel, null);
    }

    public void adjustChildrenYL() {
        adjustChildrenYL(rootModel, null);
    }

    /**
     * 子节点以父节点为中心，依次排列
     */
    private void adjustChildrenYR(NodeModel parentModel, Double y) {
        List<NodeModel> children = parentModel.getChildrenR();
        if (children.isEmpty()) {
            return;
        }

        // 递归时，y 不为空，以传入的 y 为第一个子节点的 Y 坐标
        double childY;
        if (y == null) {
            double totalHeight = parentModel.getChildrenHeightR();
            double parentMidY = parentModel.getY() + parentModel.getSelfHeight() / 2.0;
            childY = parentMidY - totalHeight / 2.0;
        } else {
            childY = y;
        }

        for (NodeModel childModel : children) {
            List<NodeModel> childrenOfChild = childModel.getChildrenR();

            double selfHeight = childModel.getSelfHeight();
            if (childrenOfChild.isEmpty()) {
                childModel.setY(childY);
                // 当前Y + 当前节点高度 + 间距
                childY += selfHeight + SizeConstants.NODE_GAP_Y;
            } else {
                // 当前节点的高度 < 子节点的总高度
                if (selfHeight < childModel.getChildrenHeightR()) {
                    // 先调整子节点们的位置，再让当前节点在子节点的中间
                    adjustChildrenYR(childModel, childY);
                    childModel.setY((childModel.getStartYR() + childModel.getEndYR() - selfHeight) / 2.0);
                    // 最后一个子节点的底部 + 间距
                    childY = childModel.getEndYR() + SizeConstants.NODE_GAP_Y;
                } else {
                    childModel.setY(childY);
                    // 当前Y + 当前节点高度 + 间距
                    childY += selfHeight + SizeConstants.NODE_GAP_Y;
                    adjustChildrenYR(childModel, null);
                }
            }

        }
    }

    private void adjustChildrenYL(NodeModel parentModel, Double y) {
        List<NodeModel> children = parentModel.getChildrenL();
        if (children.isEmpty()) {
            return;
        }

        double childY;
        if (y == null) {
            double totalHeight = parentModel.getChildrenHeightL();
            double parentMidY = parentModel.getY() + parentModel.getSelfHeight() / 2.0;
            childY = parentMidY - totalHeight / 2.0;
        } else {
            childY = y;
        }

        for (NodeModel childModel : children) {
            List<NodeModel> childrenOfChild = childModel.getChildrenL();

            double selfHeight = childModel.getSelfHeight();
            if (childrenOfChild.isEmpty()) {
                childModel.setY(childY);
                childY += selfHeight + SizeConstants.NODE_GAP_Y;
            } else {
                if (selfHeight < childModel.getChildrenHeightL()) {
                    adjustChildrenYL(childModel, childY);
                    childModel.setY((childModel.getStartYL() + childModel.getEndYL() - selfHeight) / 2.0);
                    childY = childModel.getEndYL() + SizeConstants.NODE_GAP_Y;
                } else {
                    childModel.setY(childY);
                    childY += selfHeight + SizeConstants.NODE_GAP_Y;
                    adjustChildrenYL(childModel, null);
                }
            }
        }
    }

    public void adjustChildrenSize() {
        adjustChildrenSizeR(rootModel);
        adjustChildrenSizeL(rootModel);
    }

    public void adjustChildrenSizeR(NodeModel nodeModel) {
        for (NodeModel childModel : nodeModel.getChildrenR()) {
            childModel.getMindNode().adjustSize();

            List<NodeModel> childrenOfChild = childModel.getChildrenR();
            if (!childrenOfChild.isEmpty()) {
                adjustChildrenSizeR(childModel);
            }
        }
    }

    public void adjustChildrenSizeL(NodeModel nodeModel) {
        for (NodeModel childModel : nodeModel.getChildrenL()) {
            childModel.getMindNode().adjustSize();

            List<NodeModel> childrenOfChild = childModel.getChildrenL();
            if (!childrenOfChild.isEmpty()) {
                adjustChildrenSizeL(childModel);
            }
        }
    }

    //———————————————————————————————————————————刷新连线———————————————————————————————————————————
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
        List<NodeModel> children = parentModel.getChildrenR();
        if (!children.isEmpty()) {
            for (NodeModel childModel : children) {
                MindNode childNode = childModel.getMindNode();
                Point2D start = getPointR(parentModel.getMindNode());
                Point2D end = getPointL(childNode);

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
        List<NodeModel> children = parentModel.getChildrenL();
        if (!children.isEmpty()) {
            for (NodeModel childModel : children) {
                MindNode childNode = childModel.getMindNode();
                Point2D start = getPointL(parentModel.getMindNode());
                Point2D end = getPointR(childNode);

                Line line = new Line(start.getX(), start.getY(), end.getX(), end.getY());
                line.setStroke(Color.rgb(100, 100, 100));
                line.setStrokeWidth(2.5);
                line.setStrokeDashOffset(0);

                linesLayer.getChildren().add(line);
                refreshLinesL(linesLayer, childModel);
            }
        }
    }

    private Point2D getPointR(MindNode node) {
        double x = node.getLayoutX() + node.getPrefWidth();
        double y = node.getLayoutY() + node.getPrefHeight() / 2;
        return new Point2D(x, y);
    }

    private Point2D getPointL(MindNode node) {
        double x = node.getLayoutX();
        double y = node.getLayoutY() + node.getPrefHeight() / 2;
        return new Point2D(x, y);
    }

    //———————————————————————————————————————————其他———————————————————————————————————————————
    public void setSelectedModel(NodeModel nodeModel) {
        this.selectedModel = nodeModel;
        if (nodeModel != null) {
            selectedModel.getMindNode().getTextArea().requestFocus();
        } else {
            subject.requestFocus();
        }
    }
}