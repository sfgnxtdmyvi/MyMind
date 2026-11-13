package myMind.componet;

import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.stage.Stage;
import myMind.common.constants.IOConstants;
import myMind.common.constants.StyleSheetConstants;

public class PrettyDialog extends Dialog<ButtonType> {

    public PrettyDialog(String title) {
        DialogPane dialogPane = getDialogPane();
        Scene scene = dialogPane.getScene();
        scene.getStylesheets().add(StyleSheetConstants.DIALOG);
        ((Stage) scene.getWindow()).getIcons().add(IOConstants.ICON);
        setTitle(title);
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    }

    public void setButtonTypes(ButtonType... buttonTypes) {
        getDialogPane().getButtonTypes().setAll(buttonTypes);
    }

}
