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
        measureText.setFont(Font.font("System", SizeConstants.NODE_FONT_SIZE));
    }
}
