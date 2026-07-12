package myMind.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import lombok.Setter;
import myMind.common.constants.PosConstants;
import myMind.common.manager.ReferenceManager;
import myMind.componet.MapNode;

import java.util.List;


public class ContextMenuController {
    @Setter
    private SubjectController subjectController;

    @FXML
    private MenuItem collapseOrExpandItem;

    @FXML
    private void onShowing() {
        MapNode selectedNode = subjectController.getSelectedNode();
        List<MapNode> children;
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
    public void reference() {
        ReferenceManager.setSrc(subjectController.getMindMap(),
                subjectController.getSubject(),
                subjectController.getSelectedNode());
    }

    public void dispose() {
        subjectController = null;
    }
}
