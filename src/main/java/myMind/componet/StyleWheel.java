package myMind.componet;

import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Pane;
import javafx.stage.Popup;
import myMind.common.manager.CssManager;

import java.io.IOException;

public class StyleWheel extends Popup {
    // 用静态代码块的话，只能用静态的东西，无法使用继承下来的非静态方法
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
