package myMind.componet;

import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Pane;
import javafx.stage.Popup;

import java.io.IOException;
import java.util.ResourceBundle;

public class StyleWheel extends Popup {
    private StyleWheel() {
        try {
            ResourceBundle config = ResourceBundle.getBundle("config");
            FXMLLoader loader = new FXMLLoader(StyleWheel.class.getResource(config.getString("styleWheel")));
            Pane styleWheel = loader.load();
            getContent().add(styleWheel);
            setAutoHide(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static StyleWheel getInstance() {
        return StyleWheelHolder.INSTANCE;
    }

    private static class StyleWheelHolder {
        private static final StyleWheel INSTANCE = new StyleWheel();
    }
}
