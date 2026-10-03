package myMind.componet.styledArea;

import javafx.beans.NamedArg;
import javafx.geometry.VPos;
import javafx.scene.Node;
import org.fxmisc.richtext.TextExt;
import org.fxmisc.richtext.model.Codec;
import org.fxmisc.richtext.model.EditableStyledDocument;
import org.fxmisc.richtext.model.SegmentOps;
import org.fxmisc.richtext.model.SimpleEditableStyledDocument;
import org.fxmisc.richtext.model.StyledSegment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public class StyleClassedTextArea extends GenericStyledArea<Collection<String>, String, Collection<String>> {

    public StyleClassedTextArea() {
        this(true);
    }

    public StyleClassedTextArea(@NamedArg("preserveStyle") boolean preserveStyle) {
        this(new SimpleEditableStyledDocument<>(Collections.emptyList(), Collections.emptyList()),
                preserveStyle);
    }

    public StyleClassedTextArea(@NamedArg("document") EditableStyledDocument<Collection<String>, String, Collection<String>> document,
                                @NamedArg("preserveStyle") boolean preserveStyle) {
        super(Collections.emptyList(),
                (paragraph, styleClasses) -> paragraph.getStyleClass().addAll(styleClasses),
                Collections.emptyList(), document, SegmentOps.styledTextOps(), preserveStyle,
                seg -> createStyledTextNode(seg,
                        (text, styleClasses) -> text.getStyleClass().addAll(styleClasses))
        );

        setStyleCodecs(
                Codec.collectionCodec(Codec.STRING_CODEC),
                Codec.styledTextCodec(Codec.collectionCodec(Codec.STRING_CODEC))
        );

        setWrapText(true);
    }

    /**
     * Creates a TextExt node using the given styled text
     */
    public static <S> Node createStyledTextNode(StyledSegment<String, S> seg,
                                                BiConsumer<? super TextExt, S> applyStyle) {
        return createStyledTextNode(seg.getSegment(), seg.getStyle(), applyStyle);
    }

    /**
     * Creates a TextExt node using the given styled text
     */
    public static <S> Node createStyledTextNode(String text, S style,
                                                BiConsumer<? super TextExt, S> applyStyle) {
        TextExt t = new TextExt(text);
        t.setTextOrigin(VPos.TOP);
        t.getStyleClass().add("text");
        applyStyle.accept(t, style);
        return t;
    }

    public void append(String text, String styleClass) {
        insert(getLength(), text, styleClass);
    }

    public void insert(int position, String text, String styleClass) {
        replace(position, position, text, Collections.singleton(styleClass));
    }

    public void replace(int start, int end, String text, String styleClass) {
        replace(start, end, text, Collections.singleton(styleClass));
    }

    public void setStyleClass(int from, int to, String styleClass) {
        setStyle(from, to, Collections.singletonList(styleClass));
    }


    //———————————————————————————————————————————折叠———————————————————————————————————————————

    /**
     * Folds (hides/collapses) paragraphs from startPar to endPar,
     * "into" (i.e. excluding) the first paragraph of the range
     */
    public void foldParagraphs(int startPar, int endPar) {
        foldParagraphs(startPar, endPar, getAddFoldStyle());
    }

    /**
     * Folds (hides/collapses) the currently selected paragraphs,
     * "into" (i.e. excluding) the first paragraph of the range
     */
    public void foldSelectedParagraphs() {
        foldSelectedParagraphs(getAddFoldStyle());
    }

    /**
     * Folds (hides/collapses) paragraphs from character position start to end,
     * "into" (i.e. excluding) the first paragraph of the range
     */
    public void foldText(int start, int end) {
        fold(start, end, getAddFoldStyle());
    }

    public boolean isFolded(int paragraph) {
        return getFoldStyleCheck().test(getParagraph(paragraph).getParagraphStyle());
    }

    /**
     * Unfolds paragraphs startingFrom onwards for the currently folded block
     */
    public void unfoldParagraphs(int startingFromPar) {
        unfoldParagraphs(startingFromPar, getFoldStyleCheck(), getRemoveFoldStyle());
    }

    /**
     * Unfolds text startingFromPos onwards for the currently folded block
     */
    public void unfoldText(int startingFromPos) {
        startingFromPos = offsetToPosition(startingFromPos, Bias.Backward).getMajor();
        unfoldParagraphs(startingFromPos, getFoldStyleCheck(), getRemoveFoldStyle());
    }

    /**
     * @return a Predicate that given a paragraph style, returns true if it includes folding
     */
    protected Predicate<Collection<String>> getFoldStyleCheck() {
        return styleList -> styleList != null && styleList.contains("collapse");
    }

    /**
     * @return a UnaryOperator that given a paragraph style, returns a style that includes fold styling
     */
    protected UnaryOperator<Collection<String>> getAddFoldStyle() {
        return styleList -> {
            styleList = new ArrayList<>(styleList);
            // "collapse" is in styled-text-area.css:
            // .collapse { visibility: false; }
            styleList.add("collapse");
            return styleList;
        };
    }

    /**
     * @return a UnaryOperator that given a paragraph style, returns a style that excludes fold styling
     */
    protected UnaryOperator<Collection<String>> getRemoveFoldStyle() {
        return styleList -> {
            styleList = new ArrayList<>(styleList);
            styleList.remove("collapse");
            return styleList;
        };
    }

}
