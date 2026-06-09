package myMind.controller;

import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.QuadCurve;
import lombok.Data;
import myMind.componet.MindNode;
import myMind.componet.Subject;
import myMind.constants.PosConstants;
import myMind.constants.SizeConstants;
import myMind.util.CloneNodeUtil;
import org.fxmisc.richtext.StyleClassedTextArea;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

@Data
public class SubjectController {
    private final Subject subject = new Subject();
    private MindNode rootNode;
    private MindNode selectedNode;

    public SubjectController() {
        rootNode = new MindNode(PosConstants.MIDDLE);
        addNode(rootNode);
    }

    public SubjectController(MindNode node) {
        rootNode = node;
        addNode(node);
    }

    //———————————————————————————————————————————新增———————————————————————————————————————————
    public void addChildR() {
        if (selectedNode == null || selectedNode.getPos() == PosConstants.LEFT) {
            return;
        }

        MindNode childNode = new MindNode(PosConstants.RIGHT, calculateChildXR(selectedNode), 0);
        selectedNode.addChildR(childNode);
        addNode(childNode);

        adjustChildrenYR();
        refreshLinesR();
    }

    public void addChildL() {
        if (selectedNode == null || selectedNode.getPos() == PosConstants.RIGHT) {
            return;
        }

        MindNode childNode = new MindNode(PosConstants.LEFT, calculateChildXL(selectedNode), 0);
        selectedNode.addChildL(childNode);
        addNode(childNode);

        adjustChildrenYL();
        refreshLinesL();
    }

    public void addSibling() {
        if (selectedNode == null) {
            return;
        }

        if (selectedNode.getPos() == PosConstants.RIGHT) {
            addSiblingR();
        } else {
            addSiblingL();
        }
    }

    public void addSiblingR() {
        // 根节点无法添加兄弟节点
        MindNode parentNode = selectedNode.getParentNode();
        if (parentNode == null) {
            return;
        }

        MindNode siblingNode = new MindNode(PosConstants.RIGHT, selectedNode.getLayoutX(), 0);
        parentNode.addChildRAt(parentNode.getChildrenR().indexOf(selectedNode) + 1, siblingNode);
        addNode(siblingNode);

        adjustChildrenYR();
        refreshLinesR();
    }

    public void addSiblingL() {
        MindNode parentNode = selectedNode.getParentNode();
        if (parentNode == null) {
            return;
        }

        // 父节点 X 轴 - 节点间隔 - 节点最小宽度
        MindNode siblingNode = new MindNode(PosConstants.LEFT, parentNode.getLayoutX() - SizeConstants.ADD_LEFT_NODE_GAP_X, 0);
        parentNode.addChildLAt(parentNode.getChildrenL().indexOf(selectedNode) + 1, siblingNode);
        addNode(siblingNode);

        adjustChildrenYL();
        refreshLinesL();
    }

    public void addNode(MindNode node) {
        subject.addNode(node);
        setSelectedNode(node);
        setOnAction(node);
    }

