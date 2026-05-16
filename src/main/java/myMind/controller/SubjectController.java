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

        NodeModel childModel = new NodeModel(childX, childY, PosConstants.RIGHT);
        selectedModel.addChildR(childModel);
        addNode(childModel);

        adjustChildrenYR();
        refreshLinesR();
    }

    public void addChildL() {
        if (selectedModel == null || selectedModel.getPos() == PosConstants.RIGHT) {
            return;
        }

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

        NodeModel childModel = new NodeModel(childX, childY, PosConstants.LEFT);
        selectedModel.addChildL(childModel);
        addNode(childModel);

        adjustChildrenYL();
        refreshLinesL();
    }

    public void addSibling() {
        if (selectedModel == null) {
            return;
        }

        if (selectedModel.getPos() == PosConstants.RIGHT) {
            addSiblingR();
        } else {
            addSiblingL();
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

        if (selectedModel.getPos() == PosConstants.LEFT) {
            pasteL(copyNode);
        } else {
            pasteR(copyNode);
        }
    }

    public void pasteR(MindNode copyNode) {
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

        NodeModel cloneModel = copyNode.getModel();
        cloneModel.setX(childX);
        cloneModel.setY(childY);
        selectedModel.addChildR(cloneModel);
        addNodeWithChildrenR(copyNode);
        setSelectedModel(cloneModel);

        adjustChildrenXR(cloneModel);
        adjustChildrenYR();
        refreshLinesR();
    }

    public void pasteL(MindNode copyNode) {
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

        NodeModel cloneModel = copyNode.getModel();
        cloneModel.setX(childX);
        cloneModel.setY(childY);
        selectedModel.addChildL(cloneModel);
        addNodeWithChildrenL(copyNode);
        setSelectedModel(cloneModel);

        adjustChildrenXL(cloneModel);
        adjustChildrenYL();
        refreshLinesL();
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
        List<NodeModel> childrenR = parentModel.getChildrenR();
        for (NodeModel childModel : childrenR) {
            subject.remove(childModel.getMindNode());
            parentModel.removeChildR(childModel);

            deleteChildrenR(childModel);
        }
    }

    private void deleteChildrenL(NodeModel parentModel) {
        List<NodeModel> childrenL = parentModel.getChildrenL();
        for (NodeModel childModel : childrenL) {
            subject.remove(childModel.getMindNode());
            parentModel.removeChildL(childModel);

            deleteChildrenL(childModel);
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

        // 递归时，y 不为空，以传入的 y 为第一个子节点的Y坐标
        double childY;
        if (y == null) {
            double totalHeight = calculateTotalHeightR(children);
            double parentMidY = parentModel.getY() + parentModel.getSelfHeight() / 2.0;
            childY = parentMidY - totalHeight / 2.0;
        } else {
            childY = y;
        }

        // 依次调整所有子节点
        for (NodeModel childModel : children) {
            List<NodeModel> childrenOfChild = childModel.getChildrenR();

            double selfHeight = childModel.getSelfHeight();
            if (childrenOfChild.isEmpty()) {
                childModel.setY(childY);
                // 当前Y + 当前节点高度 + 间距
                childY += selfHeight + SizeConstants.NODE_GAP_Y;
            } else {
                double childrenHeight = childModel.getChildrenHeightR();
                if (selfHeight < childrenHeight) {
                    adjustChildrenYR(childModel, childY);
                    childModel.setY(childModel.getMidYR() - selfHeight / 2.0);
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
        List<NodeModel> children = parentModel.getChildrenL();
        if (children.isEmpty()) {
            return;
        }

        // 递归时，y 不为空，以传入的 y 为第一个子节点的Y坐标
        double childY;
        if (y == null) {
            double totalHeight = calculateTotalHeightL(children);
            double parentMidY = parentModel.getY() + parentModel.getSelfHeight() / 2.0;
            childY = parentMidY - totalHeight / 2.0;
        } else {
            childY = y;
        }

        // 依次调整所有子节点
        for (NodeModel childModel : children) {
            List<NodeModel> childrenOfChild = childModel.getChildrenL();

            double selfHeight = childModel.getSelfHeight();
            if (childrenOfChild.isEmpty()) {
                childModel.setY(childY);
                // 当前Y + 当前节点高度 + 间距
                childY += selfHeight + SizeConstants.NODE_GAP_Y;
            } else {
                double childrenHeight = childModel.getChildrenHeightL();
                if (selfHeight < childrenHeight) {
                    adjustChildrenYL(childModel, childY);
                    childModel.setY(childModel.getMidYL() - selfHeight / 2.0);
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
     * 每个 Math.max(子节点高度, 孙节点高度) + 间距 * (子节点数量 - 1)
     */
    private double calculateTotalHeightR(List<NodeModel> children) {
        double totalHeight = 0;
        for (NodeModel child : children) {
            totalHeight += Math.max(child.getSelfHeight(), child.getChildrenHeightR());
        }
        totalHeight += SizeConstants.NODE_GAP_Y * (children.size() - 1);

        return totalHeight;
    }

    private double calculateTotalHeightL(List<NodeModel> children) {
        double totalHeight = 0;
        for (NodeModel child : children) {
            totalHeight += Math.max(child.getSelfHeight(), child.getChildrenHeightL());
        }
        totalHeight += SizeConstants.NODE_GAP_Y * (children.size() - 1);

        return totalHeight;
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