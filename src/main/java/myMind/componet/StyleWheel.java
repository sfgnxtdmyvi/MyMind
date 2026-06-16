package myMind.componet;

import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Pane;
import javafx.stage.Popup;
import myMind.common.manager.CssManager;

import java.io.IOException;

public class StyleWheel extends Popup {
    private StyleWheel() {
        try {
            FXMLLoader loader = new FXMLLoader(StyleWheel.class.getResource(CssManager.getStyleWheel()));
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
