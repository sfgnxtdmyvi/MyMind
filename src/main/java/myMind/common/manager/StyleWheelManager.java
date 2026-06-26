package myMind.common.manager;

import javafx.fxml.FXMLLoader;
import javafx.stage.Popup;
import lombok.Getter;

import java.io.IOException;

/**
 * 负责根据配置加载和对外提供样式轮盘
 */
public class StyleWheelManager {

    @Getter
    private static Popup styleWheel;

    static {
        load();
    }

    public static void load() {
        try {
            FXMLLoader loader = new FXMLLoader(StyleWheelManager.class.getResource(CssManager.getStyleWheel()));
            styleWheel = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
