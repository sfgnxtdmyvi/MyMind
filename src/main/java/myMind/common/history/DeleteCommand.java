package myMind.common.history;

import myMind.common.constants.NodeConstants;
import myMind.componet.MapNode;
import myMind.componet.Subject;
import myMind.controller.SubjectController;

import java.util.List;

public class DeleteCommand implements Command {
    private final SubjectController subjectController;
    private final Subject subject;
    private final MapNode parentNode;
    private final MapNode deletedNode;

    private boolean keepChildren;
    private final double translateY;
    private final byte pos;
    private final int index;

    public DeleteCommand(SubjectController subjectController, MapNode deletedNode, boolean keepChildren) {
        this.subjectController = subjectController;
        this.subject = subjectController.getSubject();
        this.parentNode = deletedNode.getParentNode();
        this.deletedNode = deletedNode;
        // 是否保留子节点
        this.keepChildren = keepChildren;
        this.translateY = subject.getTranslateY();
        this.pos = deletedNode.getPos();
        // 撤消时，插入原位置
        this.index = parentNode.getChildren(pos).indexOf(deletedNode);
    }

    @Override
    public void execute() {
        if (keepChildren) {
            List<MapNode> children = deletedNode.getChildren(pos);
            if (children.isEmpty()) {
                deleteNotRemain();
                keepChildren = false;
                return;
            }

            // 删除节点，子节点成为父节点的子节点
            int i = index;
            for (MapNode childNode : children) {
                parentNode.addChildAt(i, childNode, pos);
                i++;
            }
            // deletedNode 的子节点 List 中仍保留子节点，方便 undo
            subjectController.delete(deletedNode, pos);

            subjectController.adjustChildrenX(parentNode, pos);
            // 删除前会改变选中节点 -> 失焦事件 -> adjust -> adjustChildrenY
            // 此时 deletedNode 还没删除，但是它的子节点都已经添加到 parentNode 中，子节点有2份，
            // 因此 adjustChildrenY 的结果是错误的，需要再调整一遍
            subjectController.adjustChildrenY(pos);
            subjectController.refreshLines(pos);
            MapNode lastChild = deletedNode.getLastChild(pos);
            subjectController.setSelectedNode(lastChild);
            subjectController.adjustTranslateY(lastChild);
        } else {
            deleteNotRemain();
        }
    }

    private void deleteNotRemain() {
        // 删除空白节点时，这里的调整就够了
        if (parentNode.getChildren(pos).size() != 1) {
            subjectController.setSubjectTranslateY(deletedNode.getHeight(pos) * NodeConstants.TRANSLATE_RATE);
        }
        // 删除 subject 中的子节点
        subjectController.deleteChildrenFromSubject(deletedNode, pos);
        subjectController.delete(deletedNode, pos);
        subjectController.adjustChildrenY(pos);
        subjectController.refreshLines(pos);
        subjectController.adjustTranslateY(subjectController.getSelectedNode());
    }

    @Override
    public void undo() {
        if (keepChildren) {
            double selfHeight = deletedNode.getPrefHeight();
            List<MapNode> children = deletedNode.getChildren(pos);
            if (children.isEmpty()) {
                return;
            }

            // 删除节点重新插入，该节点在父节点中的子节点删除
            for (MapNode childNode : children) {
                parentNode.removeChild(childNode, deletedNode, pos);
            }
            parentNode.addChildAt(index, deletedNode, pos);
            subject.addNode(deletedNode);

            subjectController.adjustChildrenX(parentNode, pos);
            double childrenHeight = deletedNode.getChildrenHeight(pos);
            if (selfHeight > childrenHeight) {
                subjectController.adjustChildrenY(pos);
            }
            subjectController.refreshLines(pos);
        } else {
            undo(deletedNode, pos);
            parentNode.addChildAt(index, deletedNode, pos);
            if (parentNode.getChildren(pos).size() != 1) {
                subjectController.setSubjectTranslateY(-(deletedNode.getHeight(pos) * NodeConstants.TRANSLATE_RATE));
            }
            subjectController.adjustChildrenY(pos);
            subjectController.refreshLines(pos);
        }
        subjectController.setSelectedNode(deletedNode);
        subject.setTranslateY(translateY);
    }

    private void undo(MapNode parentNode, byte pos) {
        subject.addNode(parentNode);
        for (MapNode childNode : parentNode.getChildren(pos)) {
            undo(childNode, pos);
        }
    }
}