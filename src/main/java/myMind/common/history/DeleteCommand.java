package myMind.common.history;

import myMind.common.constants.NodeConstants;
import myMind.common.constants.PosConstants;
import myMind.componet.MindNode;
import myMind.componet.Subject;
import myMind.controller.SubjectController;

public class DeleteCommand implements Command {
    private final SubjectController subjectController;
    private final Subject subject;
    private final MindNode parent;
    private final MindNode deletedNode;
    private final byte pos;
    private final int index;
    private final boolean keepChildren;

    public DeleteCommand(SubjectController subjectController, MindNode deletedNode, boolean keepChildren) {
        this.subjectController = subjectController;
        this.subject = subjectController.getSubject();
        this.parent = deletedNode.getParentNode();
        this.deletedNode = deletedNode;
        this.pos = deletedNode.getPos();
        // 撤消时，插入原位置
        this.index = (pos == PosConstants.RIGHT) ? parent.getChildrenR().indexOf(deletedNode) : parent.getChildrenL().indexOf(deletedNode);
        this.keepChildren = keepChildren;
    }

    @Override
    public void execute() {
        if (deletedNode == null || deletedNode == subjectController.getRootNode()) {
            return;
        }

        if (pos == PosConstants.RIGHT) {
            if (parent.getChildrenR().size() != 1) {
                subjectController.setSubjectTranslateY(deletedNode.getHeightR() * NodeConstants.TRANSLATE_RATE);
            }
            // 删除 subject 中的子节点
            subjectController.deleteChildrenFromSubjectR(deletedNode);
            subjectController.deleteR(deletedNode);
            subjectController.adjustChildrenYR();
            subjectController.refreshLinesR();
        } else {
            if (parent.getChildrenL().size() != 1) {
                subjectController.setSubjectTranslateY(deletedNode.getHeightL() * NodeConstants.TRANSLATE_RATE);
            }
            subjectController.deleteChildrenFromSubjectL(deletedNode);
            subjectController.deleteL(deletedNode);
            subjectController.adjustChildrenYL();
            subjectController.refreshLinesL();
        }
    }

    @Override
    public void undo() {
        if (pos == PosConstants.RIGHT) {
            undoR(deletedNode);
            parent.addChildRAt(index, deletedNode);
//            subjectController.adjustR(parent);
            subjectController.adjustChildrenYR();
            subjectController.refreshLinesR();
        } else {
            undoL(deletedNode);
            parent.addChildLAt(index, deletedNode);
            subjectController.adjustChildrenYL();
            subjectController.refreshLinesL();
        }

        subjectController.setSelectedNode(deletedNode);
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