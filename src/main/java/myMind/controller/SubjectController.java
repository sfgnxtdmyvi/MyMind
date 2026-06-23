package myMind.controller;

import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.QuadCurve;
import lombok.Data;
import myMind.common.constants.NodeConstants;
import myMind.common.constants.PosConstants;
import myMind.common.history.CommandHistory;
import myMind.common.history.DeleteCommand;
import myMind.common.util.CloneNodeUtil;
import myMind.componet.MapTextArea;
import myMind.componet.MindNode;
import myMind.componet.Subject;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.richtext.model.EditableStyledDocument;
import org.fxmisc.richtext.model.ReadOnlyStyledDocument;
import org.fxmisc.richtext.model.SimpleEditableStyledDocument;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

@Data
public class SubjectController {
    private final Subject subject = new Subject();
    private MindNode rootNode;
    private MindNode selectedNode;
    private CommandHistory commandHistory = new CommandHistory();

    public SubjectController() {
        rootNode = new MindNode(PosConstants.MIDDLE);
        // todo 根节点样式
        rootNode.getStyleClass().add("root-node");
        addNodeAndSelect(rootNode);
    }

    public SubjectController(MindNode node) {
        rootNode = node;
        rootNode.getStyleClass().add("root-node");
        addNodeAndSelect(node);
    }

    //———————————————————————————————————————————新增———————————————————————————————————————————

    /**
     * 在当前节点和它的子节点之间添加一个节点，
     * 原本的子节点成为新节点的子节点
     */
    public void insertChild() {
        if (selectedNode == null) {
            return;
        }

        if (selectedNode.getPos() == PosConstants.LEFT) {
            MindNode childNode = new MindNode(PosConstants.LEFT, calculateChildXL(selectedNode), calculateChildY(selectedNode));
            // 原本的子节点成为新节点的子节点
            Iterator<MindNode> iterator = selectedNode.getChildrenL().iterator();
            while (iterator.hasNext()) {
                MindNode node = iterator.next();
                iterator.remove();
                childNode.addChildL(node);
            }
            selectedNode.addChildL(childNode);
            addNodeAndSelect(childNode);

            adjustChildrenXL(selectedNode);
            refreshLinesL();
        } else {
            MindNode childNode = new MindNode(PosConstants.RIGHT, calculateChildXR(selectedNode), calculateChildY(selectedNode));
            Iterator<MindNode> iterator = selectedNode.getChildrenR().iterator();
            while (iterator.hasNext()) {
                MindNode node = iterator.next();
                iterator.remove();
                childNode.addChildR(node);
            }
            selectedNode.addChildR(childNode);
            addNodeAndSelect(childNode);

            adjustChildrenXR(selectedNode);
            refreshLinesR();
        }
    }

    public void addChildR() {
        if (selectedNode == null || selectedNode.getPos() == PosConstants.LEFT) {
            return;
        }

        if (!selectedNode.getChildrenR().isEmpty()) {
            setSubjectTranslateY(-NodeConstants.NODE_TRANSLATE);
        }
        MindNode childNode = new MindNode(PosConstants.RIGHT, calculateChildXR(selectedNode), 0);
        selectedNode.addChildR(childNode);
        addNodeAndSelect(childNode);

        adjustChildrenYR();
        refreshLinesR();
        adjustTranslate(childNode);
    }

    public void addChildL() {
        if (selectedNode == null || selectedNode.getPos() == PosConstants.RIGHT) {
            return;
        }

        if (!selectedNode.getChildrenL().isEmpty()) {
            setSubjectTranslateY(-NodeConstants.NODE_TRANSLATE);
        }
        MindNode childNode = new MindNode(PosConstants.LEFT, calculateChildXL(selectedNode), 0);
        selectedNode.addChildL(childNode);
        addNodeAndSelect(childNode);

        adjustChildrenYL();
        refreshLinesL();
        adjustTranslate(childNode);
    }

