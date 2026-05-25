package myMind.controller;

import javafx.geometry.Point2D;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.QuadCurve;
import lombok.Data;
import myMind.componet.MindNode;
import myMind.componet.NodeModel;
import myMind.componet.Subject;
import myMind.constants.PosConstants;
import myMind.constants.SizeConstants;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Data
public class SubjectController {
    private final Subject subject = new Subject(this);
    private NodeModel rootModel;
    private NodeModel selectedModel;
    private MindNode cloneNode;
    private Map<NodeModel, MindNode> cloneMap = new HashMap<>();

    //———————————————————————————————————————————新增———————————————————————————————————————————
    public void initRootNode(double centerX, double centerY) {
        rootModel = new NodeModel(centerX, centerY, PosConstants.MIDDLE);
        addNode(rootModel);
    }

    public void addChild() {
        if (selectedModel == null) {
            return;
        }

        if (selectedModel.getPos() == PosConstants.LEFT) {
            NodeModel childModel = new NodeModel(calculateChildXL(selectedModel), 0, PosConstants.LEFT);
            selectedModel.addChildL(childModel);
            addNode(childModel);

            adjustChildrenYL();
            refreshLinesL();
        } else {
            NodeModel childModel = new NodeModel(calculateChildXR(selectedModel), 0, PosConstants.RIGHT);
            selectedModel.addChildR(childModel);
            addNode(childModel);

            adjustChildrenYR();
            refreshLinesR();
        }
    }

    public void addChildR() {
        if (selectedModel == null || selectedModel.getPos() == PosConstants.LEFT) {
            return;
        }

        NodeModel childModel = new NodeModel(calculateChildXR(selectedModel), 0, PosConstants.RIGHT);
        selectedModel.addChildR(childModel);
        addNode(childModel);

        adjustChildrenYR();
        refreshLinesR();
    }

    public void addChildL() {
        if (selectedModel == null || selectedModel.getPos() == PosConstants.RIGHT) {
            return;
        }

        NodeModel childModel = new NodeModel(calculateChildXL(selectedModel), 0, PosConstants.LEFT);
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
        // 根节点无法添加兄弟节点
        NodeModel parentModel = selectedModel.getParent();
        if (parentModel == null) {
            return;
        }

        NodeModel siblingModel = new NodeModel(selectedModel.getX(), 0, PosConstants.RIGHT);
        parentModel.addChildAtR(parentModel.getChildrenR().indexOf(selectedModel) + 1, siblingModel);
        addNode(siblingModel);

        adjustChildrenYR();
        refreshLinesR();
    }

    public void addSiblingL() {
        NodeModel parentModel = selectedModel.getParent();
        if (parentModel == null) {
            return;
        }

        // 父节点 X 轴 - 节点间隔 - 节点最小宽度
        NodeModel siblingModel = new NodeModel(parentModel.getX() - SizeConstants.ADD_LEFT_NODE_GAP_X, 0, PosConstants.LEFT);
        parentModel.addChildAtL(parentModel.getChildrenL().indexOf(selectedModel) + 1, siblingModel);
        addNode(siblingModel);

        adjustChildrenYL();
        refreshLinesL();
    }

    public void addNode(NodeModel model) {
        MindNode node = new MindNode(model);
        subject.addNode(node);
        setSelectedModel(model);
        setOnAction(node, model);
    }

    public void addNode(MindNode node) {
        subject.addNode(node);
        NodeModel model = node.getModel();
        setSelectedModel(model);
        setOnAction(node, model);
    }

    private void setOnAction(MindNode node, NodeModel model) {
        node.setOnAction(event -> {
            switch (event) {
                case SELECT -> setSelectedModel(model);

                case PASTE_SIBLING -> {
                    if (cloneNode != null) {
                        pasteSibling(cloneNode, model.getPos());
                        cloneNode = null;
                        cloneMap.clear();
                    }
                }
                case ADD_BUTTON_R -> {
                    if (cloneNode == null) {
                        addChildR();
                    } else {
                        pasteChild(cloneNode, PosConstants.RIGHT);
                        cloneNode = null;
                        cloneMap.clear();
                    }
                }
                case ADD_BUTTON_L -> {
                    if (cloneNode == null) {
                        addChildL();
                    } else {
                        pasteChild(cloneNode, PosConstants.LEFT);
                        cloneNode = null;
                        cloneMap.clear();
                    }
                }

                case ADJUST_R -> adjustR(model);
                case ADJUST_L -> adjustL(model);
                case ADJUST_YR -> {
                    adjustChildrenYR();
                    refreshLinesR();
                }
                case ADJUST_YL -> {
                    adjustChildrenYL();
                    refreshLinesL();
                }
            }
        });
    }

