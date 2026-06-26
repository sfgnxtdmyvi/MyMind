package myMind.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import lombok.Setter;
import myMind.common.constants.PosConstants;
import myMind.common.manager.QuoteManager;
import myMind.componet.MindNode;
import myMind.componet.Subject;

import java.util.List;


public class ContextMenuController {
    @Setter
    private SubjectController subjectController;

    @FXML
    private MenuItem collapseOrExpandItem;

    @FXML
    private void onShowing() {
        MindNode selectedNode = subjectController.getSelectedNode();
        List<MindNode> children;
        if (selectedNode.getPos() == PosConstants.LEFT) {
            children = selectedNode.getChildrenL();
        } else {
            children = selectedNode.getChildrenR();
        }
        if (children.isEmpty() || children.get(0).isVisible()) {
            Label label = (Label) collapseOrExpandItem.getGraphic();
            label.setText("收起 ▲");
            label.getTooltip().setText("Alt + -");
            collapseOrExpandItem.setUserData("collapse");
        } else {
            Label label = (Label) collapseOrExpandItem.getGraphic();
            label.setText("展开 ▼");
            label.getTooltip().setText("Alt + =");
            collapseOrExpandItem.setUserData("expand");
        }
    }

    @FXML
    public void copy() {
        subjectController.copy();
    }

    @FXML
    public void cut() {
        subjectController.cut();
    }

    public void collapse() {
        subjectController.collapse();
    }

    public void expand() {
        subjectController.expand();
    }

    @FXML
    public void delete() {
        subjectController.delete(false);
    }

    @FXML
    public void deleteRemainChildren() {
        subjectController.delete(true);
    }

    @FXML
    public void deleteEmpty() {
        subjectController.deleteEmpty();
    }

    @FXML
    public void collapseOrExpand() {
        if (collapseOrExpandItem.getUserData().equals("collapse")) {
            subjectController.collapse();
        } else {
            subjectController.expand();
        }
    }

    @FXML
    public void quote() {
        QuoteManager.setQuoting( true);
        QuoteManager.setSrcNode(subjectController.getSelectedNode());
        QuoteManager.setSubjectIndex(subjectController.getMindMap().getSelectionModel().getSelectedIndex());
        Subject subject = subjectController.getSubject();
        QuoteManager.setSubjectTranslateX(subject.getTranslateX());
        QuoteManager.setSubjectTranslateY(subject.getTranslateY());
    }
}
