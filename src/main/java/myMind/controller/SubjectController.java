package myMind.controller;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.QuadCurve;
import lombok.Data;
import myMind.common.constants.NodeConstants;
import myMind.common.constants.PosConstants;
import myMind.common.history.CommandHistory;
import myMind.common.history.DeleteCommand;
import myMind.common.manager.ReferenceManager;
import myMind.common.util.CloneNodeUtil;
import myMind.common.util.IdGenerator;
import myMind.componet.MapNode;
import myMind.componet.MapTextArea;
import myMind.componet.MindMap;
import myMind.componet.Subject;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.richtext.model.EditableStyledDocument;
import org.fxmisc.richtext.model.ReadOnlyStyledDocument;
import org.fxmisc.richtext.model.SimpleEditableStyledDocument;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@Data
public class SubjectController {
    private final Subject subject;
    private MapNode rootNode;
    private MapNode selectedNode;
    private CommandHistory commandHistory = new CommandHistory();

    public SubjectController() {
        subject = new Subject(IdGenerator.nextId());
        rootNode = new MapNode(PosConstants.MIDDLE);
        // todo 根节点样式
        rootNode.getStyleClass().add("root-node");
        addNode(rootNode);
        Platform.runLater(() -> {
            selectedNode = rootNode;
            selectedNode.getTextArea().requestFocus();
        });
    }

