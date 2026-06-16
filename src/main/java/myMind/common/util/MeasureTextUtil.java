package myMind.common.util;

import javafx.scene.text.Font;
import javafx.scene.text.Text;
import myMind.common.constants.NodeConstants;

public class MeasureTextUtil {
    //用于测量文本尺寸
    private static final Text measureText;

    static {
        measureText = new Text();
        measureText.setFont(Font.font("Microsoft YaHei", NodeConstants.FONT_SIZE));
    }

    public static double getTextWidth(String text) {
        measureText.setWrappingWidth(NodeConstants.MAX_TEXTAREA_WIDTH);
        measureText.setText(text);
        measureText.setWrappingWidth(0);
        return measureText.getLayoutBounds().getWidth();
    }

    public static double getTextHeight() {
        measureText.setWrappingWidth(NodeConstants.MAX_TEXTAREA_WIDTH);
        return measureText.getLayoutBounds().getHeight();
    }
}
