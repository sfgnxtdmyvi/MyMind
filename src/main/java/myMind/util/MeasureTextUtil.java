package myMind.util;

import javafx.scene.text.Font;
import javafx.scene.text.Text;
import lombok.Getter;
import myMind.constants.SizeConstants;

public class MeasureTextUtil {
    //用于测量文本尺寸
    @Getter
    private static Text measureText;
    static {
        measureText = new Text();
        measureText.setFont(Font.font("Microsoft YaHei", SizeConstants.FONT_SIZE));
        measureText.setWrappingWidth(0);
    }

    public static double getTextWidth(String text) {
        measureText.setText(text);
        return measureText.getLayoutBounds().getWidth();
    }
}
