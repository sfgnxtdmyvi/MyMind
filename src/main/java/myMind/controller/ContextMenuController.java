package myMind.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import lombok.Setter;
import myMind.common.manager.ReferenceManager;
import myMind.componet.MapNode;
import org.fxmisc.richtext.StyleClassedTextArea;

import java.util.List;


public class ContextMenuController {
    @Setter
    private SubjectController subjectController;

    @FXML
    private MenuItem collapseOrExpandItem;
    @FXML
    private MenuItem referenceItem;

    @FXML
    private void onShowing() {
        MapNode selectedNode = subjectController.getSelectedNode();
        List<MapNode> children = selectedNode.getChildren(selectedNode.getPos());
        Label label = (Label) collapseOrExpandItem.getGraphic();
        if (children.isEmpty() || children.get(0).isVisible()) {
            label.setText("收起 ▲");
            label.getTooltip().setText("Alt + -");
            collapseOrExpandItem.setUserData("collapse");
        } else {
            label.setText("展开 ▼");
            label.getTooltip().setText("Alt + =");
            collapseOrExpandItem.setUserData("expand");
        }

        label = (Label) referenceItem.getGraphic();
        if (selectedNode.getOutgoingReference() == null) {
            label.setText("引用");
            label.getTooltip().setText("引用其他节点");
        } else {
            label.setText("取消引用");
            label.getTooltip().setText("取消引用其他节点");
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
        subjectController.deleteNode(false);
    }

    @FXML
    public void deleteRemainChildren() {
        subjectController.deleteNode(true);
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
        MapNode selectedNode = subjectController.getSelectedNode();
        MapNode outgoingNode;
        if ((outgoingNode = selectedNode.getOutgoingReference()) == null) {
            ReferenceManager.setSrc(subjectController.getMindMap(),
                    subjectController.getSubject(),
                    selectedNode);
        } else {
            selectedNode.setOutgoingReference(null);
            StyleClassedTextArea textArea = selectedNode.getTextArea();
            textArea.clearStyle(0, textArea.getText().length());
            outgoingNode.removeIncomingReference(selectedNode);
        }

    }

    public void dispose() {
        subjectController = null;
    }
}