    private double calculateChildXR(NodeModel parentModel) {
        // 父节点 X 轴 +父节点宽度 + 节点间隔
        return parentModel.getX() + parentModel.getNodeWidth() + SizeConstants.NODE_GAP_X;
    }

    private double calculateChildXL(NodeModel parentModel) {
        // 父节点 X 轴 - 节点间隔 - 节点最小宽度
        return parentModel.getX() - SizeConstants.ADD_LEFT_NODE_GAP_X;
    }

    //———————————————————————————————————————————复制粘贴———————————————————————————————————————————
    public void copy() {
        cloneNode = clone(selectedModel);
    }

    private MindNode clone(NodeModel originalModel) {
        byte pos = originalModel.getPos();
        NodeModel cloneModel = new NodeModel(
                0,
                0,
                pos
        );

        MindNode originalNode = getNode(originalModel);
        MindNode cloneNode = new MindNode(cloneModel);
        setOnAction(cloneNode, cloneModel);
        cloneModel.setNodeWidth(originalModel.getNodeWidth());
        cloneModel.setNodeHeight(originalModel.getNodeHeight());
        String imageName = originalNode.getImageName();
        ImageView image = originalNode.getImage();

        if (imageName != null) {
            cloneNode.loadImage(imageName, image.getFitWidth(), image.getFitHeight());
        }
        originalNode.copyStyles(cloneNode, originalNode);
        // 不能在复制时，直接添加到 subject 中，如果后面没有粘贴，会导致内存泄漏
        cloneMap.put(cloneModel, cloneNode);

        if (pos == PosConstants.LEFT) {
            for (NodeModel childModel : originalModel.getChildrenL()) {
                NodeModel clone = clone(childModel).getModel();
                cloneModel.addChildL(clone);
                clone.setParent(cloneModel);
            }
        } else {
            for (NodeModel childModel : originalModel.getChildrenR()) {
                NodeModel clone = clone(childModel).getModel();
                cloneModel.addChildR(clone);
                clone.setParent(cloneModel);
            }
        }

        // 复制根节点时，把左子节点都添加到右边
        if (pos == PosConstants.MIDDLE) {
            cloneModel.setPos(PosConstants.RIGHT);
            for (NodeModel childModel : originalModel.getChildrenL()) {
                NodeModel clone = clone(childModel).getModel();
                cloneModel.addChildR(clone);
                clone.setParent(cloneModel);
            }
        }

        return cloneNode;
    }

    public void cut() {
        if (selectedModel == null) {
            return;
        }

        // 根节点改成复制
        if (selectedModel == rootModel) {
            cloneNode = clone(selectedModel);
        }

        NodeModel model = selectedModel;
        if (selectedModel.getPos() == PosConstants.RIGHT) {
            deleteChildrenFromSubjectR(model);
            deleteR();
        } else {
            deleteChildrenFromSubjectL(model);
            deleteL();
        }

        cloneNode = getNode(model);
    }

    /**
     *
     * @param cloneNode
     * @param pos       粘到目标的左边还是右边，跟 cloneNode 的 pos 可以不一致
     */
    private void pasteChild(MindNode cloneNode, byte pos) {
        if (selectedModel == null || cloneNode == null) {
            return;
        }

        NodeModel cloneModel = cloneNode.getModel();
        if (pos == PosConstants.RIGHT) {
            cloneModel.setX(calculateChildXR(selectedModel));

            if (cloneModel.getPos() == PosConstants.LEFT) {
                cloneModel.setParent(selectedModel);
                transLToR(cloneModel);
            } else {
                selectedModel.addChildR(cloneModel);
            }
            subject.addClone(cloneMap);

            adjustL(cloneModel);
        } else {
            cloneModel.setX(calculateChildXL(selectedModel));

            // selectedModel 与 cloneModel 的 pos 不一致时，需要移动
            if (cloneModel.getPos() == PosConstants.RIGHT) {
                cloneModel.setParent(selectedModel);
                transRToL(cloneModel);
            } else {
                selectedModel.addChildL(cloneModel);
            }
            subject.addClone(cloneMap);

            adjustR(cloneModel);
        }

        setSelectedModel(cloneModel);
    }