    public void addSibling() {
        if (selectedNode == null) {
            return;
        }
        // 根节点无法添加兄弟节点
        MindNode parentNode = selectedNode.getParentNode();
        if (parentNode == null) {
            return;
        }

        MindNode siblingNode;
        if (selectedNode.getPos() == PosConstants.RIGHT) {
            siblingNode = new MindNode(PosConstants.RIGHT, selectedNode.getLayoutX(), 0);
            parentNode.addChildRAt(parentNode.getChildrenR().indexOf(selectedNode) + 1, siblingNode);
            addNodeAndSelect(siblingNode);

            adjustChildrenYR();
            refreshLinesR();
        } else {
            // 父节点 X 轴 - 节点间隔 - 节点最小宽度
            siblingNode = new MindNode(PosConstants.LEFT, parentNode.getLayoutX() - NodeConstants.ADD_LEFT_NODE_GAP_X, 0);
            parentNode.addChildLAt(parentNode.getChildrenL().indexOf(selectedNode) + 1, siblingNode);
            addNodeAndSelect(siblingNode);

            adjustChildrenYL();
            refreshLinesL();
        }

        setSubjectTranslateY(-NodeConstants.NODE_TRANSLATE);
        adjustTranslate(siblingNode);
    }

    /**
     * 批量添加子节点，并选中第一个节点
     */
    public void batchAddChildR() {
        if (selectedNode.getPos() == PosConstants.LEFT) {
            return;
        }

        if (selectedNode.getChildrenR().isEmpty()) {
            setSubjectTranslateY(-NodeConstants.FOUR_NODE_TRANSLATE);
        } else {
            setSubjectTranslateY(-NodeConstants.FIVE_NODE_TRANSLATE);
        }
        MindNode firstNode = new MindNode(PosConstants.RIGHT, calculateChildXR(selectedNode), 0);
        addNodeR(firstNode);
        for (int i = 0; i < 4; i++) {
            addNodeR(new MindNode(PosConstants.RIGHT, calculateChildXR(selectedNode), 0));
        }
        setSelectedNode(firstNode);

        adjustChildrenYR();
        refreshLinesR();
    }

    public void batchAddChildL() {
        if (selectedNode.getPos() == PosConstants.RIGHT) {
            return;
        }

        if (selectedNode.getChildrenR().isEmpty()) {
            setSubjectTranslateY(-NodeConstants.FOUR_NODE_TRANSLATE);
        } else {
            setSubjectTranslateY(-NodeConstants.FIVE_NODE_TRANSLATE);
        }
        MindNode firstNode = new MindNode(PosConstants.LEFT, calculateChildXL(selectedNode), 0);
        addNodeL(firstNode);
        for (int i = 0; i < 4; i++) {
            addNodeL(new MindNode(PosConstants.LEFT, calculateChildXL(selectedNode), 0));
        }
        setSelectedNode(firstNode);

        adjustChildrenYL();
        refreshLinesL();
    }

    public void batchAddSibling() {
        if (selectedNode == null) {
            return;
        }
        // 根节点无法添加兄弟节点
        MindNode parentNode = selectedNode.getParentNode();
        if (parentNode == null) {
            return;
        }

        setSubjectTranslateY(-NodeConstants.FIVE_NODE_TRANSLATE);
        if (selectedNode.getPos() == PosConstants.RIGHT) {
            MindNode firstNode = new MindNode(PosConstants.RIGHT, selectedNode.getLayoutX(), 0);
            parentNode.addChildRAt(parentNode.getChildrenR().indexOf(selectedNode) + 1, firstNode);
            addNode(firstNode);
            for (int i = 1; i < 5; i++) {
                MindNode siblingNode = new MindNode(PosConstants.RIGHT, selectedNode.getLayoutX(), 0);
                parentNode.addChildRAt(parentNode.getChildrenR().indexOf(selectedNode) + 1 + i, siblingNode);
                addNode(siblingNode);
            }
            setSelectedNode(firstNode);

            adjustChildrenYR();
            refreshLinesR();
        } else {
            // 父节点 X 轴 - 节点间隔 - 节点最小宽度
            MindNode firstNode = new MindNode(PosConstants.LEFT, parentNode.getLayoutX() - NodeConstants.ADD_LEFT_NODE_GAP_X, 0);
            parentNode.addChildLAt(parentNode.getChildrenL().indexOf(selectedNode) + 1, firstNode);
            addNode(firstNode);
            for (int i = 1; i < 5; i++) {
                MindNode siblingNode = new MindNode(PosConstants.LEFT, selectedNode.getLayoutX(), 0);
                parentNode.addChildLAt(parentNode.getChildrenL().indexOf(selectedNode) + 1 + i, siblingNode);
                addNode(siblingNode);
            }
            setSelectedNode(firstNode);

            adjustChildrenYL();
            refreshLinesL();
        }
    }

