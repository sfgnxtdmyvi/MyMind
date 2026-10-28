package myMind.controller;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.IndexRange;
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
import myMind.common.util.FormatUtil;
import myMind.common.util.IdGenerator;
import myMind.common.util.saveUtil;
import myMind.componet.MapNode;
import myMind.componet.MindMap;
import myMind.componet.Subject;
import myMind.componet.styledArea.StyleClassedTextArea;
import org.fxmisc.richtext.model.Paragraph;
import org.fxmisc.richtext.model.TwoDimensional;
import org.reactfx.collection.LiveList;

import java.util.ArrayList;
import java.util.Collection;
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

    private Runnable onSelect;

    public SubjectController(Runnable onSelect) {
        this(new MapNode(PosConstants.MIDDLE), IdGenerator.nextId(), onSelect);
    }

    public SubjectController(MapNode node, long id, Runnable onSelect) {
        subject = new Subject(id);
        rootNode = node;
        rootNode.getStyleClass().add("root-node");
        addNode(node, false);
        selectedNode = rootNode;
        selectedNode.getStyleClass().add("selected-node");
        this.onSelect = onSelect;
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
        // y 轴 + 高度的一半 = selectedNode 的中间，- 新增节点高度的一半 = 新增节点的 y轴
        double y = selectedNode.getLayoutY() + (selectedNode.getHeight() - NodeConstants.MIN_NODE_HEIGHT) / 2.0;
        if (children.isEmpty()) {
            if (selectedNode == rootNode) {
                return;
            }
            MapNode parentNode = selectedNode.getParentNode();

            //以选中文本作为新节点的内容
            StyleClassedTextArea selectedTextArea = selectedNode.getTextArea();
            IndexRange selection = selectedTextArea.getSelection();
            if (selection.getLength() != 0) {
                StyleClassedTextArea textArea = new StyleClassedTextArea();
                textArea.replaceText(selectedTextArea.getSelectedText());
                insertNode = new MapNode(pos, calculateChildX(parentNode, pos), y, textArea);
            } else {
                insertNode = new MapNode(pos, calculateChildX(parentNode, pos), y);
            }

            // 插入节点替代当前节点的位置
            parentNode.addChildAt(parentNode.getChildren(pos).indexOf(selectedNode), insertNode, pos);
            // 当前节点变成插入节点的子节点
            parentNode.removeChild(selectedNode, pos);
            insertNode.addChild(selectedNode, pos);
        } else {
            insertNode = new MapNode(pos, calculateChildX(selectedNode, pos), y);
            // 当前节点的子节点变成插入节点的子节点
            Iterator<MapNode> iterator = children.iterator();
            while (iterator.hasNext()) {
                MapNode node = iterator.next();
                iterator.remove();
                insertNode.addChild(node, pos);
            }
            selectedNode.addChild(insertNode, pos);
        }

        addNode(insertNode, true);
        adjustAsync(insertNode, () -> {
            adjustChildrenX(insertNode, pos);
            refreshLines(pos);
            insertNode.addSizeListener();
        });
        saveLater();
    }

    public void addChild(byte pos) {
        if (isNotValidPos(pos)) {
            return;
        }

        MapNode childNode = new MapNode(pos, calculateChildX(selectedNode, pos), 0);
        selectedNode.addChild(childNode, pos);
        addNode(childNode, true);
        adjustAsync(childNode, () -> {
            adjustChildrenY(pos);
            refreshLines(pos);
            adjustTranslateY(childNode);
            childNode.addSizeListener();
        });
        saveLater();
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
        addNode(siblingNode, true);
        adjustAsync(siblingNode, () -> {
            adjustChildrenY(pos);
            refreshLines(pos);
            adjustTranslateY(siblingNode);
            siblingNode.addSizeListener();
        });
        saveLater();
    }

    /**
     * 批量添加子节点，并选中第一个节点
     */
    public void batchAddChild(byte pos) {
        if (isNotValidPos(pos)) {
            return;
        }

        int index = selectedNode.getChildren(pos).size() - 1;
        batchAddNode(selectedNode, pos, index);
    }

    public void batchAddSibling() {
        if (selectedNode == null || selectedNode == rootNode) {
            return;
        }

        MapNode parentNode = selectedNode.getParentNode();
        byte pos = selectedNode.getPos();
        int index = parentNode.getChildren(pos).indexOf(selectedNode);
        batchAddNode(parentNode, pos, index);
    }

    private void batchAddNode(MapNode parentNode, byte pos, int index) {
        List<MapNode> nodeList = new ArrayList<>(5);
        double x = calculateChildX(parentNode, pos);

        for (int i = 1; i <= 5; i++) {
            MapNode node = new MapNode(pos, x, 0);
            parentNode.addChildAt(index + i, node, pos);
            addNode(node, false);
            nodeList.add(node);
        }
        setSelectedNode(nodeList.get(0), true);

        MapNode lastNode = nodeList.get(4);
        adjustAsync(lastNode, () -> {
            adjustChildrenY(pos);
            refreshLines(pos);
            adjustTranslateY(lastNode);
            nodeList.forEach(MapNode::addSizeListener);
        });
        saveLater();
    }

    public void addNode(MapNode node, boolean needSelecting) {
        subject.addNode(node);
        setOnAction(node);
        if (needSelecting) {
            setSelectedNode(node, true);
        }
    }

    private void setOnAction(MapNode node) {
        node.setOnAction(event -> {
            byte pos = node.getPos();
            switch (event) {
                case CLICK -> {
                    setSelectedNode(node, true);
                    if (ReferenceManager.isReferencing()) {
                        ReferenceManager.setReferencing(false);

                        // 添加引用
                        MapNode srcNode = ReferenceManager.getSrcNode();
                        srcNode.setOutgoingReference(node);
                        StyleClassedTextArea textArea = srcNode.getTextArea();
                        textArea.setStyle(0, textArea.getText().length(), Collections.singletonList("quote"));

                        node.addIncomingReference(srcNode);
                        node.setSubjectId(subject.getSubjectId());
                        saveLater();
                    } else {
                        MapNode cloneNode = CloneNodeUtil.getCloneNode();
                        if (cloneNode != null) {
                            pasteSibling(cloneNode, pos);
                        }
                    }
                }
                case JUMP -> {
                    if (node.getOutgoingReference() != null) {
                        setSelectedNode(node, true);
                        MapNode targetNode = node.getOutgoingReference();
                        // 不延迟会有 bug，node 的 textArea 会获得两次焦点
                        // node 获取（因为点击） -> node 失去（targetNode 获取）->
                        // targetNode 获取（跳转） -> targetNode 失去（node 再次获取） -> node 再次获取（未知 bug）
                        Platform.runLater(() -> getMindMap().jump(targetNode.getSubjectId(), targetNode));
                    }
                }

                case ADD_BUTTON_R -> {
                    setSelectedNode(node, true);
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
                    setSelectedNode(node, true);
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

                case ADJUST_X -> {
                    if (pos == PosConstants.MIDDLE) {
                        adjustChildrenX(node, PosConstants.RIGHT);
                        adjustChildrenX(node, PosConstants.LEFT);
                        refreshLines();
                    } else {
                        adjustChildrenX(node, pos);
                        refreshLines(pos);
                    }
                }
                case ADJUST_Y -> {
                    adjustChildrenY(pos);
                    refreshLines(pos);
                }

                case CHANGE -> saveLater();
            }
        });

        node.setSetSubjectTranslateY(this::setSubjectTranslateY);
        node.setSetSubjectTranslateX(this::setSubjectTranslateX);
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
            return parentNode.getLayoutX() + parentNode.getWidth() + NodeConstants.GAP_X;
        } else {
            // 父节点 X 轴 - 节点间隔 - 节点最小宽度
            return parentNode.getLayoutX() - NodeConstants.ADD_LEFT_NODE_GAP_X;
        }
    }

    /**
     * 不是根节点，且节点的 pos 和传入的 pos 不一致，则非法
     */
    private boolean isNotValidPos(byte pos) {
        return selectedNode == null || (selectedNode.getPos() != PosConstants.MIDDLE && selectedNode.getPos() != pos);
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
        saveLater();
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
        if (selectedNode == null) {
            return;
        }
        // 不放回去会丢失 cloneNode
        if (selectedNode == rootNode) {
            CloneNodeUtil.setCloneNode(cloneNode);
            return;
        }
        MapNode parentNode = selectedNode.getParentNode();
        paste(parentNode, cloneNode, parentNode.getChildren(pos).indexOf(selectedNode), pos);
    }

    /**
     * @param index -1 表示添加到最后
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
        setSelectedNode(cloneNode, true);

        // 如果有节点引用 cloneNode，更新 subjectId
        if (cloneNode.getSubjectId() != 0) {
            cloneNode.setSubjectId(subject.getSubjectId());
        }

        adjustChildrenXY(parentNode, pos);
        adjustTranslateY(cloneNode);
        saveLater();
    }

    //———————————————————————————————————————————删除———————————————————————————————————————————

    /**
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
        saveLater();
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
            setSelectedNode(parent, true);
        } else {
            int index = children.indexOf(toDelete);
            if (index != children.size() - 1) {
                setSelectedNode(children.get(index + 1), true);
            } else {
                setSelectedNode(children.get(index - 1), true);
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

    //—————————————————————————————————————————文本处理—————————————————————————————————————————

    /**
     * 格式化
     */
    public void format() {
        StyleClassedTextArea textArea = selectedNode.getTextArea();
        IndexRange selection = textArea.getSelection();
        // 没有选中文本则格式化全部，否则只格式化选中的文本
        if (selection.getLength() == 0) {
            textArea.selectAll();
            String selectedText = textArea.getSelectedText();
            textArea.replaceText(FormatUtil.format(selectedText));
        } else {
            String selectedText = textArea.getSelectedText();
            String formatedText = FormatUtil.format(selectedText);
            textArea.replaceText(selection.getStart(), selection.getEnd(), formatedText);
        }
    }

    /**
     * 分割节点
     * 切割选中文本到新节点
     * 冒号左边的保留在原节点，冒号右边的移到新节点
     */
    public void split() {
        StyleClassedTextArea textArea = selectedNode.getTextArea();
        IndexRange selection = textArea.getSelection();
        String newNodeText;

        if (selection.getLength() != 0) {
            newNodeText = textArea.getSelectedText();
            textArea.replaceText(selection.getStart(), selection.getEnd(), "");
        } else {
            String[] split = FormatUtil.split(textArea.getText());
            if (split == null) {
                return;
            }
            textArea.replaceText(split[0]);
            newNodeText = split[1];
        }

        StyleClassedTextArea childTextArea = new StyleClassedTextArea();
        childTextArea.replaceText(newNodeText);
        byte pos = selectedNode.getPos();
        MapNode newNode = new MapNode(pos, calculateChildX(selectedNode, pos), 0, childTextArea);

        selectedNode.addChild(newNode, pos);
        addNode(newNode, false);
        adjustChildrenX(selectedNode, pos);
        adjustChildrenY(pos);
        refreshLines(pos);
    }

    /**
     * 向下复制一行，或复制选中文本
     */
    public void copyLine() {
        StyleClassedTextArea textArea = selectedNode.getTextArea();
        int caretPos = textArea.getCaretPosition();

        IndexRange selection = textArea.getSelection();
        if (selection.getLength() == 0) {
            textArea.selectLine();
        }
        selection = textArea.getSelection();

        String selectedText = textArea.getSelectedText();
        textArea.replaceText(selection.getStart(), selection.getEnd(), selectedText + "\n" + selectedText);
        textArea.moveTo(caretPos + selectedText.length() + 1);
    }

    /**
     * 删除当前行，保持光标不变
     */
    public void deleteLine() {
        StyleClassedTextArea textArea = selectedNode.getTextArea();

        int caretPos = textArea.getCaretPosition();
        TwoDimensional.Position pos = textArea.offsetToPosition(caretPos, TwoDimensional.Bias.Forward);
        int rowIndex = pos.getMajor();
        int columnIndex = pos.getMinor();

        LiveList<Paragraph<Collection<String>, String, Collection<String>>> paragraphs = textArea.getParagraphs();
        // 当前行的起始偏移量
        int start = 0;
        for (int i = 0; i < rowIndex; i++) {
            start += paragraphs.get(i).length();
        }
        // 当前行的结束偏移量（包含换行符）
        int end = start + paragraphs.get(rowIndex).length() + 1;

        if (paragraphs.size() - 1 == rowIndex) {
            // 当前是最后一行，移动到上一行
            rowIndex = rowIndex - 1;
            // 上一行的长度小于当前列索引，将列索引设为上一行的末尾
            if (paragraphs.size() != 1 && paragraphs.get(rowIndex).length() < columnIndex) {
                columnIndex = paragraphs.get(rowIndex).length();
            }
        }
        // 下一行的长度小于当前列索引，将列索引设为下一行的末尾
        else if (paragraphs.size() != 1 && paragraphs.get(rowIndex + 1).length() < columnIndex) {
            columnIndex = paragraphs.get(rowIndex + 1).length();
        }

        textArea.deleteText(start, end);

        if (textArea.getLength() == 0) {
            return;
        }
        textArea.moveTo(rowIndex, columnIndex);
    }

    //———————————————————————————————————————————调整节点坐标———————————————————————————————————————————

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
                child.setLayoutX(childX - child.getWidth());
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
            double parentMidY = parentNode.getLayoutY() + parentNode.getHeight() / 2.0;
            childY = parentMidY - totalHeight / 2.0;
        } else {
            childY = y;
        }

        for (MapNode childNode : children) {
            if (!childNode.isVisible()) {
                continue;
            }
            List<MapNode> childrenOfChild = childNode.getChildren(pos);

            double selfHeight = childNode.getHeight();
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

    /**
     * 在 node 得到真正的高度后，进行调整，并添加尺寸监听
     */
    public void adjustAsync(MapNode node, final Runnable runnable) {
        ChangeListener<Number> changeListener = new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Number> obs, Number oldVal, Number newVal) {
                obs.removeListener(this);
                runnable.run();
            }
        };
        node.heightProperty().addListener(changeListener);
    }

    //———————————————————————————————————————————调整 subject 的位置———————————————————————————————————————————

    /**
     * 调整节点与 scene 下面和上面的间距
     */
    public void adjustTranslateY(MapNode mapNode) {
        // 定位到 tabHeaderArea 下面的位置
        MindMap mindMap = getMindMap();
        StackPane tabHeaderArea = (StackPane) mindMap.lookup(".tab-header-area");
        Point2D mindMapPoint = mindMap.localToScene(0, tabHeaderArea.getHeight());

        Point2D sceneCoords = mapNode.localToScene(0, 0);
        double mapNodeY = sceneCoords.getY();
        if (mapNodeY < mindMapPoint.getY()) {
            setSubjectTranslateY(mindMapPoint.getY() - mapNodeY);
        } else if (mapNode.getScene().getHeight() < mapNodeY + mapNode.getHeight()) {
            double dy = mapNodeY + mapNode.getHeight() - mapNode.getScene().getHeight();
            setSubjectTranslateY(-dy);
        }
    }

    public void adjustTranslateX(MapNode mapNode) {
        Point2D sceneCoords = mapNode.localToScene(0, 0);
        double mapNodeX = sceneCoords.getX();
        if (mapNodeX < 0) {
            setSubjectTranslateX(-mapNodeX);
        } else if (mapNode.getScene().getWidth() < mapNodeX + mapNode.getWidth()) {
            double dx = mapNodeX + mapNode.getWidth() - mapNode.getScene().getWidth();
            setSubjectTranslateX(-dx);
        }
    }

    public void toCenter(MapNode mapNode) {
        Point2D sceneCoords = mapNode.localToScene(0, 0);
        double mapNodeX = sceneCoords.getX();
        double mapNodeY = sceneCoords.getY();

        MindMap mindMap = getMindMap();
        StackPane tabHeaderArea = (StackPane) mindMap.lookup(".tab-header-area");
        double mindMapHeight = mapNode.getScene().getHeight() - tabHeaderArea.getHeight();
        double centerX = (mapNode.getScene().getWidth() - mapNode.getWidth()) / 2;
        double centerY = (mindMapHeight - mapNode.getHeight()) / 2;

        setSubjectTranslateX(centerX - mapNodeX);
        setSubjectTranslateY(centerY - mapNodeY);
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
            x = node.getLayoutX() + node.getWidth() - getLevel(maxIndex - Math.min(i, maxIndex - i));
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
            return new Point2D(node.getLayoutX() + node.getWidth(), getMidY(node));
        }
    }

    private double getMidY(MapNode node) {
        return node.getLayoutY() + node.getHeight() / 2;
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

    public void setSelectedNode(MapNode node, boolean needRecording) {
        // 保证文本框在任何情况下都能获得焦点
        node.getTextArea().requestFocus();
        if (selectedNode == node) {
            return;
        }
        selectedNode.getStyleClass().remove("selected-node");
        selectedNode = node;
        selectedNode.getStyleClass().add("selected-node");
        if (needRecording) {
            onSelect.run();
        }
    }

    public void moveRight() {
        // 左边节点 -> 父节点
        // 根、右边节点 -> 中间的右子节点
        if (selectedNode.getPos() == PosConstants.LEFT) {
            setSelectedNode(selectedNode.getParentNode(), true);
            adjustTranslateX(selectedNode);
        } else {
            List<MapNode> children = selectedNode.getChildrenR();
            if (!children.isEmpty()) {
                setSelectedNode(children.get(children.size() / 2), true);
                adjustTranslateX(selectedNode);
            }
        }
    }

    public void moveLeft() {
        // 父节点 <- 右边节点
        // 中间的左子节点 <- 左边、根节点
        if (selectedNode.getPos() == PosConstants.RIGHT) {
            setSelectedNode(selectedNode.getParentNode(), true);
            adjustTranslateX(selectedNode);
        } else {
            List<MapNode> children = selectedNode.getChildrenL();
            if (!children.isEmpty()) {
                setSelectedNode(children.get(children.size() / 2), true);
                adjustTranslateX(selectedNode);
            }
        }
    }

    public void moveUp() {
        move(-1);
    }

    public void moveDown() {
        move(1);
    }

    private void move(int dir) {
        if (selectedNode == rootNode) {
            return;
        }

        byte pos = selectedNode.getPos();
        MapNode parentNode = selectedNode.getParentNode();
        List<MapNode> children = parentNode.getChildren(pos);
        int index = children.indexOf(selectedNode);
        // 边界索引：上移看第一个，下移看最后一个
        int boundaryIndex = (dir == -1) ? 0 : children.size() - 1;

        if (index != boundaryIndex) {
            // 有相邻兄弟，直接选中
            setSelectedNode(children.get(index + dir), true);
        } else {
            // 没有相邻兄弟，需要跨层找“祖先的兄弟”
            int depth = 1;
            MapNode ancestor = parentNode;
            if (ancestor == rootNode) {
                return;
            }
            // 祖先和祖先的兄弟的共同祖先
            MapNode commoAncestor = ancestor.getParentNode();
            List<MapNode> childrenOfAncestor = commoAncestor.getChildren(pos);
            int boundary = (dir == -1) ? 0 : childrenOfAncestor.size() - 1;

            // 向上找，直到找到一个有兄弟的祖先
            while ((index = childrenOfAncestor.indexOf(ancestor)) == boundary) {
                ancestor = commoAncestor;
                commoAncestor = ancestor.getParentNode();
                if (commoAncestor == null) {
                    return;
                }
                childrenOfAncestor = commoAncestor.getChildren(pos);
                boundary = (dir == -1) ? 0 : childrenOfAncestor.size() - 1;
                depth++;
            }

            // 当前节点的兄弟，它的初值是当前节点的祖先的兄弟
            MapNode brother = childrenOfAncestor.get(index + dir);
            // 深入祖先的兄弟的子树，上移取最后一个孩子，下移取第一个孩子
            while (depth != 0) {
                List<MapNode> childrenOfBrother = brother.getChildren(pos);
                if (childrenOfBrother.isEmpty()) {
                    break;
                }
                int childIndex = (dir == -1) ? childrenOfBrother.size() - 1 : 0;
                brother = childrenOfBrother.get(childIndex);
                depth--;
            }
            setSelectedNode(brother, true);
        }
        adjustTranslateY(selectedNode);
    }

    //—————————————————————————————————————————移动节点—————————————————————————————————————————

    public void moveNodeUp() {
        moveNode(-1);
    }

    public void moveNodeDown() {
        moveNode(1);
    }

    private void moveNode(int dir) {
        if (selectedNode == rootNode) {
            return;
        }

        byte pos = selectedNode.getPos();
        MapNode parentNode = selectedNode.getParentNode();
        List<MapNode> children = parentNode.getChildren(pos);
        int index = children.indexOf(selectedNode);
        int targetIndex = index + dir;
        if (targetIndex < 0 || targetIndex >= children.size()) {
            return;
        }

        children.remove(index);
        children.add(targetIndex, selectedNode);

        adjustChildrenY(parentNode, null, pos);
        refreshLines(pos);
        saveLater();
    }

    //———————————————————————————————————————————其他———————————————————————————————————————————

    public void undo() {
        commandHistory.undo();
        saveLater();
    }

    public void redo() {
        commandHistory.redo();
    }

    public MindMap getMindMap() {
        return (MindMap) getSubject().getParent().getParent();
    }

    public void saveLater() {
        saveUtil.saveLater(getMindMap());
    }
}