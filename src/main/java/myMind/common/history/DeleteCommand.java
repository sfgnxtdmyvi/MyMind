package myMind.common.history;

import myMind.common.constants.NodeConstants;
import myMind.common.constants.PosConstants;
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
        this.index = (pos == PosConstants.RIGHT) ? parentNode.getChildrenR().indexOf(deletedNode) : parentNode.getChildrenL().indexOf(deletedNode);
    }

    @Override
    public void execute() {
        if (keepChildren) {
            if (pos == PosConstants.RIGHT) {
                List<MapNode> childrenR = deletedNode.getChildrenR();
                if (childrenR.isEmpty()) {
                    deleteNotRemain();
                    keepChildren = false;
                    return;
                }

                // 删除节点，子节点成为父节点的子节点
                int i = index;
                for (MapNode childNode : childrenR) {
                    parentNode.addChildRAt(i, childNode);
                    i++;
                }
                // deletedNode 的子节点 List 中仍保留子节点，方便 undo
                subjectController.deleteR(deletedNode);

                subjectController.adjustChildrenXR(parentNode);
                // 删除前会改变选中节点 -> 失焦事件 -> adjust -> adjustChildrenY
                // 此时 deletedNode 还没删除，但是它的子节点都已经添加到 parentNode 中，子节点有2份，
                // 因此 adjustChildrenY 的结果是错误的，需要再调整一遍
                subjectController.adjustChildrenYR();
                subjectController.refreshLinesR();
                MapNode lastChildR = deletedNode.getLastChildR();
                subjectController.setSelectedNode(lastChildR);
                subjectController.adjustTranslateY(lastChildR);
            } else {
                List<MapNode> childrenL = deletedNode.getChildrenL();
                subjectController.deleteL(deletedNode);
                if (childrenL.isEmpty()) {
                    deleteNotRemain();
                    keepChildren = false;
                    return;
                }

                int i = index;
                for (MapNode childNode : childrenL) {
                    parentNode.addChildLAt(i, childNode);
                    i++;
                }

                subjectController.adjustChildrenXL(parentNode);
                subjectController.adjustChildrenYL();
                subjectController.refreshLinesL();
                MapNode lastChildL = deletedNode.getLastChildL();
                subjectController.setSelectedNode(lastChildL);
                subjectController.adjustTranslateY(lastChildL);
            }
        } else {
            deleteNotRemain();
        }
    }

    private void deleteNotRemain() {
        if (pos == PosConstants.RIGHT) {
            // 删除空白节点时，这里的调整就够了
            if (parentNode.getChildrenR().size() != 1) {
                subjectController.setSubjectTranslateY(deletedNode.getHeightR() * NodeConstants.TRANSLATE_RATE);
            }
            // 删除 subject 中的子节点
            subjectController.deleteChildrenFromSubjectR(deletedNode);
            subjectController.deleteR(deletedNode);
            subjectController.adjustChildrenYR();
            subjectController.refreshLinesR();
        } else {
            if (parentNode.getChildrenL().size() != 1) {
                subjectController.setSubjectTranslateY(deletedNode.getHeightL() * NodeConstants.TRANSLATE_RATE);
            }
            subjectController.deleteChildrenFromSubjectL(deletedNode);
            subjectController.deleteL(deletedNode);
            subjectController.adjustChildrenYL();
            subjectController.refreshLinesL();
        }
        subjectController.adjustTranslateY(subjectController.getSelectedNode());
    }

    @Override
    public void undo() {
        if (keepChildren) {
            double selfHeight = deletedNode.getPrefHeight();
            if (pos == PosConstants.RIGHT) {
                List<MapNode> childrenR = deletedNode.getChildrenR();
                if (childrenR.isEmpty()) {
                    return;
                }

                // 删除节点重新插入，该节点在父节点中的子节点删除
                for (MapNode childNode : childrenR) {
                    parentNode.undoR(childNode, deletedNode);
                }
                parentNode.addChildRAt(index, deletedNode);
                subject.addNode(deletedNode);

                subjectController.adjustChildrenXR(parentNode);
                double childrenHeight = deletedNode.getChildrenHeightR();
                if (selfHeight > childrenHeight) {
                    subjectController.adjustChildrenYR();
                }
                subjectController.refreshLinesR();
            } else {
                List<MapNode> childrenL = deletedNode.getChildrenL();
                if (childrenL.isEmpty()) {
                    return;
                }

                for (MapNode childNode : childrenL) {
                    parentNode.undoL(childNode, deletedNode);
                }
                parentNode.addChildLAt(index, deletedNode);
                subject.addNode(deletedNode);

                subjectController.adjustChildrenXL(parentNode);
                double childrenHeight = deletedNode.getChildrenHeightL();
                if (selfHeight > childrenHeight) {
                    subjectController.adjustChildrenYL();
                }
                subjectController.refreshLinesL();
            }
        } else {
            if (pos == PosConstants.RIGHT) {
                undoR(deletedNode);
                parentNode.addChildRAt(index, deletedNode);
                if (parentNode.getChildrenR().size() != 1) {
                    subjectController.setSubjectTranslateY(-(deletedNode.getHeightR() * NodeConstants.TRANSLATE_RATE));
                }
                subjectController.adjustChildrenYR();
                subjectController.refreshLinesR();
            } else {
                undoL(deletedNode);
                parentNode.addChildLAt(index, deletedNode);
                if (parentNode.getChildrenL().size() != 1) {
                    subjectController.setSubjectTranslateY(-(deletedNode.getHeightL() * NodeConstants.TRANSLATE_RATE));
                }
                subjectController.adjustChildrenYL();
                subjectController.refreshLinesL();
            }
        }
        subjectController.setSelectedNode(deletedNode);
        subject.setTranslateY(translateY);
    }

    private void undoR(MapNode parentNode) {
        subject.addNode(parentNode);
        for (MapNode childNode : parentNode.getChildrenR()) {
            undoR(childNode);
        }
    }

    private void undoL(MapNode parentNode) {
        subject.addNode(parentNode);
        for (MapNode childNode : parentNode.getChildrenL()) {
            undoL(childNode);
        }
    }
}