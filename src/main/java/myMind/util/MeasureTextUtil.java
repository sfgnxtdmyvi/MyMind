package myMind.util;

import javafx.scene.text.Font;
import javafx.scene.text.Text;
import myMind.constants.SizeConstants;

public class MeasureTextUtil {
    //用于测量文本尺寸
    private static Text measureText;

    static {
        measureText = new Text();
        measureText.setFont(Font.font("Microsoft YaHei", SizeConstants.FONT_SIZE));
    }

    public static double getTextWidth(String text) {
        measureText.setWrappingWidth(SizeConstants.MAX_TEXTAREA_WIDTH);
        measureText.setText(text);
        measureText.setWrappingWidth(0);
        return measureText.getLayoutBounds().getWidth();
    }

    public static double getTextHeight() {
        measureText.setWrappingWidth(SizeConstants.MAX_TEXTAREA_WIDTH);
        return measureText.getLayoutBounds().getHeight();
    }
}
