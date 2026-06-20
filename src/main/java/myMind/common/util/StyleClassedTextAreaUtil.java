package myMind.common.util;

import javafx.geometry.Point2D;
import javafx.scene.input.InputMethodRequests;
import myMind.common.constants.ConfigConstants;
import org.fxmisc.richtext.StyleClassedTextArea;

public class StyleClassedTextAreaUtil {
    public static void setInputMethodRequests(StyleClassedTextArea textArea) {
        textArea.setInputMethodRequests(new InputMethodRequests() {
            @Override
            public Point2D getTextLocation(int offset) {
                Point2D caret = textArea.getCaretBounds()
                        .or(() -> textArea.getCharacterBoundsOnScreen(offset, offset))
                        .map((cb) -> new Point2D(cb.getMaxX() - (double) 5.0F, cb.getMaxY()))
                        .orElseGet(() -> new Point2D(10.0F, 10.0F));
                return new Point2D(caret.getX() * ConfigConstants.SCALE, caret.getY() * ConfigConstants.SCALE);
            }

            @Override
            public String getSelectedText() {
                return getSelectedText();
            }

            @Override
            public int getLocationOffset(int x, int y) {
                return 0;
            }

            @Override
            public void cancelLatestCommittedText() {
            }
        });
    }
}
