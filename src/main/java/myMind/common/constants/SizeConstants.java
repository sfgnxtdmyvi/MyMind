package myMind.common.constants;

import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;

public class SizeConstants {

    public static final double SCREEN_WIDTH;
    public static final double SCREEN_HEIGHT;
    static {
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        SCREEN_WIDTH = screenBounds.getWidth();
        SCREEN_HEIGHT = screenBounds.getHeight();
    }

    public static final int MAX_RECENT_FILES = 15;

    // subject 离屏幕的距离
    public static final int SUBJECT_MARGIN = 300;

    public static final int TRANSLATE_MOVE = 50;
    public static final int TRANSLATE_MOVE_TEN = TRANSLATE_MOVE * 10;

}