    private void setOnAction(MindNode node) {
        node.setOnAction(event -> {
            switch (event) {
                case SELECT -> setSelectedNode(node);

                // 粘贴到选中节点上方
                case PASTE_SIBLING -> {
                    MindNode cloneNode = CloneNodeUtil.getNode();
                    if (cloneNode != null) {
                        pasteSibling(cloneNode, node.getPos());
                    }
                }
                case ADD_BUTTON_R -> {
                    setSelectedNode(node);
                    MindNode cloneNode = CloneNodeUtil.getNode();
                    if (cloneNode == null) {
                        addChildR();
                    } else {
                        pasteChild(cloneNode, PosConstants.RIGHT);
                    }
                }
                case ADD_BUTTON_L -> {
                    setSelectedNode(node);
                    MindNode cloneNode = CloneNodeUtil.getNode();
                    if (cloneNode == null) {
                        addChildL();
                    } else {
                        pasteChild(cloneNode, PosConstants.LEFT);
                    }
                }

                case ADJUST_R -> adjustR(node);
                case ADJUST_L -> adjustL(node);
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

    private void setOnActionChildrenR(MindNode cloneNode) {
        for (MindNode node : cloneNode.getChildrenR()) {
            setOnAction(node);
            setOnActionChildrenR(node);
        }
    }

    private void setOnActionChildrenL(MindNode cloneNode) {
        for (MindNode node : cloneNode.getChildrenL()) {
            setOnAction(node);
            setOnActionChildrenL(node);
        }
    }

    private double calculateChildXR(MindNode parentNode) {
        // 父节点 X 轴 +父节点宽度 + 节点间隔
        return parentNode.getLayoutX() + parentNode.getPrefWidth() + SizeConstants.NODE_GAP_X;
    }

    private double calculateChildXL(MindNode parentNode) {
        // 父节点 X 轴 - 节点间隔 - 节点最小宽度
        return parentNode.getLayoutX() - SizeConstants.ADD_LEFT_NODE_GAP_X;
    }

    //———————————————————————————————————————————复制粘贴———————————————————————————————————————————
    public void copy() {
        CloneNodeUtil.setNode(clone(selectedNode));
    }

    private MindNode clone(MindNode originalNode) {
        byte pos = originalNode.getPos();

        MindNode cloneNode;
        String imageName = originalNode.getImageName();
        if (imageName != null) {
            ImageView image = originalNode.getImageView();
            cloneNode = new MindNode(pos, imageName, image.getFitWidth(), image.getFitHeight(), buildTextArea(originalNode.getTextArea()));
            cloneNode.getTextArea().setVisible(originalNode.getTextArea().isVisible());
        } else {
            cloneNode = new MindNode(pos, buildTextArea(originalNode.getTextArea()));
        }

        cloneNode.setPrefWidth(originalNode.getPrefWidth());
        cloneNode.setPrefHeight(originalNode.getPrefHeight());

        if (pos == PosConstants.LEFT) {
            for (MindNode childNode : originalNode.getChildrenL()) {
                cloneNode.addChildL(clone(childNode));
            }
        } else {
            for (MindNode childNode : originalNode.getChildrenR()) {
                cloneNode.addChildR(clone(childNode));
            }
        }

        // 复制根节点时，把左子节点都添加到右边
        if (pos == PosConstants.MIDDLE) {
            cloneNode.setPos(PosConstants.RIGHT);
            ObservableList<Node> children = cloneNode.getChildren();
            children.remove(cloneNode.getAddButtonL());
            cloneNode.addButtonListenR();

            for (MindNode childNode : originalNode.getChildrenL()) {
                MindNode childCloneNode = clone(childNode);
                childCloneNode.setPos(PosConstants.RIGHT);
                cloneNode.addChildR(childCloneNode);
                transBtnToR(childCloneNode);
            }
        }
        return cloneNode;
    }

    private StyleClassedTextArea buildTextArea(StyleClassedTextArea originalTextArea) {
        StyleClassedTextArea textArea = new StyleClassedTextArea();
        textArea.getStyleClass().add("text-area");
        textArea.setWrapText(true);

        int length = originalTextArea.getLength();
        if (length == 0) {
            textArea.setMaxWidth(SizeConstants.MIN_TEXTAREA_WIDTH);
            return textArea;
        }

        textArea.setPrefHeight(originalTextArea.getPrefHeight());
        textArea.setMaxWidth(originalTextArea.getMaxWidth());
        textArea.replaceText(originalTextArea.getText());
        // 样式
        int start = 0;
        Collection<String> lastStyles = originalTextArea.getStyleOfChar(0);

        for (int i = 1; i < length; i++) {
            Collection<String> currentStyles = originalTextArea.getStyleOfChar(i);

            // 样式变化时保存前一段
            if (!currentStyles.equals(lastStyles)) {
                if (!lastStyles.isEmpty()) {
                    textArea.setStyle(start, i, lastStyles);
                }

                lastStyles = currentStyles;
                start = i;
            }
        }

        // 由于最后一段不会变化，额外处理
        if (!lastStyles.isEmpty()) {
            textArea.setStyle(start, length, lastStyles);
        }

        return textArea;
    }

    public void cut() {
        if (selectedNode == null) {
            return;
        }

        // 根节点改成复制
        if (selectedNode == rootNode) {
            CloneNodeUtil.setNode(clone(selectedNode));
        } else {
            CloneNodeUtil.setNode(selectedNode);
        }

        if (selectedNode.getPos() == PosConstants.RIGHT) {
            deleteChildrenFromSubjectR(selectedNode);
            deleteR();
            adjustChildrenYR();
            refreshLinesR();
        } else {
            deleteChildrenFromSubjectL(selectedNode);
            deleteL();
            adjustChildrenYL();
            refreshLinesL();
        }
    }

    /**
     *
     * @param pos 粘到目标的左边还是右边，跟 cloneNode 的 pos 可以不一致
     */
    private void pasteChild(MindNode cloneNode, byte pos) {
        if (selectedNode == null) {
            return;
        }

        // 添加事件应在粘贴时，在复制事添加事件是用当前主题添加的，如果粘贴到其他主题则无法使用
        setOnAction(cloneNode);
        if (pos == PosConstants.RIGHT) {
            cloneNode.setLayoutX(calculateChildXR(selectedNode));

            if (cloneNode.getPos() == PosConstants.LEFT) {
                cloneNode.setParentNode(selectedNode);
                transToR(cloneNode);
            } else {
                selectedNode.addChildR(cloneNode);
            }

            subject.addClone(cloneNode);
            setOnActionChildrenR(cloneNode);
            adjustR(cloneNode);
        } else {
            // calculateChildXL 算的是新增空节点的 x 坐标
            cloneNode.setLayoutX(selectedNode.getLayoutX() - SizeConstants.NODE_GAP_X - cloneNode.getPrefWidth());

            // selectedNode 与 cloneNode 的 pos 不一致时，需要移动
            if (cloneNode.getPos() == PosConstants.RIGHT) {
                cloneNode.setParentNode(selectedNode);
                transToL(cloneNode);
            } else {
                selectedNode.addChildL(cloneNode);
            }

            subject.addClone(cloneNode);
            setOnActionChildrenL(cloneNode);
            adjustL(cloneNode);
        }

        setSelectedNode(cloneNode);
    }

    /**
     * 粘贴到选中节点的上面
     *
     */
    public void pasteSibling(MindNode cloneNode, byte pos) {
        if (selectedNode == null || selectedNode == rootNode) {
            return;
        }

        MindNode parentNode = selectedNode.getParentNode();
        setOnAction(cloneNode);
        if (pos == PosConstants.RIGHT) {
            int index = parentNode.getChildrenR().indexOf(selectedNode);
            cloneNode.setLayoutX(calculateChildXR(parentNode));

            if (cloneNode.getPos() == PosConstants.LEFT) {
                cloneNode.setParentNode(parentNode);
                transToRAt(index, cloneNode);
            } else {
                parentNode.addChildRAt(index, cloneNode);
            }

            subject.addClone(cloneNode);
            setOnActionChildrenR(cloneNode);
            adjustR(cloneNode);
        } else {
            int index = parentNode.getChildrenL().indexOf(selectedNode);
            cloneNode.setLayoutX(parentNode.getLayoutX() - SizeConstants.NODE_GAP_X - cloneNode.getPrefWidth());

            if (cloneNode.getPos() == PosConstants.RIGHT) {
                cloneNode.setParentNode(parentNode);
                transToLAt(index, cloneNode);
            } else {
                parentNode.addChildLAt(index, cloneNode);
            }

            subject.addClone(cloneNode);
            setOnActionChildrenL(cloneNode);
            adjustL(cloneNode);
        }

        setSelectedNode(cloneNode);
    }

    /**
     * 节点改到右边
     */
    private void transToR(MindNode cloneNode) {
        cloneNode.setPos(PosConstants.RIGHT);
        cloneNode.getParentNode().addChildR(cloneNode);
        transBtnToR(cloneNode);

        Iterator<MindNode> iterator = cloneNode.getChildrenL().iterator();
        while (iterator.hasNext()) {
            MindNode child = iterator.next();
            iterator.remove();
            transToR(child);
        }
    }

    private void transToL(MindNode cloneNode) {
        cloneNode.setPos(PosConstants.LEFT);
        cloneNode.getParentNode().addChildL(cloneNode);
        transBtnToL(cloneNode);

        Iterator<MindNode> iterator = cloneNode.getChildrenR().iterator();
        while (iterator.hasNext()) {
            MindNode child = iterator.next();
            iterator.remove();
            transToL(child);
        }
    }

    private void transToRAt(int index, MindNode cloneNode) {
        // 父节点粘到选中节点的上面，子节点继续跟着父节点
        cloneNode.setPos(PosConstants.RIGHT);
        if (index != -1) {
            cloneNode.getParentNode().addChildRAt(index, cloneNode);
        } else {
            cloneNode.getParentNode().addChildR(cloneNode);
        }
        transBtnToR(cloneNode);

        Iterator<MindNode> iterator = cloneNode.getChildrenL().iterator();
        while (iterator.hasNext()) {
            MindNode child = iterator.next();
            iterator.remove();
            transToRAt(-1, child);
        }
    }

    private void transToLAt(int index, MindNode clonenode) {
        clonenode.setPos(PosConstants.LEFT);
        if (index != -1) {
            clonenode.getParentNode().addChildLAt(index, clonenode);
        } else {
            clonenode.getParentNode().addChildL(clonenode);
        }
        transBtnToL(clonenode);

        Iterator<MindNode> iterator = clonenode.getChildrenR().iterator();
        while (iterator.hasNext()) {
            MindNode child = iterator.next();
            iterator.remove();
            transToLAt(-1, child);
        }
    }

    /**
     * 移动按钮
     */

    private static void transBtnToL(MindNode cloneNode) {
        ObservableList<Node> children = cloneNode.getChildren();

        children.remove(cloneNode.getAddButtonR());
        cloneNode.addButtonL(children);
        cloneNode.addButtonListenL();
    }

    private static void transBtnToR(MindNode cloneNode) {
        ObservableList<Node> children = cloneNode.getChildren();
        children.remove(cloneNode.getAddButtonL());
        cloneNode.addButtonR(children);
        cloneNode.addButtonListenR();
    }

    /**
     * 仅从 subject 删除子节点
     */
    private void deleteChildrenFromSubjectR(MindNode parentNode) {
        for (MindNode childNode : parentNode.getChildrenR()) {
            subject.remove(childNode);
            deleteChildrenFromSubjectR(childNode);
        }
    }

    private void deleteChildrenFromSubjectL(MindNode parentNode) {
        for (MindNode childNode : parentNode.getChildrenL()) {
            subject.remove(childNode);
            deleteChildrenFromSubjectL(childNode);
        }
    }

    //———————————————————————————————————————————删除———————————————————————————————————————————

    /**
     * 删除节点及其子节点
     */
    public void delete() {
        if (selectedNode == null || selectedNode == rootNode) {
            return;
        }

        if (selectedNode.getPos() == PosConstants.RIGHT) {
            deleteChildrenR(selectedNode);
            deleteR();
            adjustChildrenYR();
            refreshLinesR();
        } else {
            deleteChildrenL(selectedNode);
            deleteL();
            adjustChildrenYL();
            refreshLinesL();
        }
    }

    /**
     * 删除节点，子节点成为父节点的子节点
     */
    public void deleteRemainChildren() {
        if (selectedNode == null || selectedNode == rootNode) {
            return;
        }

        MindNode parentNode = selectedNode.getParentNode();
        double selfHeight = selectedNode.getPrefHeight();
        if (selectedNode.getPos() == PosConstants.RIGHT) {
            List<MindNode> childrenR = selectedNode.getChildrenR();
            if (childrenR.isEmpty()) {
                return;
            }

            double childrenHeight = selectedNode.getChildrenHeightR();
            for (MindNode childNode : childrenR) {
                parentNode.addChildR(childNode);
            }
            deleteR();

            adjustChildrenXR(parentNode);
            if (selfHeight > childrenHeight) {
                adjustChildrenYR();
            }
            refreshLinesR();
        } else {
            List<MindNode> childrenL = selectedNode.getChildrenL();
            if (childrenL.isEmpty()) {
                return;
            }

            double childrenHeight = selectedNode.getChildrenHeightL();
            for (MindNode childNode : childrenL) {
                parentNode.addChildL(childNode);
            }
            deleteL();

            adjustChildrenXL(parentNode);
            if (selfHeight > childrenHeight) {
                adjustChildrenYL();
            }
            refreshLinesL();
        }
    }

    /**
     * 删除空白节点
     */
    public void deleteEmpty() {
        if (selectedNode == null) {
            return;
        }

        // 中间节点要删左右子节点
        if (selectedNode.getPos() != PosConstants.LEFT) {
            Iterator<MindNode> iterator = selectedNode.getChildrenR().iterator();
            while (iterator.hasNext()) {
                MindNode childNode = iterator.next();
                if (childNode.getTextArea().getText().isEmpty() && childNode.getImageName() == null) {
                    iterator.remove();
                    childNode.setParentNode(null);
                    subject.remove(childNode);
                    // 空白节点如果有子节点，一并删除
                    // 一个节点有两个引用，父节点和 nodesLayer，两个引用都删除，就会被 GC 掉
                    deleteChildrenFromSubjectR(childNode);
                }
            }
            adjustChildrenYR();
            refreshLinesR();
        }
        if (selectedNode.getPos() != PosConstants.RIGHT) {
            Iterator<MindNode> iterator = selectedNode.getChildrenL().iterator();
            while (iterator.hasNext()) {
                MindNode childNode = iterator.next();
                if (childNode.getTextArea().getText().isEmpty() && childNode.getImageName() == null) {
                    iterator.remove();
                    childNode.setParentNode(null);
                    subject.remove(childNode);
                    deleteChildrenFromSubjectL(childNode);
                }
            }
            adjustChildrenYL();
            refreshLinesL();
        }
    }

    /**
     * 删除节点
     */
    private void deleteR() {
        MindNode parent = selectedNode.getParentNode();
        // 改变选中节点，记录之前选中的要删除的节点
        MindNode toDelete = selectedNode;
        changeSelectedNode(toDelete, parent, parent.getChildrenR());

        parent.removeChildR(toDelete);
        subject.remove(toDelete);
    }

    private void deleteL() {
        MindNode parent = selectedNode.getParentNode();
        MindNode toDelete = selectedNode;
        changeSelectedNode(toDelete, parent, parent.getChildrenL());

        parent.removeChildL(toDelete);
        subject.remove(toDelete);
    }

    /**
     * 从 subject 和父节点的子节点数组中删除子节点
     */
    private void deleteChildrenR(MindNode parentNode) {
        Iterator<MindNode> iterator = parentNode.getChildrenR().iterator();
        while (iterator.hasNext()) {
            MindNode childNode = iterator.next();
            subject.remove(childNode);
            iterator.remove();
            childNode.setParentNode(null);

            deleteChildrenR(childNode);
        }
    }

    private void deleteChildrenL(MindNode parentNode) {
        Iterator<MindNode> iterator = parentNode.getChildrenL().iterator();
        while (iterator.hasNext()) {
            MindNode childNode = iterator.next();
            subject.remove(childNode);
            iterator.remove();
            childNode.setParentNode(null);

            deleteChildrenL(childNode);
        }
    }

    /**
     * 改变选中节点
     * 有下一个节点就改成下一个节点，没有就改成上一个
     * 没有兄弟节点，就改成父节点
     */
    private void changeSelectedNode(MindNode toDelete, MindNode parent, List<MindNode> children) {
        if (children.size() == 1) {
            setSelectedNode(parent);
        } else {
            int index = children.indexOf(toDelete);
            if (index != children.size() - 1) {
                setSelectedNode(children.get(index + 1));
            } else {
                setSelectedNode(children.get(index - 1));
            }
        }
    }

    //———————————————————————————————————————————调整———————————————————————————————————————————
    public void adjustXY() {
        adjustR(rootNode);
        adjustL(rootNode);
    }

    private void adjustR(MindNode node) {
        adjustChildrenXR(node);
        adjustChildrenYR();
        refreshLinesR();
    }

    private void adjustL(MindNode node) {
        adjustChildrenXL(node);
        adjustChildrenYL();
        refreshLinesL();
    }

    /**
     * 调整子节点 X 坐标
     */
    public void adjustChildrenXR(MindNode parentNode) {
        List<MindNode> children = parentNode.getChildrenR();
        if (children.isEmpty()) {
            return;
        }

        // 父节点的 X 坐标 + 父节点的宽度 + 节点间隔
        double childX = parentNode.getLayoutX() + parentNode.getPrefWidth() + SizeConstants.NODE_GAP_X;
        for (MindNode child : children) {
            child.setLayoutX(childX);
            adjustChildrenXR(child);
        }
    }

    public void adjustChildrenXL(MindNode parentNode) {
        List<MindNode> children = parentNode.getChildrenL();
        if (children.isEmpty()) {
            return;
        }

        // 父节点的 X 坐标 - 节点间隔 - 子节点的宽度
        double childX = parentNode.getLayoutX() - SizeConstants.NODE_GAP_X;
        for (MindNode child : children) {
            child.setLayoutX(childX - child.getPrefWidth());
            adjustChildrenXL(child);
        }
    }

    // 调整子节点Y坐标
    public void adjustChildrenYR() {
        adjustChildrenYR(rootNode, null);
    }

    public void adjustChildrenYL() {
        adjustChildrenYL(rootNode, null);
    }

    /**
     * 子节点以父节点为中心，依次排列
     */
    private void adjustChildrenYR(MindNode parentNode, Double y) {
        List<MindNode> children = parentNode.getChildrenR();
        if (children.isEmpty()) {
            return;
        }

        // 递归时，y 不为空，以传入的 y 为第一个子节点的 Y 坐标
        double childY;
        if (y == null) {
            double totalHeight = parentNode.getChildrenHeightR();
            double parentMidY = parentNode.getLayoutY() + parentNode.getPrefHeight() / 2.0;
            childY = parentMidY - totalHeight / 2.0;
        } else {
            childY = y;
        }

        for (MindNode childNode : children) {
            List<MindNode> childrenOfChild = childNode.getChildrenR();

            double selfHeight = childNode.getPrefHeight();
            if (childrenOfChild.isEmpty()) {
                childNode.setLayoutY(childY);
                // 当前Y + 当前节点高度 + 间距
                childY += selfHeight + SizeConstants.NODE_GAP_Y;
            } else {
                // 当前节点的高度 < 子节点的总高度
                if (selfHeight < childNode.getChildrenHeightR()) {
                    // 先调整子节点们的位置，再让当前节点在子节点的中间
                    adjustChildrenYR(childNode, childY);
                    childNode.setLayoutY((childNode.getStartYR() + childNode.getEndYR() - selfHeight) / 2.0);
                    // 最后一个子节点的底部 + 间距
                    childY = childNode.getEndYR() + SizeConstants.NODE_GAP_Y;
                } else {
                    childNode.setLayoutY(childY);
                    // 当前Y + 当前节点高度 + 间距
                    childY += selfHeight + SizeConstants.NODE_GAP_Y;
                    adjustChildrenYR(childNode, null);
                }
            }
        }
    }

    private void adjustChildrenYL(MindNode parentNode, Double y) {
        List<MindNode> children = parentNode.getChildrenL();
        if (children.isEmpty()) {
            return;
        }

        double childY;
        if (y == null) {
            double totalHeight = parentNode.getChildrenHeightL();
            double parentMidY = parentNode.getLayoutY() + parentNode.getPrefHeight() / 2.0;
            childY = parentMidY - totalHeight / 2.0;
        } else {
            childY = y;
        }

        for (MindNode childNode : children) {
            List<MindNode> childrenOfChild = childNode.getChildrenL();

            double selfHeight = childNode.getPrefHeight();
            if (childrenOfChild.isEmpty()) {
                childNode.setLayoutY(childY);
                childY += selfHeight + SizeConstants.NODE_GAP_Y;
            } else {
                if (selfHeight < childNode.getChildrenHeightL()) {
                    adjustChildrenYL(childNode, childY);
                    childNode.setLayoutY((childNode.getStartYL() + childNode.getEndYL() - selfHeight) / 2.0);
                    childY = childNode.getEndYL() + SizeConstants.NODE_GAP_Y;
                } else {
                    childNode.setLayoutY(childY);
                    childY += selfHeight + SizeConstants.NODE_GAP_Y;
                    adjustChildrenYL(childNode, null);
                }
            }
        }
    }

    public void adjustChildrenSize() {
        rootNode.adjustSize();
        adjustChildrenSizeR(rootNode);
        adjustChildrenSizeL(rootNode);
    }

    private void adjustChildrenSizeR(MindNode MindNode) {
        for (MindNode childNode : MindNode.getChildrenR()) {
            childNode.adjustSize();
            adjustChildrenSizeR(childNode);
        }
    }

    private void adjustChildrenSizeL(MindNode MindNode) {
        for (MindNode childNode : MindNode.getChildrenL()) {
            childNode.adjustSize();
            adjustChildrenSizeL(childNode);
        }
    }

    //———————————————————————————————————————————刷新连线———————————————————————————————————————————
    public void refreshLinesR() {
        subject.clearLineR();
        refreshLinesR(rootNode);
    }

    public void refreshLinesL() {
        subject.clearLineL();
        refreshLinesL(rootNode);
    }

    private void refreshLinesR(MindNode parentNode) {
        List<MindNode> childrenR = parentNode.getChildrenR();
        int size = childrenR.size();
        int maxIndex = size - 1;

        for (MindNode childNode : childrenR) {
            // todo 根据高度优化
            QuadCurve curve = getQuadCurve(getStartR(parentNode, childrenR.indexOf(childNode), maxIndex),
                    getEndR(childNode));
            subject.addLineR(curve);

            refreshLinesR(childNode);
        }
    }

    private void refreshLinesL(MindNode parentNode) {
        List<MindNode> childrenL = parentNode.getChildrenL();
        int size = childrenL.size();
        int maxIndex = size - 1;

        for (MindNode childNode : childrenL) {
            QuadCurve curve = getQuadCurve(getStartL(parentNode, childrenL.indexOf(childNode), maxIndex),
                    getEndL(childNode));
            subject.addLineL(curve);

            refreshLinesL(childNode);
        }
    }

    private Point2D getStartR(MindNode node, int i, int maxIndex) {
        //最大值 - min（从左数第几个， 从右数第几个）
        double x = node.getLayoutX() + node.getPrefWidth() - getLevel(maxIndex - Math.min(i, maxIndex - i));
        return new Point2D(x, getMidY(node));
    }

    private Point2D getStartL(MindNode node, int i, int maxIndex) {
        //最大值 + min（从左数第几个，从右数第几个）
        double x = node.getLayoutX() + getLevel(maxIndex - Math.min(i, maxIndex - i));
        return new Point2D(x, getMidY(node));
    }

    private Point2D getEndR(MindNode node) {
        return new Point2D(node.getLayoutX(), getMidY(node));
    }

    private Point2D getEndL(MindNode node) {
        return new Point2D(node.getLayoutX() + node.getPrefWidth(), getMidY(node));
    }

    private double getMidY(MindNode node) {
        return node.getLayoutY() + node.getPrefHeight() / 2;
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
    public void setSelectedNode(MindNode node) {
        this.selectedNode = node;
        selectedNode.getTextArea().requestFocus();
    }
}