package myMind.controller;

import javafx.fxml.FXML;
import lombok.Setter;

public class ContextMenuController {
    @Setter
    private SubjectController subjectController;

    @FXML
    public void copy() {
        subjectController.copy();
    }

    @FXML
    public void cut() {
        subjectController.cut();
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