    /**
     * 粘贴到选中节点的上面
     *
     */
    public void pasteSibling(MindNode cloneNode, byte pos) {
        if (selectedModel == null || cloneNode == null || selectedModel == rootModel) {
            return;
        }

        NodeModel parentModel = selectedModel.getParent();
        NodeModel cloneModel = cloneNode.getModel();

        if (pos == PosConstants.RIGHT) {
            int index = parentModel.getChildrenR().indexOf(selectedModel);
            cloneModel.setX(calculateChildXR(parentModel));

            if (cloneModel.getPos() == PosConstants.LEFT) {
                cloneModel.setParent(parentModel);
                transLToRAt(index, cloneModel);
            } else {
                parentModel.addChildAtR(index, cloneModel);
            }
            subject.addClone(cloneMap);

            adjustL(cloneModel);
        } else {
            int index = parentModel.getChildrenL().indexOf(selectedModel);
            cloneModel.setX(calculateChildXL(parentModel));

            if (cloneModel.getPos() == PosConstants.RIGHT) {
                cloneModel.setParent(parentModel);
                transRToLAt(index, cloneModel);
            } else {
                parentModel.addChildAtL(index, cloneModel);
            }
            subject.addClone(cloneMap);

            adjustR(cloneModel);
        }

        setSelectedModel(cloneModel);
    }

    /**
     * 节点改到右边，再从从左边删除
     */
    private void transLToR(NodeModel cloneModel) {
        cloneModel.setPos(PosConstants.RIGHT);
        cloneModel.getParent().addChildR(cloneModel);

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

    private void transLToRAt(int index, NodeModel cloneModel) {
        // 父节点粘到选中节点的上面，子节点继续跟着父节点
        cloneModel.setPos(PosConstants.RIGHT);
        if (index != -1) {
            cloneModel.getParent().addChildAtR(index, cloneModel);
        } else {
            cloneModel.getParent().addChildR(cloneModel);
        }

        Iterator<NodeModel> iterator = cloneModel.getChildrenL().iterator();
        while (iterator.hasNext()) {
            NodeModel child = iterator.next();
            iterator.remove();
            transLToRAt(-1, child);
        }
    }

    private void transRToLAt(int index, NodeModel cloneModel) {
        cloneModel.setPos(PosConstants.LEFT);
        if (index != -1) {
            cloneModel.getParent().addChildAtL(index, cloneModel);
        } else {
            cloneModel.getParent().addChildL(cloneModel);
        }

        Iterator<NodeModel> iterator = cloneModel.getChildrenR().iterator();
        while (iterator.hasNext()) {
            NodeModel child = iterator.next();
            iterator.remove();
            transRToLAt(-1, child);
        }
    }

    /**
     * 仅从 subject 删除子节点
     */
    private void deleteChildrenFromSubjectR(NodeModel parentModel) {
        List<NodeModel> childrenR = parentModel.getChildrenR();
        for (NodeModel childModel : childrenR) {
            subject.remove(childModel);

            deleteChildrenFromSubjectR(childModel);
        }
    }

    private void deleteChildrenFromSubjectL(NodeModel parentModel) {
        List<NodeModel> childrenL = parentModel.getChildrenL();
        for (NodeModel childModel : childrenL) {
            subject.remove(childModel);

            deleteChildrenFromSubjectL(childModel);
        }
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
        subject.remove(toDelete);

        adjustChildrenYR();
        refreshLinesR();
    }

    private void deleteL() {
        NodeModel parent = selectedModel.getParent();

        NodeModel toDelete = selectedModel;
        changeSelectedModel(toDelete, parent, parent.getChildrenL());

        parent.removeChildL(toDelete);
        subject.remove(toDelete);

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
            subject.remove(childModel);
            iterator.remove();
            childModel.setParent(null);

            deleteChildrenR(childModel);
        }
    }


    private void deleteChildrenL(NodeModel parentModel) {
        Iterator<NodeModel> iterator = parentModel.getChildrenL().iterator();
        while (iterator.hasNext()) {
            NodeModel childModel = iterator.next();
            subject.remove(childModel);
            iterator.remove();
            childModel.setParent(null);

            deleteChildrenL(childModel);
        }
    }

