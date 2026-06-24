package myMind.common.history;

import myMind.common.constants.NodeConstants;
import myMind.common.constants.PosConstants;
import myMind.componet.MindNode;
import myMind.componet.Subject;
import myMind.controller.SubjectController;

import java.util.List;

public class DeleteCommand implements Command {
    private final SubjectController subjectController;
    private final Subject subject;
    private final MindNode parentNode;
    private final MindNode deletedNode;

    private boolean keepChildren;
    private final double translateY;
    private final byte pos;
    private final int index;

    public DeleteCommand(SubjectController subjectController, MindNode deletedNode, boolean keepChildren) {
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
            double selfHeight = deletedNode.getPrefHeight();
            if (pos == PosConstants.RIGHT) {
                List<MindNode> childrenR = deletedNode.getChildrenR();
                if (childrenR.isEmpty()) {
                    deleteNotRemain();
                    keepChildren = false;
                    return;
                }

                // 删除节点，子节点成为父节点的子节点
                int i = index;
                for (MindNode childNode : childrenR) {
                    parentNode.addChildRAt(i, childNode);
                    i++;
                }
                // deletedNode 的子节点 List 中仍保留子节点，方便 undo
                subjectController.deleteR(deletedNode);

                subjectController.adjustChildrenXR(parentNode);
                // 删除节点比它的子节点高，才有必要调整 Y
                double childrenHeight = deletedNode.getChildrenHeightR();
                if (selfHeight > childrenHeight) {
                    if (parentNode.getChildrenR().size() != 1) {
                        subjectController.setSubjectTranslateY(selfHeight * NodeConstants.TRANSLATE_RATE);
                    }
                    subjectController.adjustChildrenYR();
                }
                subjectController.refreshLinesR();
                MindNode lastChildR = deletedNode.getLastChildR();
                subjectController.setSelectedNode(lastChildR);
                subjectController.adjustTranslateY(lastChildR);
            } else {
                List<MindNode> childrenL = deletedNode.getChildrenL();
                subjectController.deleteL(deletedNode);
                if (childrenL.isEmpty()) {
                    deleteNotRemain();
                    keepChildren = false;
                    return;
                }

                int i = index;
                for (MindNode childNode : childrenL) {
                    parentNode.addChildLAt(i, childNode);
                    i++;
                }

                subjectController.adjustChildrenXL(parentNode);
                double childrenHeight = deletedNode.getChildrenHeightL();
                if (selfHeight > childrenHeight) {
                    if (parentNode.getChildrenL().size() != 1) {
                        subjectController.setSubjectTranslateY(selfHeight * NodeConstants.TRANSLATE_RATE);
                    }
                    subjectController.adjustChildrenYL();
                }
                subjectController.refreshLinesL();
                MindNode lastChildL = deletedNode.getLastChildL();
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
                List<MindNode> childrenR = deletedNode.getChildrenR();
                if (childrenR.isEmpty()) {
                    return;
                }

                // 删除节点重新插入，该节点在父节点中的子节点删除
                for (MindNode childNode : childrenR) {
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
                List<MindNode> childrenL = deletedNode.getChildrenL();
                if (childrenL.isEmpty()) {
                    return;
                }

                for (MindNode childNode : childrenL) {
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

    private void undoR(MindNode parentNode) {
        subject.addNode(parentNode);
        for (MindNode childNode : parentNode.getChildrenR()) {
            undoR(childNode);
        }
    }

    private void undoL(MindNode parentNode) {
        subject.addNode(parentNode);
        for (MindNode childNode : parentNode.getChildrenL()) {
            undoL(childNode);
        }
    }
}