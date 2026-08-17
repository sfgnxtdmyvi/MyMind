package myMind.controller;

import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.QuadCurve;
import lombok.Data;
import myMind.common.constants.LineColorConstants;
import myMind.common.constants.NodeConstants;
import myMind.common.constants.PosConstants;
import myMind.common.history.CommandHistory;
import myMind.common.history.DeleteCommand;
import myMind.common.manager.ReferenceManager;
import myMind.common.util.CloneNodeUtil;
import myMind.common.util.IdGenerator;
import myMind.componet.MapNode;
import myMind.componet.MindMap;
import myMind.componet.Subject;
import org.fxmisc.richtext.StyleClassedTextArea;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

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
     * 没有子节点：往父节点一侧插入
     * 有子节点：往子节点一侧插入
     */
    public void insert() {
        if (selectedNode == null) {
            return;
        }

        byte pos = selectedNode.getPos();
        List<MapNode> children = selectedNode.getChildren(pos);
        MapNode insertNode;
        if (children.isEmpty()) {
            if (selectedNode == rootNode) {
                return;
            }
            MapNode parentNode = selectedNode.getParentNode();
            insertNode = new MapNode(pos, calculateChildX(parentNode, pos), calculateChildY(parentNode));
            // 插入节点替代当前节点的位置
            parentNode.addChildAt(parentNode.getChildren(pos).indexOf(selectedNode), insertNode, pos);
            // 当前节点变成插入节点的子节点
            parentNode.removeChild(selectedNode, pos);
            insertNode.addChild(selectedNode, pos);
        } else {
            insertNode = new MapNode(pos, calculateChildX(selectedNode, pos), calculateChildY(selectedNode));
            // 当前节点的子节点变成插入节点的子节点
            Iterator<MapNode> iterator = children.iterator();
            while (iterator.hasNext()) {
                MapNode node = iterator.next();
                iterator.remove();
                insertNode.addChild(node, pos);
            }
            selectedNode.addChild(insertNode, pos);
        }
        addNodeAndSelect(insertNode);
        adjustChildrenX(insertNode, pos);
        refreshLines(pos);
    }

    public void addChild(byte pos) {
        if (isValidPos(pos)) {
            return;
        }

        MapNode childNode = new MapNode(pos, calculateChildX(selectedNode, pos), 0);
        selectedNode.addChild(childNode, pos);
        addNodeAndSelect(childNode);

        adjustChildrenY(pos);
        refreshLines(pos);
        adjustTranslateY(childNode);
    }

    public void addSibling() {
        if (selectedNode == null || selectedNode == rootNode) {
            return;
        }

        MapNode parentNode = selectedNode.getParentNode();
        MapNode siblingNode;
        byte pos = selectedNode.getPos();
        if (pos == PosConstants.RIGHT) {
            siblingNode = new MapNode(pos, selectedNode.getLayoutX(), 0);
        } else {
            // 父节点 X 轴 - 节点间隔 - 节点最小宽度
            siblingNode = new MapNode(pos, calculateChildX(parentNode, pos), 0);
        }

        parentNode.addChildAt(parentNode.getChildren(pos).indexOf(selectedNode) + 1, siblingNode, pos);
        addNodeAndSelect(siblingNode);
        adjustChildrenY(pos);
        refreshLines(pos);
        adjustTranslateY(siblingNode);
    }

    /**
     * 批量添加子节点，并选中第一个节点
     *
     * @param pos
     */
    public void batchAddChild(byte pos) {
        if (isValidPos(pos)) {
            return;
        }

        MapNode firstNode = new MapNode(pos, calculateChildX(selectedNode, pos), 0);
        addNode(firstNode, pos);
        for (int i = 0; i < 4; i++) {
            addNode(new MapNode(pos, calculateChildX(selectedNode, pos), 0), pos);
        }
        setSelectedNode(firstNode);

        adjustChildrenY(pos);
        refreshLines(pos);
        adjustTranslateY(firstNode.getParentNode().getLastChild(pos));
    }

    public void batchAddSibling() {
        if (selectedNode == null || selectedNode == rootNode) {
            return;
        }

        MapNode parentNode = selectedNode.getParentNode();
        byte pos = selectedNode.getPos();
        MapNode firstNode;
        if (pos == PosConstants.RIGHT) {
            firstNode = new MapNode(pos, selectedNode.getLayoutX(), 0);
        } else {
            // 父节点 X 轴 - 节点间隔 - 节点最小宽度
            firstNode = new MapNode(PosConstants.LEFT, calculateChildX(parentNode, PosConstants.LEFT), 0);
        }
        parentNode.addChildAt(parentNode.getChildren(pos).indexOf(selectedNode) + 1, firstNode, pos);
        addNode(firstNode);
        for (int i = 1; i < 5; i++) {
            MapNode siblingNode = new MapNode(pos, selectedNode.getLayoutX(), 0);
            parentNode.addChildAt(parentNode.getChildren(pos).indexOf(selectedNode) + 1 + i, siblingNode, pos);
            addNode(siblingNode);
        }
        setSelectedNode(firstNode);

        adjustChildrenY(pos);
        refreshLines(pos);
        adjustTranslateY(firstNode.getParentNode().getLastChild(pos));
    }

    /**
     * 添加节点，并设为选中节点
     */
    public void addNodeAndSelect(MapNode node) {
        addNode(node);
        setSelectedNode(node);
    }

    /**
     * 给选中节点添加子节点，并且不改变选中节点
     */
    private void addNode(MapNode childNode, byte pos) {
        selectedNode.addChild(childNode, pos);
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
                        MapNode cloneNode = CloneNodeUtil.getCloneNode();
                        if (cloneNode != null) {
                            pasteSibling(cloneNode, node.getPos());
                        }
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

                case ADD_BUTTON_R -> {
                    setSelectedNode(node);
                    if (node.getAddButtonR().getText().equals(NodeConstants.EXPAND_R)) {
                        node.getAddButtonR().setText(NodeConstants.ADD);
                        expand(node, PosConstants.RIGHT);
                        adjustChildrenY(PosConstants.RIGHT);
                        refreshLines(PosConstants.RIGHT);
                    } else {
                        MapNode cloneNode = CloneNodeUtil.getCloneNode();
                        if (cloneNode == null) {
                            addChild(PosConstants.RIGHT);
                        } else {
                            pasteChild(cloneNode, PosConstants.RIGHT);
                        }
                    }
                }
                case ADD_BUTTON_L -> {
                    setSelectedNode(node);
                    if (node.getAddButtonL().getText().equals(NodeConstants.EXPAND_L)) {
                        node.getAddButtonL().setText(NodeConstants.ADD);
                        expand(node, PosConstants.LEFT);
                        adjustChildrenY(PosConstants.LEFT);
                        refreshLines(PosConstants.LEFT);
                    } else {
                        MapNode cloneNode = CloneNodeUtil.getCloneNode();
                        if (cloneNode == null) {
                            addChild(PosConstants.LEFT);
                        } else {
                            pasteChild(cloneNode, PosConstants.LEFT);
                        }
                    }
                }

                case ADJUST_R -> adjustChildrenXY(node, PosConstants.RIGHT);
                case ADJUST_L -> adjustChildrenXY(node, PosConstants.LEFT);
                case ADJUST_YR -> {
                    adjustChildrenY(PosConstants.RIGHT);
                    refreshLines(PosConstants.RIGHT);
                }
                case ADJUST_YL -> {
                    adjustChildrenY(PosConstants.LEFT);
                    refreshLines(PosConstants.LEFT);
                }
            }
        });

        node.setSetSubjectTranslateY(this::setSubjectTranslateY);
        node.setSetSubjectTranslateX(this::setSubjectTranslateX);
    }

    public MindMap getMindMap() {
        return (MindMap) getSubject().getParent().getParent();
    }

    private void setOnActionChildren(MapNode cloneNode, byte pos) {
        for (MapNode node : cloneNode.getChildren(pos)) {
            setOnAction(node);
            setOnActionChildren(node, pos);
        }
    }

    private double calculateChildX(MapNode parentNode, byte pos) {
        if (pos == PosConstants.RIGHT) {
            // 父节点 X 轴 + 父节点宽度 + 节点间隔
            return parentNode.getLayoutX() + parentNode.getPrefWidth() + NodeConstants.GAP_X;
        } else {
            // 父节点 X 轴 - 节点间隔 - 节点最小宽度
            return parentNode.getLayoutX() - NodeConstants.ADD_LEFT_NODE_GAP_X;
        }
    }

    private double calculateChildY(MapNode parentNode) {
        return parentNode.getLayoutY() + (parentNode.getPrefHeight() - NodeConstants.MIN_NODE_HEIGHT) / 2.0;
    }

    //———————————————————————————————————————————复制粘贴———————————————————————————————————————————
    public void copy() {
        CloneNodeUtil.setCloneNode(selectedNode.clone());
    }

    public void cut() {
        if (selectedNode == null) {
            return;
        }

        // 根节点改成复制
        if (selectedNode == rootNode) {
            copy();
            return;
        } else {
            CloneNodeUtil.setCloneNode(selectedNode);
        }

        // 删除当前节点
        byte pos = selectedNode.getPos();
        if (selectedNode.getParentNode().getChildren(pos).size() != 1) {
            setSubjectTranslateY(selectedNode.getHeight(pos) * NodeConstants.TRANSLATE_RATE);
        }
        deleteChildrenFromSubject(selectedNode, pos);
        deleteNode(selectedNode, pos);

        adjustChildrenY(pos);
        refreshLines(pos);
        adjustTranslateY(selectedNode);
    }

    private void pasteChild(MapNode cloneNode, byte pos) {
        if (selectedNode == null) {
            return;
        }
        paste(selectedNode, cloneNode, -1, pos);
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
        paste(parentNode, cloneNode, parentNode.getChildren(pos).indexOf(selectedNode), pos);
    }

    /**
     * @param index -1表示添加到最后
     * @param pos   粘到目标的左边还是右边，跟 cloneNode 的 pos 可以不一致
     */
    private void paste(MapNode parentNode, MapNode cloneNode, int index, byte pos) {
        // selectedNode 与 cloneNode 的 pos 不一致时，需要移动
        if (cloneNode.getPos() != pos) {
            cloneNode.setParentNode(parentNode);
            cloneNode.transPosAt(index, cloneNode.getPos(), pos);
        } else {
            parentNode.addChildAt(index, cloneNode, pos);
        }

        // 添加事件应在粘贴时，在复制事添加事件是用当前主题添加的，如果粘贴到其他主题则无法使用
        setOnAction(cloneNode);
        setOnActionChildren(cloneNode, pos);
        subject.addClone(cloneNode);
        setSelectedNode(cloneNode);

        adjustChildrenXY(parentNode, pos);
        adjustTranslateY(cloneNode);
    }

    //———————————————————————————————————————————删除———————————————————————————————————————————

    /**
     *
     * @param keepChildren true： 删除选中节点及其子节点
     *                     false：删除选中节点，子节点成为父节点的子节点
     */
    public void deleteNode(boolean keepChildren) {
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
            byte pos = PosConstants.RIGHT;
            deleteEmpty(selectedNode, pos);
            adjustChildrenY(pos);
            refreshLines(pos);
        }
        if (selectedNode.getPos() != PosConstants.RIGHT) {
            byte left = PosConstants.LEFT;
            deleteEmpty(selectedNode, left);
            adjustChildrenY(left);
            refreshLines(left);
        }
    }

    private void deleteEmpty(MapNode node, byte pos) {
        Iterator<MapNode> iterator = node.getChildren(pos).iterator();
        while (iterator.hasNext()) {
            MapNode childNode = iterator.next();
            if (childNode.getTextArea().getText().isEmpty() && childNode.getImageName() == null) {
                iterator.remove();
                childNode.setParentNode(null);
                subject.remove(childNode);
                // 空白节点如果有子节点，一并删除
                // 一个节点有两个引用，父节点和 nodesLayer，两个引用都删除，就会被 GC 掉
                deleteChildrenFromSubject(childNode, pos);
                setSubjectTranslateY(childNode.getHeight(pos) * NodeConstants.TRANSLATE_RATE);
            } else {
                // 非空时，递归看子节点是否为空
                deleteEmpty(childNode, pos);
            }
        }
    }

    /**
     * 删除节点
     *
     */
    public void deleteNode(MapNode deletedNode, byte pos) {
        MapNode parent = deletedNode.getParentNode();
        changeSelectedNode(deletedNode, parent, parent.getChildren(pos));

        parent.removeChild(deletedNode, pos);
        subject.remove(deletedNode);
    }

    /**
     * 删除 subject 中的子节点
     *
     */
    public void deleteChildrenFromSubject(MapNode parentNode, byte pos) {
        for (MapNode childNode : parentNode.getChildren(pos)) {
            subject.remove(childNode);
            deleteChildrenFromSubject(childNode, pos);
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

    //———————————————————————————————————————————收起、展开———————————————————————————————————————————
    public void collapse() {
        byte pos = selectedNode.getPos();
        if (pos == PosConstants.RIGHT) {
            selectedNode.setAddButtonText(NodeConstants.EXPAND_R, pos);
        } else {
            selectedNode.setAddButtonText(NodeConstants.EXPAND_L, pos);
        }
        collapse(selectedNode, pos);
        adjustChildrenY(pos);
        refreshLines(pos);
    }

    private void collapse(MapNode parentNode, byte pos) {
        for (MapNode childNode : parentNode.getChildren(pos)) {
            childNode.setVisible(false);
            collapse(childNode, pos);
        }
    }

    public void expand() {
        byte pos = selectedNode.getPos();
        selectedNode.setAddButtonText(NodeConstants.ADD, pos);
        expand(selectedNode, pos);
        adjustChildrenY(pos);
        refreshLines(pos);
    }

    private void expand(MapNode parentNode, byte pos) {
        for (MapNode childNode : parentNode.getChildren(pos)) {
            childNode.setVisible(true);
            expand(childNode, pos);
        }
    }

    /**
     * 收起叶子节点
     */
    public void collapseLeaf() {
        collapseLeaf(rootNode, NodeConstants.EXPAND_R, PosConstants.RIGHT);
        collapseLeaf(rootNode, NodeConstants.EXPAND_L, PosConstants.LEFT);
        toCenter(selectedNode);
        adjustChildrenY();
        refreshLines();
    }

    private void collapseLeaf(MapNode parentNode, String text, byte pos) {
        for (MapNode childNode : parentNode.getChildren(pos)) {
            if (childNode.getChildren(pos).isEmpty()) {
                parentNode.setAddButtonText(text, pos);
                childNode.setVisible(false);
            } else {
                collapseLeaf(childNode, text, pos);
            }
        }
    }

    public void expandLeaf() {
        for (Node node : subject.getNodesLayer().getChildren()) {
            MapNode mapNode = (MapNode) node;
            if (mapNode.getAddButtonR() != null) {
                mapNode.getAddButtonR().setText(NodeConstants.ADD);
            }
            if (mapNode.getAddButtonL() != null) {
                mapNode.getAddButtonL().setText(NodeConstants.ADD);
            }
            mapNode.setVisible(true);
        }
        adjustChildrenY();
        refreshLines();
    }

    //———————————————————————————————————————————调整———————————————————————————————————————————
    public void adjustXY() {
        adjustChildrenXY(rootNode, PosConstants.RIGHT);
        adjustChildrenXY(rootNode, PosConstants.LEFT);
    }

    public void adjustChildrenXY(MapNode node, byte pos) {
        adjustChildrenX(node, pos);
        adjustChildrenY(pos);
        refreshLines(pos);
    }

    /**
     * 调整子节点 X 坐标
     */
    public void adjustChildrenX(MapNode parentNode, byte pos) {
        List<MapNode> children = parentNode.getChildren(pos);
        if (children.isEmpty()) {
            return;
        }

        if (pos == PosConstants.RIGHT) {
            // 父节点的 X 坐标 + 父节点的宽度 + 节点间隔
            double childX = calculateChildX(parentNode, pos);
            for (MapNode child : children) {
                child.setLayoutX(childX);
                adjustChildrenX(child, pos);
            }
        } else {
            // 父节点的 X 坐标 - 节点间隔 - 子节点的宽度
            double childX = parentNode.getLayoutX() - NodeConstants.GAP_X;
            for (MapNode child : children) {
                child.setLayoutX(childX - child.getPrefWidth());
                adjustChildrenX(child, pos);
            }
        }
    }

    public void adjustChildrenY() {
        adjustChildrenY(PosConstants.RIGHT);
        adjustChildrenY(PosConstants.LEFT);
    }

    // 调整子节点Y坐标
    public void adjustChildrenY(byte pos) {
        adjustChildrenY(rootNode, null, pos);
    }

    /**
     * 子节点以父节点为中心，依次排列
     */
    private void adjustChildrenY(MapNode parentNode, Double y, byte pos) {
        List<MapNode> children = parentNode.getChildren(pos);
        if (children.isEmpty()) {
            return;
        }

        // 递归时，y 不为空，以传入的 y 为第一个子节点的 Y 坐标
        double childY;
        if (y == null) {
            double totalHeight = parentNode.getChildrenHeight(pos);
            double parentMidY = parentNode.getLayoutY() + parentNode.getPrefHeight() / 2.0;
            childY = parentMidY - totalHeight / 2.0;
        } else {
            childY = y;
        }

        for (MapNode childNode : children) {
            if (!childNode.isVisible()) {
                continue;
            }
            List<MapNode> childrenOfChild = childNode.getChildren(pos);

            double selfHeight = childNode.getPrefHeight();
            if (childrenOfChild.isEmpty()) {
                childNode.setLayoutY(childY);
                // 当前Y + 当前节点高度 + 间距
                childY += selfHeight + NodeConstants.GAP_Y;
            } else {
                // 当前节点的高度 < 子节点的总高度
                if (selfHeight < childNode.getChildrenHeight(pos)) {
                    // 先调整子节点们的位置，再让当前节点在子节点的中间
                    adjustChildrenY(childNode, childY, pos);
                    childNode.setLayoutY((childNode.getStartY(pos) + childNode.getEndY(pos) - selfHeight) / 2.0);
                    // 最后一个子节点的底部 + 间距
                    childY = childNode.getEndY(pos) + NodeConstants.GAP_Y;
                } else {
                    childNode.setLayoutY(childY);
                    // 当前 Y + 当前节点高度 + 间距
                    childY += selfHeight + NodeConstants.GAP_Y;
                    adjustChildrenY(childNode, null, pos);
                }
            }
        }
    }

    public void adjustChildrenSize() {
        rootNode.adjustSize(true);
        adjustChildrenSize(rootNode, PosConstants.RIGHT);
        adjustChildrenSize(rootNode, PosConstants.LEFT);
    }

    private void adjustChildrenSize(MapNode MapNode, byte pos) {
        for (MapNode childNode : MapNode.getChildren(pos)) {
            childNode.adjustSize(true);
            adjustChildrenSize(childNode, pos);
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
        refreshLines(PosConstants.RIGHT);
        refreshLines(PosConstants.LEFT);
    }

    public void refreshLines(byte pos) {
        subject.clearLine(pos);
        // 根节点的子结点：颜色根据索引从 LineColorConstants 获取
        refreshLines(rootNode, LineColorConstants::getColor, pos);
    }

    private void refreshLines(MapNode parentNode, Function<Integer, Paint> paintSupplier, byte pos) {
        List<MapNode> children = parentNode.getChildren(pos);
        int size = children.size();
        int maxIndex = size - 1;

        for (int i = 0; i < size; i++) {
            MapNode childNode = children.get(i);
            // 收起的节点不绘制
            if (!childNode.isVisible()) {
                continue;
            }

            Paint color = paintSupplier.apply(i);
            // todo 根据高度优化
            QuadCurve curve = getQuadCurve(
                    getStart(parentNode, i, maxIndex, pos),
                    getEnd(childNode, pos),
                    color
            );
            subject.addLine(curve, pos);

            // 非根节点的子结点：使用同一个颜色
            refreshLines(childNode, index -> color, pos);
        }
    }

    private Point2D getStart(MapNode node, int i, int maxIndex, byte pos) {
        double x;
        if (pos == PosConstants.RIGHT) {
            //最大值 - min（从左数第几个， 从右数第几个）
            x = node.getLayoutX() + node.getPrefWidth() - getLevel(maxIndex - Math.min(i, maxIndex - i));
        } else {
            //最大值 + min（从左数第几个，从右数第几个）
            x = node.getLayoutX() + getLevel(maxIndex - Math.min(i, maxIndex - i));
        }
        return new Point2D(x, getMidY(node));
    }

    private Point2D getEnd(MapNode node, byte pos) {
        if (pos == PosConstants.RIGHT) {
            return new Point2D(node.getLayoutX(), getMidY(node));
        } else {
            return new Point2D(node.getLayoutX() + node.getPrefWidth(), getMidY(node));
        }
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

    private static QuadCurve getQuadCurve(Point2D start, Point2D end, Paint paint) {
        QuadCurve curve = new QuadCurve(
                start.getX(), start.getY(),
                start.getX(),
                end.getY(),
                end.getX(), end.getY()
        );
        curve.setStroke(paint);
        curve.setStrokeWidth(2.8);
        curve.setFill(null);

        return curve;
    }

    //—————————————————————————————————————————切换选中节点—————————————————————————————————————————
    public void setSelectedNode(MapNode node) {
        selectedNode.getStyleClass().remove("selected-node");
        selectedNode = node;
        selectedNode.getStyleClass().add("selected-node");
        // 保证在通过快捷键切换选中节点后，文本框获得焦点
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
        byte pos = selectedNode.getPos();
        if (pos == PosConstants.MIDDLE) {
            return;
        }

        MapNode parentNode = selectedNode.getParentNode();
        List<MapNode> children = parentNode.getChildren(pos);
        // 有兄弟就移动到兄弟
        int index = children.indexOf(selectedNode);
        if (index != 0) {
            setSelectedNode(children.get(index - 1));
        } else {
            //            broAncestor - node - node - node - brother
            // ancestor -
            //            curAncestor - node - node - node - selectedNode
            // 当前节点的深度
            int depth = 1;
            // 当前节点的祖先
            MapNode curAncestor = parentNode;
            // 祖先和祖先的兄弟的共同祖先
            MapNode ancestor = curAncestor.getParentNode();
            List<MapNode> childrenOfAncestor = ancestor.getChildren(pos);
            while ((index = childrenOfAncestor.indexOf(curAncestor)) == 0) {
                curAncestor = ancestor;
                ancestor = curAncestor.getParentNode();
                if (ancestor == null) {
                    return;
                }
                childrenOfAncestor = ancestor.getChildren(pos);
                depth++;
            }

            // 当前节点的兄弟，它的初值是当前节点的祖先的兄弟
            MapNode brother = childrenOfAncestor.get(index - 1);
            while (depth != 0) {
                List<MapNode> childrenOfBrother = brother.getChildren(pos);
                if (childrenOfBrother.isEmpty()) {
                    break;
                }
                brother = childrenOfBrother.get(childrenOfBrother.size() - 1);
                depth--;
            }
            setSelectedNode(brother);
        }
        adjustTranslateY(selectedNode);
    }

    public void moveDown() {
        byte pos = selectedNode.getPos();
        if (pos == PosConstants.MIDDLE) {
            return;
        }

        MapNode parentNode = selectedNode.getParentNode();
        List<MapNode> children = parentNode.getChildren(pos);
        int index = children.indexOf(selectedNode);
        if (index != children.size() - 1) {
            setSelectedNode(children.get(index + 1));
        } else {
            int depth = 1;
            // 当前节点的祖先
            MapNode curAncestor = parentNode;
            MapNode ancestor = curAncestor.getParentNode();
            List<MapNode> childrenOfAncestor = ancestor.getChildren(pos);
            while ((index = childrenOfAncestor.indexOf(curAncestor)) == childrenOfAncestor.size() - 1) {
                curAncestor = ancestor;
                ancestor = curAncestor.getParentNode();
                if (ancestor == null) {
                    return;
                }
                childrenOfAncestor = ancestor.getChildren(pos);
                depth++;
            }

            MapNode brother = childrenOfAncestor.get(index + 1);
            while (depth != 0) {
                List<MapNode> childrenOfBrother = brother.getChildren(pos);
                if (childrenOfBrother.isEmpty()) {
                    break;
                }
                brother = childrenOfBrother.get(0);
                depth--;
            }
            setSelectedNode(brother);
        }
        adjustTranslateY(selectedNode);
    }

    //———————————————————————————————————————————其他———————————————————————————————————————————

    public void undo() {
        commandHistory.undo();
    }

    public void redo() {
        commandHistory.redo();
    }

    private boolean isValidPos(byte pos) {
        if (selectedNode == null || (selectedNode.getPos() != PosConstants.MIDDLE && selectedNode.getPos() != pos)) {
            return true;
        }
        return false;
    }
}