    /**
     * 添加节点，并设它为选中节点
     */
    public void addNodeAndSelect(MindNode node) {
        addNode(node);
        setSelectedNode(node);
    }

    /**
     * 给选中节点添加子节点，并且不改变选中节点
     */
    private void addNodeR(MindNode childNode) {
        selectedNode.addChildR(childNode);
        addNode(childNode);
    }

    private void addNodeL(MindNode childNode) {
        selectedNode.addChildL(childNode);
        addNode(childNode);
    }

    private void addNode(MindNode node) {
        subject.addNode(node);
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
                    if (node.getAddButtonR().getText().equals(NodeConstants.EXPAND_R)) {
                        node.getAddButtonR().setText(NodeConstants.ADD);
                        expandR(node);
                        adjustChildrenYR();
                        refreshLinesR();
                    } else {
                        MindNode cloneNode = CloneNodeUtil.getNode();
                        if (cloneNode == null) {
                            addChildR();
                        } else {
                            pasteChild(cloneNode, PosConstants.RIGHT);
                        }
                    }
                }
                case ADD_BUTTON_L -> {
                    setSelectedNode(node);
                    if (node.getAddButtonL().getText().equals(NodeConstants.EXPAND_L)) {
                        node.getAddButtonL().setText(NodeConstants.ADD);
                        expandL(node);
                        adjustChildrenYL();
                        refreshLinesL();
                    } else {
                        MindNode cloneNode = CloneNodeUtil.getNode();
                        if (cloneNode == null) {
                            addChildL();
                        } else {
                            pasteChild(cloneNode, PosConstants.LEFT);
                        }
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

        node.setSetSubjectTranslateY(this::setSubjectTranslateY);
        node.setSetSubjectTranslateX(this::setSubjectTranslateX);
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
        return parentNode.getLayoutX() + parentNode.getPrefWidth() + NodeConstants.GAP_X;
    }

    private double calculateChildXL(MindNode parentNode) {
        // 父节点 X 轴 - 节点间隔 - 节点最小宽度
        return parentNode.getLayoutX() - NodeConstants.ADD_LEFT_NODE_GAP_X;
    }

    private double calculateChildY(MindNode parentNode) {
        return parentNode.getLayoutY() + (parentNode.getPrefHeight() - NodeConstants.MIN_NODE_HEIGHT) / 2.0;
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
        // EditableStyledDocument 是 StyleClassedTextArea 的内容（文本和样式）
        EditableStyledDocument<Collection<String>, String, Collection<String>> originalDoc
                = originalTextArea.getContent();

        int length = originalDoc.getLength();
        if (length == 0) {
            StyleClassedTextArea textArea = new MapTextArea(true);
            textArea.setMaxWidth(NodeConstants.MIN_TEXTAREA_WIDTH);
            return textArea;
        }

        // 获取只读快照
        ReadOnlyStyledDocument<Collection<String>, String, Collection<String>> snapshot
                = (ReadOnlyStyledDocument<Collection<String>, String, Collection<String>>)
                originalDoc.subSequence(0, length);

        // 基于快照构造新的可编辑文档
        StyleClassedTextArea textArea
                = new MapTextArea(new SimpleEditableStyledDocument<>(snapshot), true);
        textArea.setPrefHeight(originalTextArea.getPrefHeight());
        textArea.setMaxWidth(originalTextArea.getMaxWidth());
        textArea.layout();

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
            if (selectedNode.getParentNode().getChildrenR().size() != 1) {
                setSubjectTranslateY(selectedNode.getHeightR() * NodeConstants.TRANSLATE_RATE);
            }
            deleteChildrenFromSubjectR(selectedNode);
            deleteR(selectedNode);
            adjustChildrenYR();
            refreshLinesR();
        } else {
            if (selectedNode.getParentNode().getChildrenL().size() != 1) {
                setSubjectTranslateY(selectedNode.getHeightL() * NodeConstants.TRANSLATE_RATE);
            }
            deleteChildrenFromSubjectL(selectedNode);
            deleteL(selectedNode);
            adjustChildrenYL();
            refreshLinesL();
        }
        adjustTranslate(selectedNode);
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
            cloneNode.setLayoutX(selectedNode.getLayoutX() - NodeConstants.GAP_X - cloneNode.getPrefWidth());

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
        adjustTranslate(cloneNode);
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
            cloneNode.setLayoutX(parentNode.getLayoutX() - NodeConstants.GAP_X - cloneNode.getPrefWidth());

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
        adjustTranslate(cloneNode);
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

    private void transToLAt(int index, MindNode cloneNode) {
        cloneNode.setPos(PosConstants.LEFT);
        if (index != -1) {
            cloneNode.getParentNode().addChildLAt(index, cloneNode);
        } else {
            cloneNode.getParentNode().addChildL(cloneNode);
        }
        transBtnToL(cloneNode);

        Iterator<MindNode> iterator = cloneNode.getChildrenR().iterator();
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

    //———————————————————————————————————————————收起、展开———————————————————————————————————————————
    public void collapse() {
        if (selectedNode.getPos() == PosConstants.LEFT) {
            selectedNode.getAddButtonL().setText(NodeConstants.EXPAND_L);
            collapseL(selectedNode);
            adjustChildrenYL();
            refreshLinesL();
        } else {
            selectedNode.getAddButtonR().setText(NodeConstants.EXPAND_R);
            collapseR(selectedNode);
            adjustChildrenYR();
            refreshLinesR();
        }
    }

    private void collapseR(MindNode parentNode) {
        for (MindNode childNode : parentNode.getChildrenR()) {
            childNode.setVisible(false);
            collapseR(childNode);
        }
    }

    private void collapseL(MindNode parentNode) {
        for (MindNode childNode : parentNode.getChildrenL()) {
            childNode.setVisible(false);
            collapseL(childNode);
        }
    }

    public void expand() {
        if (selectedNode.getPos() == PosConstants.LEFT) {
            expandL(selectedNode);
            adjustChildrenYL();
            refreshLinesL();
        } else {
            expandR(selectedNode);
            adjustChildrenYR();
            refreshLinesR();
        }
    }

    private void expandR(MindNode parentNode) {
        for (MindNode childNode : parentNode.getChildrenR()) {
            childNode.setVisible(true);
            expandR(childNode);
        }
    }

    private void expandL(MindNode parentNode) {
        for (MindNode childNode : parentNode.getChildrenL()) {
            childNode.setVisible(true);
            expandL(childNode);
        }
    }

    /**
     * 收起叶子节点
     */
    public void collapseLeaf() {
        collapseLeafR(rootNode);
        collapseLeafL(rootNode);
        adjustChildrenY();
        refreshLines();
    }

    private void collapseLeafR(MindNode parentNode) {
        for (MindNode childNode : parentNode.getChildrenR()) {
            if (childNode.getChildrenR().isEmpty()) {
                parentNode.getAddButtonR().setText(NodeConstants.EXPAND_R);
                childNode.setVisible(false);
            } else {
                collapseLeafR(childNode);
            }
        }
    }

    private void collapseLeafL(MindNode parentNode) {
        for (MindNode childNode : parentNode.getChildrenL()) {
            if (childNode.getChildrenL().isEmpty()) {
                parentNode.getAddButtonL().setText(NodeConstants.EXPAND_L);
                childNode.setVisible(false);
            } else {
                collapseLeafL(childNode);
            }
        }
    }

    public void expandLeaf() {
        for (Node node : subject.getNodesLayer().getChildren()) {
            node.setVisible(true);
        }
        adjustChildrenY();
        refreshLines();
    }

    //———————————————————————————————————————————删除———————————————————————————————————————————

    /**
     *
     * @param keepChildren true： 删除选中节点及其子节点
     *                     false：删除选中节点，子节点成为父节点的子节点
     */
    public void delete(boolean keepChildren) {
        if (selectedNode == null || selectedNode == rootNode) {
            return;
        }
        commandHistory.execute(new DeleteCommand(this, selectedNode, keepChildren));
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
            deleteEmptyR(selectedNode);
            adjustChildrenYR();
            refreshLinesR();
        }
        if (selectedNode.getPos() != PosConstants.RIGHT) {
            deleteEmptyL(selectedNode);
            adjustChildrenYL();
            refreshLinesL();
        }
    }

    private void deleteEmptyR(MindNode node) {
        Iterator<MindNode> iterator = node.getChildrenR().iterator();
        while (iterator.hasNext()) {
            MindNode childNode = iterator.next();
            if (childNode.getTextArea().getText().isEmpty() && childNode.getImageName() == null) {
                iterator.remove();
                childNode.setParentNode(null);
                subject.remove(childNode);
                // 空白节点如果有子节点，一并删除
                // 一个节点有两个引用，父节点和 nodesLayer，两个引用都删除，就会被 GC 掉
                deleteChildrenFromSubjectR(childNode);
                setSubjectTranslateY(childNode.getHeightR() * NodeConstants.TRANSLATE_RATE);
            } else {
                // 非空时，递归看子节点是否为空
                deleteEmptyR(childNode);
            }
        }
    }

    private void deleteEmptyL(MindNode node) {
        Iterator<MindNode> iterator = node.getChildrenL().iterator();
        while (iterator.hasNext()) {
            MindNode childNode = iterator.next();
            if (childNode.getTextArea().getText().isEmpty() && childNode.getImageName() == null) {
                iterator.remove();
                childNode.setParentNode(null);
                subject.remove(childNode);
                deleteChildrenFromSubjectL(childNode);
                setSubjectTranslateY(childNode.getHeightL() * NodeConstants.TRANSLATE_RATE);
            } else {
                deleteEmptyL(childNode);
            }
        }
    }

    /**
     * 删除节点
     * 传入参数，保证在重做时，不选中节点的情况下，仍然能删除
     */
    public void deleteR(MindNode deletedNode) {
        MindNode parent = deletedNode.getParentNode();
        changeSelectedNode(deletedNode, parent, parent.getChildrenR());

        parent.removeChildR(deletedNode);
        subject.remove(deletedNode);
    }

    public void deleteL(MindNode deletedNode) {
        MindNode parent = deletedNode.getParentNode();
        changeSelectedNode(deletedNode, parent, parent.getChildrenL());

        parent.removeChildL(deletedNode);
        subject.remove(deletedNode);
    }

    /**
     * 删除 subject 中的子节点
     */
    public void deleteChildrenFromSubjectR(MindNode parentNode) {
        for (MindNode childNode : parentNode.getChildrenR()) {
            subject.remove(childNode);
            deleteChildrenFromSubjectR(childNode);
        }
    }

    public void deleteChildrenFromSubjectL(MindNode parentNode) {
        for (MindNode childNode : parentNode.getChildrenL()) {
            subject.remove(childNode);
            deleteChildrenFromSubjectL(childNode);
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

    public void adjustR(MindNode node) {
        adjustChildrenXR(node);
        adjustChildrenYR();
        refreshLinesR();
    }

    public void adjustL(MindNode node) {
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
        double childX = parentNode.getLayoutX() + parentNode.getPrefWidth() + NodeConstants.GAP_X;
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
        double childX = parentNode.getLayoutX() - NodeConstants.GAP_X;
        for (MindNode child : children) {
            child.setLayoutX(childX - child.getPrefWidth());
            adjustChildrenXL(child);
        }
    }

    public void adjustChildrenY() {
        adjustChildrenYR();
        adjustChildrenYL();
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
            if (!childNode.isVisible()) {
                continue;
            }
            List<MindNode> childrenOfChild = childNode.getChildrenR();

            double selfHeight = childNode.getPrefHeight();
            if (childrenOfChild.isEmpty()) {
                childNode.setLayoutY(childY);
                // 当前Y + 当前节点高度 + 间距
                childY += selfHeight + NodeConstants.GAP_Y;
            } else {
                // 当前节点的高度 < 子节点的总高度
                if (selfHeight < childNode.getChildrenHeightR()) {
                    // 先调整子节点们的位置，再让当前节点在子节点的中间
                    adjustChildrenYR(childNode, childY);
                    childNode.setLayoutY((childNode.getStartYR() + childNode.getEndYR() - selfHeight) / 2.0);
                    // 最后一个子节点的底部 + 间距
                    childY = childNode.getEndYR() + NodeConstants.GAP_Y;
                } else {
                    childNode.setLayoutY(childY);
                    // 当前 Y + 当前节点高度 + 间距
                    childY += selfHeight + NodeConstants.GAP_Y;
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
            if (!childNode.isVisible()) {
                continue;
            }
            List<MindNode> childrenOfChild = childNode.getChildrenL();

            double selfHeight = childNode.getPrefHeight();
            if (childrenOfChild.isEmpty()) {
                childNode.setLayoutY(childY);
                childY += selfHeight + NodeConstants.GAP_Y;
            } else {
                if (selfHeight < childNode.getChildrenHeightL()) {
                    adjustChildrenYL(childNode, childY);
                    childNode.setLayoutY((childNode.getStartYL() + childNode.getEndYL() - selfHeight) / 2.0);
                    childY = childNode.getEndYL() + NodeConstants.GAP_Y;
                } else {
                    childNode.setLayoutY(childY);
                    childY += selfHeight + NodeConstants.GAP_Y;
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

    /**
     * 调整节点与 scene 下面和上面的间距
     */
    public void adjustTranslate(MindNode node) {
        Point2D sceneCoords = node.localToScene(0, 0);
        double nodeY = sceneCoords.getY();
        if (nodeY < 0) {
            setSubjectTranslateY(-nodeY);
        } else if (node.getScene().getHeight() < nodeY + node.getPrefHeight()) {
            double dx = nodeY + node.getPrefHeight() - node.getScene().getHeight();
            setSubjectTranslateY(-dx);
        }
    }

    public void setSubjectTranslateY(double translateY) {
        subject.setTranslateY(subject.getTranslateY() + translateY);
    }

    public void setSubjectTranslateX(double translateX) {
        subject.setTranslateX(subject.getTranslateX() + translateX);
    }

    //———————————————————————————————————————————刷新连线———————————————————————————————————————————
    public void refreshLines() {
        refreshLinesR();
        refreshLinesL();
    }

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
            // 收起的节点不绘制
            if (!childNode.isVisible()) {
                continue;
            }
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
            if (!childNode.isVisible()) {
                continue;
            }
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

    public void undo() {
        commandHistory.undo();
    }

    public void redo() {
        commandHistory.redo();
    }
}