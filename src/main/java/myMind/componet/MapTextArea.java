package myMind.componet;

import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.input.InputMethodRequests;
import myMind.common.constants.ConfigConstants;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.richtext.model.EditableStyledDocument;
import org.fxmisc.richtext.model.SimpleEditableStyledDocument;

import java.util.Collection;
import java.util.Collections;

public class MapTextArea extends StyleClassedTextArea {

    public MapTextArea() {
        this(true);
    }

    public MapTextArea(boolean preserveStyle) {
        this(new SimpleEditableStyledDocument<>(
                Collections.emptyList(), Collections.emptyList()), preserveStyle);
    }

    public MapTextArea(EditableStyledDocument<Collection<String>, String, Collection<String>> document, boolean preserveStyle) {
        super(document, preserveStyle);
        if (Platform.isFxApplicationThread()) {
            this.initInputMethodHandling();
        } else {
            Platform.runLater(this::initInputMethodHandling);
        }

        getStyleClass().add("text-area");
        setWrapText(true);
        setAutoHeight(true);
    }

    private void initInputMethodHandling() {
        if (Platform.isSupported(ConditionalFeature.INPUT_METHOD)) {
            this.setInputMethodRequests(new InputMethodRequests() {
                public Point2D getTextLocation(int offset) {
                    Point2D caretBounds = getCaretBounds()
                            .or(() -> getCharacterBoundsOnScreen(offset, offset))
                            .map((cb) -> new Point2D(cb.getMaxX() - (double) 5.0F, cb.getMaxY()))
                            .orElseGet(() -> new Point2D(10.0F, 10.0F));
                    return new Point2D(caretBounds.getX() * ConfigConstants.SCALE,
                            caretBounds.getY() * ConfigConstants.SCALE);
                }

                public int getLocationOffset(int x, int y) {
                    return 0;
                }

                public void cancelLatestCommittedText() {
                }

                public String getSelectedText() {
                    return getSelectedText();
                }
            });
        }
    }
}