    /**
     * 改变选中节点
     * 有下一个节点就改成下一个节点，没有就改成上一个
     * 没有兄弟节点，就改成父节点
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
    public void adjustXY() {
        adjustR(rootModel);
        adjustL(rootModel);
    }

    private void adjustR(NodeModel model) {
        adjustChildrenXL(model);
        adjustChildrenYL();
        refreshLinesL();
    }

    private void adjustL(NodeModel model) {
        adjustChildrenXR(model);
        adjustChildrenYR();
        refreshLinesR();
    }

    public void adjustChildrenXR(NodeModel parentModel) {
        List<NodeModel> children = parentModel.getChildrenR();
        if (children.isEmpty()) {
            return;
        }

        // 父节点的 X 坐标 + 父节点的宽度 + 节点间隔
        double childX = parentModel.getX() + parentModel.getNodeWidth() + SizeConstants.NODE_GAP_X;
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
            child.setX(childX - child.getNodeWidth());
            adjustChildrenXL(child);
        }
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
            double parentMidY = parentModel.getY() + parentModel.getNodeHeight() / 2.0;
            childY = parentMidY - totalHeight / 2.0;
        } else {
            childY = y;
        }

        for (NodeModel childModel : children) {
            List<NodeModel> childrenOfChild = childModel.getChildrenR();

            double selfHeight = childModel.getNodeHeight();
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
            double parentMidY = parentModel.getY() + parentModel.getNodeHeight() / 2.0;
            childY = parentMidY - totalHeight / 2.0;
        } else {
            childY = y;
        }

        for (NodeModel childModel : children) {
            List<NodeModel> childrenOfChild = childModel.getChildrenL();

            double selfHeight = childModel.getNodeHeight();
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

    private void adjustChildrenSizeR(NodeModel nodeModel) {
        for (NodeModel childModel : nodeModel.getChildrenR()) {
            getNode(childModel).adjustSize();

            List<NodeModel> childrenOfChild = childModel.getChildrenR();
            if (!childrenOfChild.isEmpty()) {
                adjustChildrenSizeR(childModel);
            }
        }
    }

    private void adjustChildrenSizeL(NodeModel nodeModel) {
        for (NodeModel childModel : nodeModel.getChildrenL()) {
            getNode(childModel).adjustSize();

            List<NodeModel> childrenOfChild = childModel.getChildrenL();
            if (!childrenOfChild.isEmpty()) {
                adjustChildrenSizeL(childModel);
            }
        }
    }

    //———————————————————————————————————————————刷新连线———————————————————————————————————————————
    public void refreshLinesR() {
        subject.clearLineR();
        refreshLinesR(rootModel);
    }

    public void refreshLinesL() {
        subject.clearLineL();
        refreshLinesL(rootModel);
    }

    private void refreshLinesR(NodeModel parentModel) {
        List<NodeModel> childrenR = parentModel.getChildrenR();
        int size = childrenR.size();
        int maxIndex = size - 1;

        for (NodeModel childModel : childrenR) {
            // todo 根据高度优化
            QuadCurve curve = getQuadCurve(getStartR(parentModel, childrenR.indexOf(childModel), maxIndex),
                    getEndR(childModel));
            subject.addLineR(curve);

            refreshLinesR(childModel);
        }
    }

    private void refreshLinesL(NodeModel parentModel) {
        List<NodeModel> childrenL = parentModel.getChildrenL();
        int size = childrenL.size();
        int maxIndex = size - 1;

        for (NodeModel childModel : childrenL) {
            QuadCurve curve = getQuadCurve(getStartL(parentModel, childrenL.indexOf(childModel), maxIndex),
                    getEndL(childModel));
            subject.addLineL(curve);

            refreshLinesL(childModel);
        }
    }

    private Point2D getStartR(NodeModel model, int i, int maxIndex) {
        //最大值 - min（从左数第几个， 从右数第几个）
        double x = model.getX() + model.getNodeWidth() - getLevel(maxIndex - Math.min(i, maxIndex - i));
        return new Point2D(x, getMidY(model));
    }

    private Point2D getEndR(NodeModel model) {
        return new Point2D(model.getX(), getMidY(model));
    }

    private Point2D getStartL(NodeModel model, int i, int maxIndex) {
        //最大值 + min（从左数第几个，从右数第几个）
        double x = model.getX() + getLevel(maxIndex - Math.min(i, maxIndex - i));
        return new Point2D(x, getMidY(model));
    }

    private Point2D getEndL(NodeModel model) {
        return new Point2D(model.getX() + model.getNodeWidth(), getMidY(model));
    }

    private double getMidY(NodeModel model) {
        return model.getY() + model.getNodeHeight() / 2;
    }

    private int getLevel(int i) {
        if (i > 3) {
            return 40;
        } else if (i > 2) {
            return 30;
        } else if (i > 1) {
            return 10;
        } else {
            return 0;
        }
    }

    private static QuadCurve getQuadCurve(Point2D start, Point2D end) {
        QuadCurve curve = new QuadCurve(
                start.getX(), start.getY(),
                start.getX(),
                end.getY(),
                end.getX(), end.getY()
        );
        curve.setStroke(Color.rgb(100, 100, 100));
        curve.setStrokeWidth(2.5);
        curve.setFill(null);

        return curve;
    }

    //———————————————————————————————————————————其他———————————————————————————————————————————
    public void setSelectedModel(NodeModel nodeModel) {
        this.selectedModel = nodeModel;
        if (nodeModel != null) {
            getSelectedNode().getTextArea().requestFocus();
        } else {
            subject.requestFocus();
        }
    }

    public MindNode getNode(NodeModel model) {
        return subject.getModelToView().get(model);
    }

    public MindNode getSelectedNode() {
        return getNode(selectedModel);
    }

    public MindNode getRootNode() {
        return getNode(rootModel);
    }
}