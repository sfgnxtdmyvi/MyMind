package myMind.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import lombok.Setter;


public class ContextMenuController {
    @Setter
    private SubjectController subjectController;

    @FXML
    private MenuItem collapseOrExpandItem;

    @FXML
    public void copy() {
        subjectController.copy();
    }

    @FXML
    public void cut() {
        subjectController.cut();
    }

    @FXML
    public void collapseOrExpand() {
        if (collapseOrExpandItem.getUserData().equals("collapse")) {
            collapse();
        }else {
            expand();
        }
    }

    public void collapse() {
        subjectController.collapse();
        Label label = (Label) collapseOrExpandItem.getGraphic();
        label.setText("展开 ▼");
        label.setTooltip(new Tooltip("Alt + ="));
        collapseOrExpandItem.setUserData("expand");
    }

    public void expand() {
        subjectController.expand();
        Label label = (Label) collapseOrExpandItem.getGraphic();
        label.setText("收起 ▲");
        label.setTooltip(new Tooltip("Alt + -"));
        collapseOrExpandItem.setUserData("collapse");
    }

    @FXML
    public void delete() {
        subjectController.delete();
    }

    @FXML
    public void deleteRemainChildren() {
        subjectController.deleteRemainChildren();
    }

    @FXML
    public void deleteEmpty() {
        subjectController.deleteEmpty();
    }
}
