package myMind.controller;

import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.input.KeyCombination;
import lombok.Setter;

public class ContextMenuController {
    @Setter
    private SubjectController subjectController;

    @FXML
    private void copy() {
        subjectController.copy();
    }

    @FXML
    private void cut() {
        subjectController.cut();
    }

    @FXML
    private void delete() {
        subjectController.delete();
    }

    public void registerGlobalAccelerators(Scene scene) {
        scene.getAccelerators().put(KeyCombination.keyCombination("Ctrl+Shift+C"), this::copy);
        scene.getAccelerators().put(KeyCombination.keyCombination("Ctrl+Shift+X"), this::cut);
        scene.getAccelerators().put(KeyCombination.keyCombination("Alt+DELETE"), this::delete);
    }
}