    public SubjectController(MapNode node, long id) {
        subject = new Subject(id);
        rootNode = node;
        rootNode.getStyleClass().add("root-node");
        addNode(node);
        Platform.runLater(() -> {
            selectedNode = rootNode;
            selectedNode.getTextArea().requestFocus();
        });
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
            MapNode childNode = new MapNode(PosConstants.LEFT, calculateChildXL(selectedNode), calculateChildY(selectedNode));
            // 原本的子节点成为新节点的子节点
            Iterator<MapNode> iterator = selectedNode.getChildrenL().iterator();
            while (iterator.hasNext()) {
                MapNode node = iterator.next();
                iterator.remove();
                childNode.addChildL(node);
            }
            selectedNode.addChildL(childNode);
            addNodeAndSelect(childNode);

            adjustChildrenXL(selectedNode);
            refreshLinesL();
        } else {
            MapNode childNode = new MapNode(PosConstants.RIGHT, calculateChildXR(selectedNode), calculateChildY(selectedNode));
            Iterator<MapNode> iterator = selectedNode.getChildrenR().iterator();
            while (iterator.hasNext()) {
                MapNode node = iterator.next();
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
        MapNode childNode = new MapNode(PosConstants.RIGHT, calculateChildXR(selectedNode), 0);
        selectedNode.addChildR(childNode);
        addNodeAndSelect(childNode);

        adjustChildrenYR();
        refreshLinesR();
        adjustTranslateY(childNode);
    }

    public void addChildL() {
        if (selectedNode == null || selectedNode.getPos() == PosConstants.RIGHT) {
            return;
        }

        if (!selectedNode.getChildrenL().isEmpty()) {
            setSubjectTranslateY(-NodeConstants.NODE_TRANSLATE);
        }
        MapNode childNode = new MapNode(PosConstants.LEFT, calculateChildXL(selectedNode), 0);
        selectedNode.addChildL(childNode);
        addNodeAndSelect(childNode);

        adjustChildrenYL();
        refreshLinesL();
        adjustTranslateY(childNode);
    }

    public void addSibling() {
        if (selectedNode == null) {
            return;
        }
        // 根节点无法添加兄弟节点
        MapNode parentNode = selectedNode.getParentNode();
        if (parentNode == null) {
            return;
        }

        MapNode siblingNode;
        if (selectedNode.getPos() == PosConstants.RIGHT) {
            siblingNode = new MapNode(PosConstants.RIGHT, selectedNode.getLayoutX(), 0);
            parentNode.addChildRAt(parentNode.getChildrenR().indexOf(selectedNode) + 1, siblingNode);
            addNodeAndSelect(siblingNode);

            adjustChildrenYR();
            refreshLinesR();
        } else {
            // 父节点 X 轴 - 节点间隔 - 节点最小宽度
            siblingNode = new MapNode(PosConstants.LEFT, parentNode.getLayoutX() - NodeConstants.ADD_LEFT_NODE_GAP_X, 0);
            parentNode.addChildLAt(parentNode.getChildrenL().indexOf(selectedNode) + 1, siblingNode);
            addNodeAndSelect(siblingNode);

            adjustChildrenYL();
            refreshLinesL();
        }

        setSubjectTranslateY(-NodeConstants.NODE_TRANSLATE);
        adjustTranslateY(siblingNode);
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
        MapNode firstNode = new MapNode(PosConstants.RIGHT, calculateChildXR(selectedNode), 0);
        addNodeR(firstNode);
        for (int i = 0; i < 4; i++) {
            addNodeR(new MapNode(PosConstants.RIGHT, calculateChildXR(selectedNode), 0));
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
        MapNode firstNode = new MapNode(PosConstants.LEFT, calculateChildXL(selectedNode), 0);
        addNodeL(firstNode);
        for (int i = 0; i < 4; i++) {
            addNodeL(new MapNode(PosConstants.LEFT, calculateChildXL(selectedNode), 0));
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
        MapNode parentNode = selectedNode.getParentNode();
        if (parentNode == null) {
            return;
        }

        setSubjectTranslateY(-NodeConstants.FIVE_NODE_TRANSLATE);
        if (selectedNode.getPos() == PosConstants.RIGHT) {
            MapNode firstNode = new MapNode(PosConstants.RIGHT, selectedNode.getLayoutX(), 0);
            parentNode.addChildRAt(parentNode.getChildrenR().indexOf(selectedNode) + 1, firstNode);
            addNode(firstNode);
            for (int i = 1; i < 5; i++) {
                MapNode siblingNode = new MapNode(PosConstants.RIGHT, selectedNode.getLayoutX(), 0);
                parentNode.addChildRAt(parentNode.getChildrenR().indexOf(selectedNode) + 1 + i, siblingNode);
                addNode(siblingNode);
            }
            setSelectedNode(firstNode);

            adjustChildrenYR();
            refreshLinesR();
        } else {
            // 父节点 X 轴 - 节点间隔 - 节点最小宽度
            MapNode firstNode = new MapNode(PosConstants.LEFT, parentNode.getLayoutX() - NodeConstants.ADD_LEFT_NODE_GAP_X, 0);
            parentNode.addChildLAt(parentNode.getChildrenL().indexOf(selectedNode) + 1, firstNode);
            addNode(firstNode);
            for (int i = 1; i < 5; i++) {
                MapNode siblingNode = new MapNode(PosConstants.LEFT, selectedNode.getLayoutX(), 0);
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
    public void addNodeAndSelect(MapNode node) {
        addNode(node);
        setSelectedNode(node);
    }

    /**
     * 给选中节点添加子节点，并且不改变选中节点
     */
    private void addNodeR(MapNode childNode) {
        selectedNode.addChildR(childNode);
        addNode(childNode);
    }

    private void addNodeL(MapNode childNode) {
        selectedNode.addChildL(childNode);
        addNode(childNode);
    }

    public void addNode(MapNode node) {
        subject.addNode(node);
        setOnAction(node);
    }

    private void setOnAction(MapNode node) {
        node.setOnAction(event -> {
            switch (event) {
                case CLICK -> {
                    if (ReferenceManager.isReferencing()) {
                        ReferenceManager.setReferencing(false);

                        // 添加引用
                        MapNode srcNode = ReferenceManager.getSrcNode();
                        srcNode.setOutgoingReference(node);
                        StyleClassedTextArea textArea = srcNode.getTextArea();
                        textArea.setStyle(0, textArea.getText().length(), Collections.singletonList("quote"));

                        node.addIncomingReference(srcNode);
                        node.setSubjectId(subject.getSubjectId());

                        ReferenceManager.back();
                    } else {
                        setSelectedNode(node);
                    }
                }
                case JUMP -> {
                    if (node.getOutgoingReference() != null) {
                        MapNode targetNode = node.getOutgoingReference();
                        MindMap mindMap = getMindMap();

                        // 记录当前位置
                        ReferenceManager.setSrc(mindMap, subject);

                        // 跳转过去
                        Tab tab = mindMap.jumpToSubject(targetNode.getSubjectId());
                        SubjectController subjectController = (SubjectController) tab.getUserData();
                        subjectController.toCenter(targetNode);
                        subjectController.setSelectedNode(targetNode);
                    }
                }

                // 粘贴到选中节点上方
                case PASTE_SIBLING -> {
                    MapNode cloneNode = CloneNodeUtil.getNode();
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
                        MapNode cloneNode = CloneNodeUtil.getNode();
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
                        MapNode cloneNode = CloneNodeUtil.getNode();
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

    public MindMap getMindMap() {
        return (MindMap) getSubject().getParent().getParent();
    }

    private void setOnActionChildrenR(MapNode cloneNode) {
        for (MapNode node : cloneNode.getChildrenR()) {
            setOnAction(node);
            setOnActionChildrenR(node);
        }
    }

    private void setOnActionChildrenL(MapNode cloneNode) {
        for (MapNode node : cloneNode.getChildrenL()) {
            setOnAction(node);
            setOnActionChildrenL(node);
        }
    }

    private double calculateChildXR(MapNode parentNode) {
        // 父节点 X 轴 +父节点宽度 + 节点间隔
        return parentNode.getLayoutX() + parentNode.getPrefWidth() + NodeConstants.GAP_X;
    }

    private double calculateChildXL(MapNode parentNode) {
        // 父节点 X 轴 - 节点间隔 - 节点最小宽度
        return parentNode.getLayoutX() - NodeConstants.ADD_LEFT_NODE_GAP_X;
    }

    private double calculateChildY(MapNode parentNode) {
        return parentNode.getLayoutY() + (parentNode.getPrefHeight() - NodeConstants.MIN_NODE_HEIGHT) / 2.0;
    }

    //———————————————————————————————————————————复制粘贴———————————————————————————————————————————
    public void copy() {
        CloneNodeUtil.setNode(clone(selectedNode));
    }

    private MapNode clone(MapNode originalNode) {
        byte pos = originalNode.getPos();

        MapNode cloneNode = new MapNode(pos, IdGenerator.nextId(), buildTextArea(originalNode.getTextArea()));
        String imageName = originalNode.getImageName();
        if (imageName != null) {
            ImageView image = originalNode.getImageView();
            cloneNode.setImage(imageName, image.getFitWidth(), image.getFitHeight());
            cloneNode.getTextArea().setVisible(originalNode.getTextArea().isVisible());
        }

        cloneNode.setPrefWidth(originalNode.getPrefWidth());
        cloneNode.setPrefHeight(originalNode.getPrefHeight());

        if (pos == PosConstants.LEFT) {
            for (MapNode childNode : originalNode.getChildrenL()) {
                cloneNode.addChildL(clone(childNode));
            }
        } else {
            for (MapNode childNode : originalNode.getChildrenR()) {
                cloneNode.addChildR(clone(childNode));
            }
        }

        // 复制根节点时，把左子节点都添加到右边
        if (pos == PosConstants.MIDDLE) {
            cloneNode.setPos(PosConstants.RIGHT);
            ObservableList<Node> children = cloneNode.getChildren();
            children.remove(cloneNode.getAddButtonL());
            cloneNode.addButtonListenR();

            for (MapNode childNode : originalNode.getChildrenL()) {
                MapNode childCloneNode = clone(childNode);
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
        adjustTranslateY(selectedNode);
    }

    /**
     *
     * @param pos 粘到目标的左边还是右边，跟 cloneNode 的 pos 可以不一致
     */
    private void pasteChild(MapNode cloneNode, byte pos) {
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
        adjustTranslateY(cloneNode);
        setSelectedNode(cloneNode);
    }

    /**
     * 粘贴到选中节点的上面
     *
     */
    public void pasteSibling(MapNode cloneNode, byte pos) {
        if (selectedNode == null || selectedNode == rootNode) {
            return;
        }

        MapNode parentNode = selectedNode.getParentNode();
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
        adjustTranslateY(cloneNode);
        setSelectedNode(cloneNode);
    }

    /**
     * 节点改到右边
     */
    private void transToR(MapNode cloneNode) {
        cloneNode.setPos(PosConstants.RIGHT);
        cloneNode.getParentNode().addChildR(cloneNode);
        transBtnToR(cloneNode);

        Iterator<MapNode> iterator = cloneNode.getChildrenL().iterator();
        while (iterator.hasNext()) {
            MapNode child = iterator.next();
            iterator.remove();
            transToR(child);
        }
    }

    private void transToL(MapNode cloneNode) {
        cloneNode.setPos(PosConstants.LEFT);
        cloneNode.getParentNode().addChildL(cloneNode);
        transBtnToL(cloneNode);

        Iterator<MapNode> iterator = cloneNode.getChildrenR().iterator();
        while (iterator.hasNext()) {
            MapNode child = iterator.next();
            iterator.remove();
            transToL(child);
        }
    }

    private void transToRAt(int index, MapNode cloneNode) {
        // 父节点粘到选中节点的上面，子节点继续跟着父节点
        cloneNode.setPos(PosConstants.RIGHT);
        if (index != -1) {
            cloneNode.getParentNode().addChildRAt(index, cloneNode);
        } else {
            cloneNode.getParentNode().addChildR(cloneNode);
        }
        transBtnToR(cloneNode);

        Iterator<MapNode> iterator = cloneNode.getChildrenL().iterator();
        while (iterator.hasNext()) {
            MapNode child = iterator.next();
            iterator.remove();
            transToRAt(-1, child);
        }
    }

    private void transToLAt(int index, MapNode cloneNode) {
        cloneNode.setPos(PosConstants.LEFT);
        if (index != -1) {
            cloneNode.getParentNode().addChildLAt(index, cloneNode);
        } else {
            cloneNode.getParentNode().addChildL(cloneNode);
        }
        transBtnToL(cloneNode);

        Iterator<MapNode> iterator = cloneNode.getChildrenR().iterator();
        while (iterator.hasNext()) {
            MapNode child = iterator.next();
            iterator.remove();
            transToLAt(-1, child);
        }
    }

    /**
     * 移动按钮
     */
    private static void transBtnToL(MapNode cloneNode) {
        ObservableList<Node> children = cloneNode.getChildren();

        children.remove(cloneNode.getAddButtonR());
        cloneNode.addButtonL(children);
        cloneNode.addButtonListenL();
    }

    private static void transBtnToR(MapNode cloneNode) {
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

    private void collapseR(MapNode parentNode) {
        for (MapNode childNode : parentNode.getChildrenR()) {
            childNode.setVisible(false);
            collapseR(childNode);
        }
    }

    private void collapseL(MapNode parentNode) {
        for (MapNode childNode : parentNode.getChildrenL()) {
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

    private void expandR(MapNode parentNode) {
        for (MapNode childNode : parentNode.getChildrenR()) {
            childNode.setVisible(true);
            expandR(childNode);
        }
    }

    private void expandL(MapNode parentNode) {
        for (MapNode childNode : parentNode.getChildrenL()) {
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

    private void collapseLeafR(MapNode parentNode) {
        for (MapNode childNode : parentNode.getChildrenR()) {
            if (childNode.getChildrenR().isEmpty()) {
                parentNode.getAddButtonR().setText(NodeConstants.EXPAND_R);
                childNode.setVisible(false);
            } else {
                collapseLeafR(childNode);
            }
        }
    }

    private void collapseLeafL(MapNode parentNode) {
        for (MapNode childNode : parentNode.getChildrenL()) {
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

    private void deleteEmptyR(MapNode node) {
        Iterator<MapNode> iterator = node.getChildrenR().iterator();
        while (iterator.hasNext()) {
            MapNode childNode = iterator.next();
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

    private void deleteEmptyL(MapNode node) {
        Iterator<MapNode> iterator = node.getChildrenL().iterator();
        while (iterator.hasNext()) {
            MapNode childNode = iterator.next();
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
    public void deleteR(MapNode deletedNode) {
        MapNode parent = deletedNode.getParentNode();
        changeSelectedNode(deletedNode, parent, parent.getChildrenR());

        parent.removeChildR(deletedNode);
        subject.remove(deletedNode);
    }

    public void deleteL(MapNode deletedNode) {
        MapNode parent = deletedNode.getParentNode();
        changeSelectedNode(deletedNode, parent, parent.getChildrenL());

        parent.removeChildL(deletedNode);
        subject.remove(deletedNode);
    }

    /**
     * 删除 subject 中的子节点
     */
    public void deleteChildrenFromSubjectR(MapNode parentNode) {
        for (MapNode childNode : parentNode.getChildrenR()) {
            subject.remove(childNode);
            deleteChildrenFromSubjectR(childNode);
        }
    }

    public void deleteChildrenFromSubjectL(MapNode parentNode) {
        for (MapNode childNode : parentNode.getChildrenL()) {
            subject.remove(childNode);
            deleteChildrenFromSubjectL(childNode);
        }
    }

    /**
     * 改变选中节点
     * 有下一个节点就改成下一个节点，没有就改成上一个
     * 没有兄弟节点，就改成父节点
     */
    private void changeSelectedNode(MapNode toDelete, MapNode parent, List<MapNode> children) {
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

    public void adjustR(MapNode node) {
        adjustChildrenXR(node);
        adjustChildrenYR();
        refreshLinesR();
    }

    public void adjustL(MapNode node) {
        adjustChildrenXL(node);
        adjustChildrenYL();
        refreshLinesL();
    }

    /**
     * 调整子节点 X 坐标
     */
    public void adjustChildrenXR(MapNode parentNode) {
        List<MapNode> children = parentNode.getChildrenR();
        if (children.isEmpty()) {
            return;
        }

        // 父节点的 X 坐标 + 父节点的宽度 + 节点间隔
        double childX = parentNode.getLayoutX() + parentNode.getPrefWidth() + NodeConstants.GAP_X;
        for (MapNode child : children) {
            child.setLayoutX(childX);
            adjustChildrenXR(child);
        }
    }

    public void adjustChildrenXL(MapNode parentNode) {
        List<MapNode> children = parentNode.getChildrenL();
        if (children.isEmpty()) {
            return;
        }

        // 父节点的 X 坐标 - 节点间隔 - 子节点的宽度
        double childX = parentNode.getLayoutX() - NodeConstants.GAP_X;
        for (MapNode child : children) {
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
    private void adjustChildrenYR(MapNode parentNode, Double y) {
        List<MapNode> children = parentNode.getChildrenR();
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

        for (MapNode childNode : children) {
            if (!childNode.isVisible()) {
                continue;
            }
            List<MapNode> childrenOfChild = childNode.getChildrenR();

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

    private void adjustChildrenYL(MapNode parentNode, Double y) {
        List<MapNode> children = parentNode.getChildrenL();
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

        for (MapNode childNode : children) {
            if (!childNode.isVisible()) {
                continue;
            }
            List<MapNode> childrenOfChild = childNode.getChildrenL();

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
        rootNode.adjustSize(true);
        adjustChildrenSizeR(rootNode);
        adjustChildrenSizeL(rootNode);
    }

    private void adjustChildrenSizeR(MapNode MapNode) {
        for (MapNode childNode : MapNode.getChildrenR()) {
            childNode.adjustSize(true);
            adjustChildrenSizeR(childNode);
        }
    }

    private void adjustChildrenSizeL(MapNode MapNode) {
        for (MapNode childNode : MapNode.getChildrenL()) {
            childNode.adjustSize(true);
            adjustChildrenSizeL(childNode);
        }
    }

    /**
     * 调整节点与 scene 下面和上面的间距
     */
    public void adjustTranslateY(MapNode node) {
        // 定位到 tabHeaderArea 下面的位置
        MindMap mindMap = getMindMap();
        StackPane tabHeaderArea = (StackPane) mindMap.lookup(".tab-header-area");
        Point2D mindMapPoint = mindMap.localToScene(0, tabHeaderArea.getHeight());

        Point2D sceneCoords = node.localToScene(0, 0);
        double nodeY = sceneCoords.getY();
        if (nodeY < mindMapPoint.getY()) {
            setSubjectTranslateY(mindMapPoint.getY() - nodeY);
        } else if (node.getScene().getHeight() < nodeY + node.getPrefHeight()) {
            double dy = nodeY + node.getPrefHeight() - node.getScene().getHeight();
            setSubjectTranslateY(-dy);
        }
    }

    public void adjustTranslateX(MapNode node) {
        Point2D sceneCoords = node.localToScene(0, 0);
        double nodeX = sceneCoords.getX();
        if (nodeX < 0) {
            setSubjectTranslateX(-nodeX);
        } else if (node.getScene().getWidth() < nodeX + node.getPrefWidth()) {
            double dx = nodeX + node.getPrefWidth() - node.getScene().getWidth();
            setSubjectTranslateX(-dx);
        }
    }

    public void toCenter(MapNode node) {
        Point2D sceneCoords = node.localToScene(0, 0);
        double nodeX = sceneCoords.getX();
        double nodeY = sceneCoords.getY();
        MindMap mindMap = getMindMap();
        StackPane tabHeaderArea = (StackPane) mindMap.lookup(".tab-header-area");
        double mindMapHeight = node.getScene().getHeight() - tabHeaderArea.getHeight();
        double centerX = (node.getScene().getWidth() - node.getPrefWidth()) / 2;
        double centerY = (mindMapHeight - node.getPrefHeight()) / 2;

        if (nodeX < centerX) {
            setSubjectTranslateX(centerX - nodeX);
        } else {
            setSubjectTranslateX(-(nodeX - centerX));
        }
        if (nodeY < centerY) {
            setSubjectTranslateY(centerY - nodeY);
        } else {
            setSubjectTranslateY(-(nodeY - centerY));
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

    private void refreshLinesR(MapNode parentNode) {
        List<MapNode> childrenR = parentNode.getChildrenR();
        int size = childrenR.size();
        int maxIndex = size - 1;

        for (MapNode childNode : childrenR) {
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

    private void refreshLinesL(MapNode parentNode) {
        List<MapNode> childrenL = parentNode.getChildrenL();
        int size = childrenL.size();
        int maxIndex = size - 1;

        for (MapNode childNode : childrenL) {
            if (!childNode.isVisible()) {
                continue;
            }
            QuadCurve curve = getQuadCurve(getStartL(parentNode, childrenL.indexOf(childNode), maxIndex),
                    getEndL(childNode));
            subject.addLineL(curve);

            refreshLinesL(childNode);
        }
    }

    private Point2D getStartR(MapNode node, int i, int maxIndex) {
        //最大值 - min（从左数第几个， 从右数第几个）
        double x = node.getLayoutX() + node.getPrefWidth() - getLevel(maxIndex - Math.min(i, maxIndex - i));
        return new Point2D(x, getMidY(node));
    }

    private Point2D getStartL(MapNode node, int i, int maxIndex) {
        //最大值 + min（从左数第几个，从右数第几个）
        double x = node.getLayoutX() + getLevel(maxIndex - Math.min(i, maxIndex - i));
        return new Point2D(x, getMidY(node));
    }

    private Point2D getEndR(MapNode node) {
        return new Point2D(node.getLayoutX(), getMidY(node));
    }

    private Point2D getEndL(MapNode node) {
        return new Point2D(node.getLayoutX() + node.getPrefWidth(), getMidY(node));
    }

    private double getMidY(MapNode node) {
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

    //—————————————————————————————————————————切换选中节点—————————————————————————————————————————
    public void setSelectedNode(MapNode node) {
        selectedNode.getStyleClass().remove("selected-node");
        selectedNode = node;
        selectedNode.getStyleClass().add("selected-node");
        selectedNode.getTextArea().requestFocus();
    }

    public void moveRight() {
        // 左边节点 -> 父节点
        // 根、右边节点 -> 中间的右子节点
        if (selectedNode.getPos() == PosConstants.LEFT) {
            setSelectedNode(selectedNode.getParentNode());
            adjustTranslateX(selectedNode);
        } else {
            List<MapNode> children = selectedNode.getChildrenR();
            if (!children.isEmpty()) {
                setSelectedNode(children.get(children.size() / 2));
                adjustTranslateX(selectedNode);
            }
        }
    }

    public void moveLeft() {
        // 父节点 <- 右边节点
        // 中间的左子节点 <- 左边、根节点
        if (selectedNode.getPos() == PosConstants.RIGHT) {
            setSelectedNode(selectedNode.getParentNode());
            adjustTranslateX(selectedNode);
        } else {
            List<MapNode> children = selectedNode.getChildrenL();
            if (!children.isEmpty()) {
                setSelectedNode(children.get(children.size() / 2));
                adjustTranslateX(selectedNode);
            }
        }
    }

    public void moveUp() {
        if (selectedNode.getPos() == PosConstants.MIDDLE) {
            return;
        }

        MapNode parentNode = selectedNode.getParentNode();
        if (selectedNode.getPos() == PosConstants.RIGHT) {
            List<MapNode> childrenR = parentNode.getChildrenR();
            int index = childrenR.indexOf(selectedNode);
            // 有上一个兄弟就移动到上一个兄弟
            if (index != 0) {
                setSelectedNode(childrenR.get(index - 1));
                adjustTranslateY(selectedNode);
            }
            //    叔 - 堂兄弟
            // 爷      最下面的堂兄弟（目标）
            //    父 - 当前
            else {
                MapNode grandPaNode = parentNode.getParentNode();
                if (grandPaNode == null) {
                    return;
                }
                List<MapNode> uncles = grandPaNode.getChildrenR();
                index = uncles.indexOf(parentNode);
                if (index != 0) {
                    MapNode uncle = uncles.get(index - 1);
                    List<MapNode> cousin = uncle.getChildrenR();
                    if (!cousin.isEmpty()) {
                        setSelectedNode(cousin.get(cousin.size() - 1));
                        adjustTranslateY(selectedNode);
                    }
                }
            }
        } else {
            List<MapNode> childrenL = parentNode.getChildrenL();
            int index = childrenL.indexOf(selectedNode);
            if (index != 0) {
                setSelectedNode(childrenL.get(index - 1));
                adjustTranslateY(selectedNode);
            } else {
                MapNode grandPaNode = parentNode.getParentNode();
                if (grandPaNode == null) {
                    return;
                }
                List<MapNode> uncles = grandPaNode.getChildrenL();
                index = uncles.indexOf(parentNode);
                if (index != 0) {
                    MapNode uncle = uncles.get(index - 1);
                    List<MapNode> cousin = uncle.getChildrenL();
                    if (!cousin.isEmpty()) {
                        setSelectedNode(cousin.get(cousin.size() - 1));
                        adjustTranslateY(selectedNode);
                    }
                }
            }
        }
    }

    public void moveDown() {
        if (selectedNode.getPos() == PosConstants.MIDDLE) {
            return;
        }

        MapNode parentNode = selectedNode.getParentNode();
        if (selectedNode.getPos() == PosConstants.RIGHT) {
            List<MapNode> childrenR = parentNode.getChildrenR();
            int index = childrenR.indexOf(selectedNode);
            if (index != childrenR.size() - 1) {
                setSelectedNode(childrenR.get(index + 1));
                adjustTranslateY(selectedNode);
            } else {
                MapNode grandPaNode = parentNode.getParentNode();
                if (grandPaNode == null) {
                    return;
                }
                List<MapNode> uncles = grandPaNode.getChildrenR();
                index = uncles.indexOf(parentNode);
                if (index != uncles.size() - 1) {
                    MapNode uncle = uncles.get(index + 1);
                    List<MapNode> cousin = uncle.getChildrenR();
                    if (!cousin.isEmpty()) {
                        setSelectedNode(cousin.get(0));
                        adjustTranslateY(selectedNode);
                    }
                }
            }
        } else {
            List<MapNode> childrenL = parentNode.getChildrenL();
            int index = childrenL.indexOf(selectedNode);
            if (index != childrenL.size() - 1) {
                setSelectedNode(childrenL.get(index + 1));
                adjustTranslateY(selectedNode);
            } else {
                MapNode grandPaNode = parentNode.getParentNode();
                if (grandPaNode == null) {
                    return;
                }
                List<MapNode> uncles = grandPaNode.getChildrenL();
                index = uncles.indexOf(parentNode);
                if (index != uncles.size() - 1) {
                    MapNode uncle = uncles.get(index + 1);
                    List<MapNode> cousin = uncle.getChildrenL();
                    if (!cousin.isEmpty()) {
                        setSelectedNode(cousin.get(0));
                        adjustTranslateY(selectedNode);
                    }
                }
            }
        }
    }

    //———————————————————————————————————————————其他———————————————————————————————————————————

    public void undo() {
        commandHistory.undo();
    }

    public void redo() {
        commandHistory.redo();
    }
}