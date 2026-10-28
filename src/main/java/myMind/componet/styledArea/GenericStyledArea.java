package myMind.componet.styledArea;

import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.beans.NamedArg;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.css.StyleConverter;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.IndexRange;
import javafx.scene.input.InputMethodEvent;
import javafx.scene.input.InputMethodRequests;
import javafx.scene.input.InputMethodTextRun;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.TextFlow;
import myMind.common.constants.ConfigConstants;
import myMind.common.constants.NodeConstants;
import org.fxmisc.flowless.Cell;
import org.fxmisc.flowless.VirtualFlow;
import org.fxmisc.flowless.VirtualFlowHit;
import org.fxmisc.flowless.Virtualized;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CharacterHit;
import org.fxmisc.richtext.CustomStyleableProperty;
import org.fxmisc.richtext.StyleActions;
import org.fxmisc.richtext.StyledTextArea;
import org.fxmisc.richtext.UndoActions;
import org.fxmisc.richtext.event.MouseOverTextEvent;
import org.fxmisc.richtext.model.Codec;
import org.fxmisc.richtext.model.EditableStyledDocument;
import org.fxmisc.richtext.model.GenericEditableStyledDocument;
import org.fxmisc.richtext.model.Paragraph;
import org.fxmisc.richtext.model.PlainTextChange;
import org.fxmisc.richtext.model.ReadOnlyStyledDocument;
import org.fxmisc.richtext.model.Replacement;
import org.fxmisc.richtext.model.RichTextChange;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyledDocument;
import org.fxmisc.richtext.model.StyledSegment;
import org.fxmisc.richtext.model.TextOps;
import org.fxmisc.richtext.model.TwoDimensional;
import org.fxmisc.richtext.model.TwoLevelNavigator;
import org.fxmisc.richtext.util.SubscribeableContentsObsSet;
import org.fxmisc.undo.UndoManager;
import org.reactfx.EventStream;
import org.reactfx.EventStreams;
import org.reactfx.Guard;
import org.reactfx.Subscription;
import org.reactfx.Suspendable;
import org.reactfx.SuspendableEventStream;
import org.reactfx.SuspendableNo;
import org.reactfx.collection.LiveList;
import org.reactfx.collection.SuspendableList;
import org.reactfx.util.Tuple2;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import static org.reactfx.EventStreams.invalidationsOf;
import static org.reactfx.EventStreams.merge;
import static org.reactfx.util.Tuples.t;

/**
 * 渲染并编辑 {@link EditableStyledDocument} 的文本编辑控件。提供 API 为一段文本指定样式，适用于语法高亮和富文本编辑器
 *
 * <h3>设置 {@link UndoManager}</h3>
 * 默认的 UndoManager 可以撤销/重做 {@link PlainTextChange} 或 {@link RichTextChange}。
 * 如果要创建自己的专用版本，撤销/重做其他的 TextChange，请使用 {@link UndoUtils} 中便捷的工厂方法
 *
 * <h3>重写默认键盘行为</h3>
 * 本类使用 {@link javafx.scene.input.KeyEvent#KEY_TYPED KEY_TYPED} 处理普通字符输入，
 * 使用 {@link javafx.scene.input.KeyEvent#KEY_PRESSED KEY_PRESSED} 处理控制键组合（包括 Enter 和 Tab）。
 * 如果要添加或重写某些键盘快捷键（其余快捷键不变），
 * 你需要将默认事件处理器与一个新的处理器组合起来，由新处理器添加或重写某些默认键组合
 * <p>例如：将 Ctrl+S 绑定到 save()</p>
 * <pre><code>
 * import static javafx.scene.input.KeyCode.*;
 * import static javafx.scene.input.KeyCombination.*;
 * import static org.fxmisc.wellbehaved.event.EventPattern.*;
 * import static org.fxmisc.wellbehaved.event.InputMap.*;
 * import org.fxmisc.wellbehaved.event.Nodes;
 *
 * Nodes.addInputMap(area, consume(keyPressed(S, CONTROL_DOWN), event -> save()));
 * </code></pre>
 *
 * <h3>重写默认鼠标行为</h3>
 * 默认鼠标行为会处理自动滚动和将选中文本拖拽到新位置，因此，某些部分无法在不影响其他行为的情况下部分重写。
 * <p>
 * 以下列出了一些 {@link org.fxmisc.wellbehaved.event.EventPattern}，无法在不对默认鼠标行为造成负面影响的情况下重写它们，
 * 以及描述了如何以特殊方式安全地重写它们，而不破坏自动滚动
 * <ul>
 *     <li>
 *         <em>左键单击事件：</em>
 *         (<code>EventPattern.mousePressed(MouseButton.PRIMARY).onlyIf(e -> e.getClickCount() == 1)</code>)不要重写，
 *         改用{@link #onOutsideSelectionMousePressed}, {@link #onInsideSelectionMousePressReleased}，或参见下一项
 *     </li>
 *     <li>
 *         <em>所有其他鼠标按下事件 (例如，双击左键)：</em>
 *         除了隐藏上下文菜单外（请在重写 InputMap 的适当位置使用 hideContextMenu() 以维持此行为），
 *         其他的都可以通过{@link org.fxmisc.wellbehaved.event.template.InputMapTemplate InputMapTemplate 的工厂方法}，
 *         或{@link org.fxmisc.wellbehaved.event.InputMap InputMap 的工厂方法}安全地重写
 *     </li>
 *     <li>
 *         <em>纯粹的左键拖拽检测事件（仅点击左键，而不是左键跟右键或滚轮一起点击）：</em>
 *         (<code>
 *             EventPattern.eventType(MouseEvent.DRAG_DETECTED).onlyIf(e -> e.getButton() == MouseButton.PRIMARY && !e.isMiddleButtonDown() && !e.isSecondaryButtonDown())
 *         </code>)
 *         不要重写，改用{@link #onNewSelectionDrag}或{@link #onSelectionDrag}.
 *     </li>
 *     <li>
 *         <em>纯粹的左键拖拽事件：</em>
 *         (<code>
 *             EventPattern.mouseDragged().onlyIf(e -> e.getButton() == MouseButton.PRIMARY && !e.isMiddleButtonDown() && !e.isSecondaryButtonDown())
 *         </code>)
 *         不要重写，但请参见下一项
 *     </li>
 *     <li>
 *         <em>其他鼠标拖拽事件：</em>
 *         仅当使用 process InputMaps (
 *         {@link org.fxmisc.wellbehaved.event.template.InputMapTemplate#process(javafx.event.EventType, BiFunction)},
 *         {@link org.fxmisc.wellbehaved.event.template.InputMapTemplate#process(org.fxmisc.wellbehaved.event.EventPattern, BiFunction)},
 *         {@link org.fxmisc.wellbehaved.event.InputMap#process(javafx.event.EventType, Function)}，
 *         或{@link org.fxmisc.wellbehaved.event.InputMap#process(org.fxmisc.wellbehaved.event.EventPattern, Function)})
 *         并且返回{@link org.fxmisc.wellbehaved.event.InputHandler.Result#PROCEED}时，
 *         才能使用不同的 {@link org.fxmisc.wellbehaved.event.EventPattern} 安全地重写其他鼠标拖拽事件，而不影响默认行为。
 *         <p>
 *         本类有一个兜底的鼠标拖拽 InputMap：当鼠标拖拽事件发生在组件边界之外时，会朝鼠标位置自动滚动；
 *         当事件发生在组件内部时，它会停止自动滚动。
 *         但是，只有当事件在到达该 InputMap 之前未被消费时，才能起作用。
 *         为确保自动滚动功能启用，请在你的 process InputMap 中将 {@link #isAutoScrollOnDragDesired()} 设置为 true。
 *         如果不需要该功能，请在 process InputMap 中将其设置为 false。
 *         <em>注意：由于这个 InputMap，所有鼠标拖拽事件都会被消费</em>
 *         <p>
 *         InputMap 有三种：consume，把事件消费掉，不再向下传递；
 *         ignore，忽略事件，不消费，继续传递；
 *         process，交给 action 处理，action 返回一个 Result 来决定事件是否继续传递
 *     </li>
 *     <li>
 *         <em>纯粹的左键释放事件：</em>
 *         (<code>
 *             EventPattern.mouseReleased().onlyIf(e -> e.getButton() == MouseButton.PRIMARY && !e.isMiddleButtonDown() && !e.isSecondaryButtonDown())
 *         </code>)
 *         不要重写，改用{@link #onNewSelectionDragFinished}, {@link #onSelectionDropped}，或参见下一项
 *     </li>
 *     <li>
 *         <em>其他的鼠标释放事件：</em>
 *         仅当使用 process InputMaps (
 *         {@link org.fxmisc.wellbehaved.event.template.InputMapTemplate#process(javafx.event.EventType, BiFunction)},
 *         {@link org.fxmisc.wellbehaved.event.template.InputMapTemplate#process(org.fxmisc.wellbehaved.event.EventPattern, BiFunction)},
 *         {@link org.fxmisc.wellbehaved.event.InputMap#process(javafx.event.EventType, Function)}，
 *         或{@link org.fxmisc.wellbehaved.event.InputMap#process(org.fxmisc.wellbehaved.event.EventPattern, Function)})
 *         并且返回{@link org.fxmisc.wellbehaved.event.InputHandler.Result#PROCEED}时，
 *         才能使用不同的 {@link org.fxmisc.wellbehaved.event.EventPattern} 安全地重写其他鼠标释放事件，而不影响默认行为
 *         <p>
 *         本类有一个兜底的鼠标释放 InputMap：在滚动时停止自动滚动
 *         但是，只有当事件在到达该 InputMap 之前未被消费时，才能起作用。
 *         <em>注意：由于这个 InputMap，所有鼠标释放事件都会被消费</em>
 *     </li>
 * </ul>
 *
 * <h3>CSS、样式类和伪类</h3>
 * 请参阅
 * <a
 * href="https://github.com/FXMisc/RichTextFX/wiki/RichTextFX-CSS-Reference-Guide">RichTextFX CSS 参考指南
 * </a>
 *
 * <h3>Area Actions and Other Operations</h3>
 * 为了区分可以在此组件上执行的实际操作与样板方法（例如属性的 getter/setter 等），
 * 看本类实现的接口，每个接口都列出了一类方法
 * <p>要在一次调用中更新底层文档的多个部分，看{@link #createMultiChange()}</p>
 *
 * <h3>计算位置</h3>
 * 要计算位置或索引，请阅读{@link org.fxmisc.richtext.model.TwoDimensional} and {@link org.fxmisc.richtext.model.TwoDimensional.Bias}。
 * 另外，请阅读{@link org.fxmisc.richtext.model.StyledDocument#getAbsolutePosition(int, int)} 中“position”和“index”之间的区别。
 *
 * @param <PS>  段落样式 (e.g. {@link TextFlow})
 * @param <SEG> {@link Paragraph} 中的‌分段类型，
 *              要么是文本（纯文本或带样式），要么是文本与其他 {@link Node} 组合的类型
 *              把“内容”与“样式”都参数化，使得同一套渲染逻辑既支持纯文本，也支持带 CSS 类名的富文本，甚至支持嵌入 Node 的复杂段落
 * @param <S>   段的样式
 * @see EditableStyledDocument
 * @see TwoDimensional
 * @see org.fxmisc.richtext.model.TwoDimensional.Bias
 * @see VirtualFlow
 * @see VirtualizedScrollPane
 * @see Caret
 * @see Selection
 * @see CaretSelectionBind
 */
public class GenericStyledArea<PS, SEG, S> extends Region
        implements
        TextEditingArea<PS, SEG, S>,
        EditActions<PS, SEG, S>,
        ClipboardActions<PS, SEG, S>,
        NavigationActions<PS, SEG, S>,
        StyleActions<PS, S>,
        UndoActions,
        ViewActions<PS, SEG, S>,
        TwoDimensional,
        Virtualized {

    //———————————————————————————————————————————CSS———————————————————————————————————————————

    private static final CssMetaData<GenericStyledArea<?, ?, ?>, Paint> HIGHLIGHT_TEXT_FILL = new CustomCssMetaData<>(
            "-fx-highlight-text-fill", StyleConverter.getPaintConverter(), Color.WHITE, s -> s.highlightTextFill
    );

    private static final List<CssMetaData<? extends Styleable, ?>> CSS_META_DATA_LIST;

    static {
        List<CssMetaData<? extends Styleable, ?>> styleables = new ArrayList<>(Region.getClassCssMetaData());

        styleables.add(HIGHLIGHT_TEXT_FILL);

        CSS_META_DATA_LIST = Collections.unmodifiableList(styleables);
    }

    /**
     * Index range [0, 0).
     */
    public static final IndexRange EMPTY_RANGE = new IndexRange(0, 0);

    private static final PseudoClass READ_ONLY = PseudoClass.getPseudoClass("readonly");
    private static final PseudoClass HAS_CARET = PseudoClass.getPseudoClass("has-caret");
    private static final PseudoClass FIRST_PAR = PseudoClass.getPseudoClass("first-paragraph");
    private static final PseudoClass LAST_PAR = PseudoClass.getPseudoClass("last-paragraph");

    /* ********************************************************************** *
     *                                                                        *
     * Properties                                                             *
     *                                                                        *
     * Properties affect behavior and/or appearance of this control.          *
     *                                                                        *
     * They are readable and writable by the client code and never change by  *
     * other means, i.e. they contain either the default value or the value   *
     * set by the client code.                                                *
     *                                                                        *
     * ********************************************************************** */

    /**
     * Text color for highlighted text.
     */
    private final StyleableObjectProperty<Paint> highlightTextFill
            = new CustomStyleableProperty<>(Color.WHITE, "highlightTextFill", this, HIGHLIGHT_TEXT_FILL);

    // editable property
    private final BooleanProperty editable = new SimpleBooleanProperty(this, "editable", true) {
        @Override
        protected void invalidated() {
            ((Region) getBean()).pseudoClassStateChanged(READ_ONLY, !get());
        }
    };

    private final ReadOnlyBooleanProperty overwriteMode;

    private final BooleanProperty wrapText = new SimpleBooleanProperty(this, "wrapText");

    private UndoManager undoManager;

    private Locale textLocale = Locale.getDefault();

    private final ObjectProperty<Duration> mouseOverTextDelay = new SimpleObjectProperty<>(null);

    private final ObjectProperty<IntFunction<? extends Node>> paragraphGraphicFactory = new SimpleObjectProperty<>(null);

    private ObjectProperty<Node> placeHolderProp = new SimpleObjectProperty<>(this, "placeHolder", null);

    private Pos placeHolderPos = Pos.CENTER;

    private ObjectProperty<ContextMenu> contextMenu = new SimpleObjectProperty<>(null);

    private DoubleProperty contextMenuXOffset = new SimpleDoubleProperty(2);

    private DoubleProperty contextMenuYOffset = new SimpleDoubleProperty(2);

    private final BooleanProperty useInitialStyleForInsertion = new SimpleBooleanProperty();

    private Optional<Tuple2<Codec<PS>, Codec<StyledSegment<SEG, S>>>> styleCodecs = Optional.empty();

    private final ObjectProperty<EventHandler<MouseEvent>> onOutsideSelectionMousePressed = new SimpleObjectProperty<>(
            e -> moveTo(hit(e.getX(), e.getY()).getInsertionIndex(), SelectionPolicy.CLEAR));

    private final ObjectProperty<EventHandler<MouseEvent>> onInsideSelectionMousePressReleased = new SimpleObjectProperty<>(
            e -> moveTo(hit(e.getX(), e.getY()).getInsertionIndex(), SelectionPolicy.CLEAR));

    private final ObjectProperty<Consumer<Point2D>> onNewSelectionDrag = new SimpleObjectProperty<>(p -> {
        CharacterHit hit = hit(p.getX(), p.getY());
        moveTo(hit.getInsertionIndex(), SelectionPolicy.ADJUST);
    });

    private final ObjectProperty<EventHandler<MouseEvent>> onNewSelectionDragFinished = new SimpleObjectProperty<>(e -> {
        CharacterHit hit = hit(e.getX(), e.getY());
        moveTo(hit.getInsertionIndex(), SelectionPolicy.ADJUST);
    });

    private final ObjectProperty<Consumer<Point2D>> onSelectionDrag = new SimpleObjectProperty<>(p -> {
        CharacterHit hit = hit(p.getX(), p.getY());
        displaceCaret(hit.getInsertionIndex());
    });

    private final ObjectProperty<EventHandler<MouseEvent>> onSelectionDropped = new SimpleObjectProperty<>(e -> {
        moveSelectedText(hit(e.getX(), e.getY()).getInsertionIndex());
    });

    // not a hook, but still plays a part in the default mouse behavior
    private final BooleanProperty autoScrollOnDragDesired = new SimpleBooleanProperty(true);

    // 收集所有事件订阅，dispose() 时一次性取消，避免泄漏
    private Subscription subscriptions = () -> {
    };

    // 虚拟化滚动容器，它只为可见的段落创建 ParagraphBox，不在视口内，则不创建
    private final VirtualFlow<Paragraph<PS, SEG, S>, Cell<Paragraph<PS, SEG, S>, ParagraphBox<PS, SEG, S>>> virtualFlow;

    // ‌用于两级导航，上层是段落，下层是段落内的行‌，用于上下移动光标
    private final TwoLevelNavigator paragraphLineNavigator;

    // 可见段落的响应式列表，layoutChildren() 和滚动时使用
    private boolean paging, followCaretRequested = false;

    //———————————————————————————————————————————光标———————————————————————————————————————————

    // 主光标 + 主选择的绑定
    private CaretSelectionBind<PS, SEG, S> caretSelectionBind;
    // 支持多个光标/选择（多光标编辑），集合变化会通知到所有 ParagraphBox，让它们按需添加/移除光标路径
    private final SubscribeableContentsObsSet<CaretNode> caretSet;
    private final SubscribeableContentsObsSet<Selection<PS, SEG, S>> selectionSet;

    private final SuspendableList<Paragraph<PS, SEG, S>> visibleParagraphs;

    // 批量更新（如 undo/redo）的标志，期间暂停 UI 响应
    private final SuspendableNo beingUpdated = new SuspendableNo();

    private final SuspendableEventStream<?> viewportDirty;

    //———————————————————————————————————————————clone 必需要用的字段———————————————————————————————————————————

    // 数据源，所有文本、样式、段落都存放在这里
    private final EditableStyledDocument<PS, SEG, S> content;

    private final PS initialParagraphStyle;
    private final S initialTextStyle;
    // 把段落样式 PS 应用到整段的 TextFlow 上
    private final BiConsumer<TextFlow, PS> applyParagraphStyle;

    // undo/redo 涉及这个字段
    private final boolean preserveStyle;

    //———————————————————————————————————————————其他———————————————————————————————————————————

    // 定义了“段”的操作语义（合并、样式处理、Codec 等）
    private final TextOps<SEG, S> segmentOps;

    private final EventStream<Boolean> autoCaretBlinksSteam;

    //———————————————————————————————————————————getter、setter———————————————————————————————————————————

    @Override
    public final BooleanProperty editableProperty() {
        return editable;
    }

    // Don't remove as FXMLLoader doesn't recognise default methods !
    @Override
    public void setEditable(boolean value) {
        editable.set(value);
    }

    @Override
    public boolean isEditable() {
        return editable.get();
    }

    /**
     * Indicates weather the area is in overwrite or insert mode.
     */
    public final ReadOnlyBooleanProperty overwriteModeProperty() {
        return overwriteMode;
    }

    public boolean isOverwriteMode() {
        return overwriteMode.get();
    }

    @Override
    public final BooleanProperty wrapTextProperty() {
        return wrapText;
    }

    // Don't remove as FXMLLoader doesn't recognise default methods !
    @Override
    public void setWrapText(boolean value) {
        wrapText.set(value);
    }

    @Override
    public boolean isWrapText() {
        return wrapText.get();
    }

    /**
     * This is used to determine word and sentence breaks while navigating or selecting.
     * Override this method if your paragraph or text style accommodates Locales as well.
     *
     * @return Locale.getDefault() by default
     */
    @Override
    public Locale getLocale() {
        return textLocale;
    }

    public void setLocale(Locale editorLocale) {
        textLocale = editorLocale;
    }

    @Override
    public ObjectProperty<Duration> mouseOverTextDelayProperty() {
        return mouseOverTextDelay;
    }

    @Override
    public ObjectProperty<IntFunction<? extends Node>> paragraphGraphicFactoryProperty() {
        return paragraphGraphicFactory;
    }

    public final ObjectProperty<Node> placeholderProperty() {
        return placeHolderProp;
    }

    public final Node getPlaceholder() {
        return placeHolderProp.get();
    }

    @Override
    public final ObjectProperty<ContextMenu> contextMenuObjectProperty() {
        return contextMenu;
    }

    // Don't remove as FXMLLoader doesn't recognise default methods !
    @Override
    public void setContextMenu(ContextMenu menu) {
        contextMenu.set(menu);
    }

    @Override
    public ContextMenu getContextMenu() {
        return contextMenu.get();
    }

    protected final boolean isContextMenuPresent() {
        return contextMenu.get() != null;
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return CSS_META_DATA_LIST;
    }

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return CSS_META_DATA_LIST;
    }

    //———————————————————————————————————————————复杂 getter、setter———————————————————————————————————————————

    public void recreateParagraphGraphic(int parNdx) {
        ObjectProperty<IntFunction<? extends Node>> gProp;
        gProp = getCell(parNdx).graphicFactoryProperty();
        gProp.unbind();
        gProp.bind(paragraphGraphicFactoryProperty());
    }

    public Node getParagraphGraphic(int parNdx) {
        return getCell(parNdx).getGraphic();
    }

    @Override
    public UndoManager getUndoManager() {
        return undoManager;
    }

    /**
     * @param undoManager may be null in which case a no op undo manager will be set.
     */
    @Override
    public void setUndoManager(UndoManager undoManager) {
        this.undoManager.close();
        this.undoManager = undoManager != null ? undoManager : UndoUtils.noOpUndoManager();
    }

    /**
     * This Node is shown to the user, centered over the area, when the area has no text content.
     * <br>To customize the placeholder's layout override {@link #configurePlaceholder(Node)}
     */
    public final void setPlaceholder(Node value) {
        setPlaceholder(value, Pos.CENTER);
    }

    public void setPlaceholder(Node value, Pos where) {
        placeHolderProp.set(value);
        placeHolderPos = Objects.requireNonNull(where);
    }

    @Override
    public void setStyleCodecs(Codec<PS> paragraphStyleCodec, Codec<StyledSegment<SEG, S>> styledSegCodec) {
        styleCodecs = Optional.of(t(paragraphStyleCodec, styledSegCodec));
    }

    @Override
    public Optional<Tuple2<Codec<PS>, Codec<StyledSegment<SEG, S>>>> getStyleCodecs() {
        return styleCodecs;
    }

    @Override
    public Var<Double> estimatedScrollXProperty() {
        return virtualFlow.estimatedScrollXProperty();
    }

    @Override
    public Var<Double> estimatedScrollYProperty() {
        return virtualFlow.estimatedScrollYProperty();
    }

    public final boolean addCaret(CaretNode caret) {
        if (caret.getArea() != this) {
            throw new IllegalArgumentException(String.format(
                    "The caret (%s) is associated with a different area (%s), " +
                            "not this area (%s)", caret, caret.getArea(), this));
        }
        return caretSet.add(caret);
    }

    public final boolean removeCaret(CaretNode caret) {
        if (caret != caretSelectionBind.getUnderlyingCaret()) {
            return caretSet.remove(caret);
        } else {
            return false;
        }
    }

    public final boolean addSelection(Selection<PS, SEG, S> selection) {
        if (selection.getArea() != this) {
            throw new IllegalArgumentException(String.format(
                    "The selection (%s) is associated with a different area (%s), " +
                            "not this area (%s)", selection, selection.getArea(), this));
        }
        return selectionSet.add(selection);
    }

    public final boolean removeSelection(Selection<PS, SEG, S> selection) {
        if (selection != caretSelectionBind.getUnderlyingSelection()) {
            return selectionSet.remove(selection);
        } else {
            return false;
        }
    }

    //———————————————————————————————————————————修改默认鼠标行为的钩子———————————————————————————————————————————

    @Override
    public final EventHandler<MouseEvent> getOnOutsideSelectionMousePressed() {
        return onOutsideSelectionMousePressed.get();
    }

    @Override
    public final void setOnOutsideSelectionMousePressed(EventHandler<MouseEvent> handler) {
        onOutsideSelectionMousePressed.set(handler);
    }

    @Override
    public final ObjectProperty<EventHandler<MouseEvent>> onOutsideSelectionMousePressedProperty() {
        return onOutsideSelectionMousePressed;
    }

    @Override
    public final EventHandler<MouseEvent> getOnInsideSelectionMousePressReleased() {
        return onInsideSelectionMousePressReleased.get();
    }

    @Override
    public final void setOnInsideSelectionMousePressReleased(EventHandler<MouseEvent> handler) {
        onInsideSelectionMousePressReleased.set(handler);
    }

    @Override
    public final ObjectProperty<EventHandler<MouseEvent>> onInsideSelectionMousePressReleasedProperty() {
        return onInsideSelectionMousePressReleased;
    }

    @Override
    public final DoubleProperty contextMenuXOffsetProperty() {
        return contextMenuXOffset;
    }

    // Don't remove as FXMLLoader doesn't recognise default methods !
    @Override
    public void setContextMenuXOffset(double offset) {
        contextMenuXOffset.set(offset);
    }

    @Override
    public double getContextMenuXOffset() {
        return contextMenuXOffset.get();
    }

    @Override
    public final DoubleProperty contextMenuYOffsetProperty() {
        return contextMenuYOffset;
    }

    // Don't remove as FXMLLoader doesn't recognise default methods !
    @Override
    public void setContextMenuYOffset(double offset) {
        contextMenuYOffset.set(offset);
    }

    @Override
    public double getContextMenuYOffset() {
        return contextMenuYOffset.get();
    }

    @Override
    public BooleanProperty useInitialStyleForInsertionProperty() {
        return useInitialStyleForInsertion;
    }

    @Override
    public final ObjectProperty<Consumer<Point2D>> onNewSelectionDragProperty() {
        return onNewSelectionDrag;
    }

    @Override
    public final EventHandler<MouseEvent> getOnNewSelectionDragFinished() {
        return onNewSelectionDragFinished.get();
    }

    @Override
    public final void setOnNewSelectionDragFinished(EventHandler<MouseEvent> handler) {
        onNewSelectionDragFinished.set(handler);
    }

    @Override
    public final ObjectProperty<EventHandler<MouseEvent>> onNewSelectionDragFinishedProperty() {
        return onNewSelectionDragFinished;
    }

    @Override
    public final ObjectProperty<Consumer<Point2D>> onSelectionDragProperty() {
        return onSelectionDrag;
    }

    @Override
    public final EventHandler<MouseEvent> getOnSelectionDropped() {
        return onSelectionDropped.get();
    }

    @Override
    public final void setOnSelectionDropped(EventHandler<MouseEvent> handler) {
        onSelectionDropped.set(handler);
    }

    @Override
    public final ObjectProperty<EventHandler<MouseEvent>> onSelectionDroppedProperty() {
        return onSelectionDropped;
    }

    @Override
    public final BooleanProperty autoScrollOnDragDesiredProperty() {
        return autoScrollOnDragDesired;
    }

    // Don't remove as FXMLLoader doesn't recognise default methods !
    @Override
    public void setAutoScrollOnDragDesired(boolean val) {
        autoScrollOnDragDesired.set(val);
    }

    @Override
    public boolean isAutoScrollOnDragDesired() {
        return autoScrollOnDragDesired.get();
    }

    /* ********************************************************************** *
     *                                                                        *
     * Observables                                                            *
     *                                                                        *
     * Observables are "dynamic" (i.e. changing) characteristics of this      *
     * control. They are not directly settable by the client code, but change *
     * in response to user input and/or API actions.                          *
     *                                                                        *
     * ********************************************************************** */

    // text
    @Override
    public final ObservableValue<String> textProperty() {
        return content.textProperty();
    }

    // rich text
    @Override
    public final StyledDocument<PS, SEG, S> getDocument() {
        return content;
    }

    @Override
    public final CaretSelectionBind<PS, SEG, S> getCaretSelectionBind() {
        return caretSelectionBind;
    }

    // length
    @Override
    public final ObservableValue<Integer> lengthProperty() {
        return content.lengthProperty();
    }

    @Override
    public LiveList<Paragraph<PS, SEG, S>> getParagraphs() {
        return content.getParagraphs();
    }

    @Override
    public final LiveList<Paragraph<PS, SEG, S>> getVisibleParagraphs() {
        return visibleParagraphs;
    }

    @Override
    public final SuspendableNo beingUpdatedProperty() {
        return beingUpdated;
    }

    // total width estimate
    @Override
    public Val<Double> totalWidthEstimateProperty() {
        return virtualFlow.totalWidthEstimateProperty();
    }

    // total height estimate
    @Override
    public Val<Double> totalHeightEstimateProperty() {
        return virtualFlow.totalHeightEstimateProperty();
    }

    //———————————————————————————————————————————Event streams———————————————————————————————————————————

    @Override
    public EventStream<List<RichTextChange<PS, SEG, S>>> multiRichChanges() {
        return content.multiRichChanges();
    }

    @Override
    public EventStream<List<PlainTextChange>> multiPlainChanges() {
        return content.multiPlainChanges();
    }

    // text changes
    @Override
    public final EventStream<PlainTextChange> plainTextChanges() {
        return content.plainChanges();
    }

    // rich text changes
    @Override
    public final EventStream<RichTextChange<PS, SEG, S>> richChanges() {
        return content.richChanges();
    }

    @Override
    public final EventStream<?> viewportDirtyEvents() {
        return viewportDirty;
    }

    @Override
    public final EditableStyledDocument<PS, SEG, S> getContent() {
        return content;
    }

    @Override
    public final S getInitialTextStyle() {
        return initialTextStyle;
    }

    @Override
    public final PS getInitialParagraphStyle() {
        return initialParagraphStyle;
    }

    @Override
    public final BiConsumer<TextFlow, PS> getApplyParagraphStyle() {
        return applyParagraphStyle;
    }

    @Override
    public final boolean isPreserveStyle() {
        return preserveStyle;
    }

    @Override
    public final TextOps<SEG, S> getSegOps() {
        return segmentOps;
    }

    final EventStream<Boolean> autoCaretBlink() {
        return autoCaretBlinksSteam;
    }

    //———————————————————————————————————————————构造器———————————————————————————————————————————

    /**
     * Creates a text area with empty text content.
     *
     * @param initialParagraphStyle style to use in places where no other style is
     *                              specified (yet).
     * @param applyParagraphStyle   function that, given a {@link TextFlow} node and
     *                              a style, applies the style to the paragraph node. This function is
     *                              used by the default skin to apply style to paragraph nodes.
     * @param initialTextStyle      style to use in places where no other style is
     *                              specified (yet).
     * @param segmentOps            The operations which are defined on the text segment objects.
     * @param nodeFactory           A function which is used to create the JavaFX scene nodes for a
     *                              particular segment.
     */
    public GenericStyledArea(@NamedArg("initialParagraphStyle") PS initialParagraphStyle,
                             @NamedArg("applyParagraphStyle") BiConsumer<TextFlow, PS> applyParagraphStyle,
                             @NamedArg("initialTextStyle") S initialTextStyle,
                             @NamedArg("segmentOps") TextOps<SEG, S> segmentOps,
                             @NamedArg("nodeFactory") Function<StyledSegment<SEG, S>, Node> nodeFactory) {
        this(initialParagraphStyle, applyParagraphStyle, initialTextStyle, segmentOps, true, nodeFactory);
    }

    /**
     * Same as {@link #GenericStyledArea(Object, BiConsumer, Object, TextOps, Function)} but also allows one
     * to specify whether the undo manager should be a plain or rich undo manager via {@code preserveStyle}.
     *
     * @param initialParagraphStyle style to use in places where no other style is specified (yet).
     * @param applyParagraphStyle   function that, given a {@link TextFlow} node and
     *                              a style, applies the style to the paragraph node. This function is
     *                              used by the default skin to apply style to paragraph nodes.
     * @param initialTextStyle      style to use in places where no other style is specified (yet).
     * @param segmentOps            The operations which are defined on the text segment objects.
     * @param preserveStyle         whether to use an undo manager that can undo/redo {@link RichTextChange}s or
     *                              {@link PlainTextChange}s
     * @param nodeFactory           A function which is used to create the JavaFX scene node for a particular segment.
     */
    public GenericStyledArea(@NamedArg("initialParagraphStyle") PS initialParagraphStyle,
                             @NamedArg("applyParagraphStyle") BiConsumer<TextFlow, PS> applyParagraphStyle,
                             @NamedArg("initialTextStyle") S initialTextStyle,
                             @NamedArg("segmentOps") TextOps<SEG, S> segmentOps,
                             @NamedArg("preserveStyle") boolean preserveStyle,
                             @NamedArg("nodeFactory") Function<StyledSegment<SEG, S>, Node> nodeFactory) {
        this(initialParagraphStyle, applyParagraphStyle, initialTextStyle,
                new GenericEditableStyledDocument<>(initialParagraphStyle, initialTextStyle, segmentOps), segmentOps, preserveStyle, nodeFactory);
    }

    /**
     * The same as {@link #GenericStyledArea(Object, BiConsumer, Object, TextOps, Function)} except that
     * this constructor can be used to create another {@code GenericStyledArea} that renders and edits the same
     * {@link EditableStyledDocument} or when one wants to use a custom {@link EditableStyledDocument} implementation.
     */
    public GenericStyledArea(
            @NamedArg("initialParagraphStyle") PS initialParagraphStyle,
            @NamedArg("applyParagraphStyle") BiConsumer<TextFlow, PS> applyParagraphStyle,
            @NamedArg("initialTextStyle") S initialTextStyle,
            @NamedArg("document") EditableStyledDocument<PS, SEG, S> document,
            @NamedArg("segmentOps") TextOps<SEG, S> segmentOps,
            @NamedArg("nodeFactory") Function<StyledSegment<SEG, S>, Node> nodeFactory) {
        this(initialParagraphStyle, applyParagraphStyle, initialTextStyle, document, segmentOps, true, nodeFactory);
    }

    /**
     * Creates an area with flexibility in all of its options.
     *
     * @param initialParagraphStyle style to use in places where no other style is specified (yet).
     * @param applyParagraphStyle   function that, given a {@link TextFlow} node and
     *                              a style, applies the style to the paragraph node. This function is
     *                              used by the default skin to apply style to paragraph nodes.
     * @param initialTextStyle      style to use in places where no other style is specified (yet).
     * @param document              the document to render and edit
     * @param segmentOps            The operations which are defined on the text segment objects.
     * @param preserveStyle         whether to use an undo manager that can undo/redo {@link RichTextChange}s or
     *                              {@link PlainTextChange}s
     * @param nodeFactory           用来为一个特定的 segment 创建 JavaFX scene node
     */
    public GenericStyledArea(
            @NamedArg("initialParagraphStyle") PS initialParagraphStyle,
            @NamedArg("applyParagraphStyle") BiConsumer<TextFlow, PS> applyParagraphStyle,
            @NamedArg("initialTextStyle") S initialTextStyle,
            @NamedArg("document") EditableStyledDocument<PS, SEG, S> document,
            @NamedArg("segmentOps") TextOps<SEG, S> segmentOps,
            @NamedArg("preserveStyle") boolean preserveStyle,
            @NamedArg("nodeFactory") Function<StyledSegment<SEG, S>, Node> nodeFactory) {
        this.initialParagraphStyle = initialParagraphStyle;
        this.applyParagraphStyle = applyParagraphStyle;
        this.initialTextStyle = initialTextStyle;
        this.content = document;
        this.segmentOps = segmentOps;
        this.preserveStyle = preserveStyle;

        undoManager = UndoUtils.defaultUndoManager(this);

        // allow tab traversal into area
        setFocusTraversable(true);

        this.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
        getStyleClass().add("styled-text-area");
        getStylesheets().add(StyledTextArea.class.getResource("styled-text-area.css").toExternalForm());

        // keeps track of currently used non-empty cells
        @SuppressWarnings("unchecked")
        ObservableSet<ParagraphBox<PS, SEG, S>> nonEmptyCells = FXCollections.observableSet();

        caretSet = new SubscribeableContentsObsSet<>();
        manageSubscription(() -> {
            List<CaretNode> l = new ArrayList<>(caretSet);
            caretSet.clear();
            l.forEach(CaretNode::dispose);
        });

        selectionSet = new SubscribeableContentsObsSet<>();
        manageSubscription(() -> {
            List<Selection<PS, SEG, S>> l = new ArrayList<>(selectionSet);
            selectionSet.clear();
            l.forEach(Selection::dispose);
        });

        // Initialize content
        virtualFlow = VirtualFlow.createVertical(
                getParagraphs(),
                par -> {
                    Cell<Paragraph<PS, SEG, S>, ParagraphBox<PS, SEG, S>> cell = createCell(
                            par,
                            applyParagraphStyle,
                            nodeFactory);
                    nonEmptyCells.add(cell.getNode());
                    return cell.beforeReset(() -> nonEmptyCells.remove(cell.getNode()))
                            .afterUpdateItem(p -> nonEmptyCells.add(cell.getNode()));
                });
        getChildren().add(virtualFlow);

        // initialize navigator
        IntSupplier cellCount = () -> getParagraphs().size();
        IntUnaryOperator cellLength = i -> virtualFlow.getCell(i).getNode().getLineCount();
        paragraphLineNavigator = new TwoLevelNavigator(cellCount, cellLength);

        viewportDirty = merge(
                // no need to check for width & height invalidations as scroll values update when these do

                // scale
                invalidationsOf(scaleXProperty()),
                invalidationsOf(scaleYProperty()),

                // scroll
                invalidationsOf(estimatedScrollXProperty()),
                invalidationsOf(estimatedScrollYProperty())
        ).suppressible();

        autoCaretBlinksSteam = EventStreams.valuesOf(focusedProperty()
                .and(editableProperty())
                .and(disabledProperty().not())
        );

        caretSelectionBind = new CaretSelectionBindImpl<>("main-caret", "main-selection", this);
        caretSelectionBind.paragraphIndexProperty().addListener(this::skipOverFoldedParagraphs);
        caretSet.add(caretSelectionBind.getUnderlyingCaret());
        selectionSet.add(caretSelectionBind.getUnderlyingSelection());

        visibleParagraphs = LiveList.map(virtualFlow.visibleCells(), c -> c.getNode().getParagraph()).suspendable();

        final Suspendable omniSuspendable = Suspendable.combine(
                beingUpdated, // must be first, to be the last one to release

                visibleParagraphs
        );
        manageSubscription(omniSuspendable.suspendWhen(content.beingUpdatedProperty()));

        // dispatch MouseOverTextEvents when mouseOverTextDelay is not null
        EventStreams.valuesOf(mouseOverTextDelayProperty())
                .flatMap(delay -> delay != null
                        ? mouseOverTextEvents(nonEmptyCells, delay)
                        : EventStreams.never())
                .subscribe(evt -> Event.fireEvent(this, evt));

        this.overwriteMode = new GenericStyledAreaBehavior(this).overwriteModeProperty();

        // Setup place holder visibility & placement
        final Val<Boolean> showPlaceholder = Val.create
                (
                        () -> getLength() == 0 && !isFocused(),
                        lengthProperty(), focusedProperty()
                );

        placeHolderProp.addListener((ob, ov, newNode) -> displayPlaceHolder(showPlaceholder.getValue(), newNode));
        showPlaceholder.addListener((ob, ov, show) -> displayPlaceHolder(show, getPlaceholder()));

        if (Platform.isFxApplicationThread()) initInputMethodHandling();
        else Platform.runLater(this::initInputMethodHandling);
    }

    private void initInputMethodHandling() {
        if (Platform.isSupported(ConditionalFeature.INPUT_METHOD)) {
            setOnInputMethodTextChanged(this::handleInputMethodEvent);
            setInputMethodRequests(new InputMethodRequests() {
                @Override
                public Point2D getTextLocation(int offset) {
                    return getCaretBounds()
                            .or(() -> getCharacterBoundsOnScreen(offset, offset))
                            .map(cb -> new Point2D((cb.getMaxX() - 5) * ConfigConstants.SCALE, cb.getMaxY() * ConfigConstants.SCALE))
                            .orElseGet(() -> new Point2D(10, 10));
                }

                @Override
                public int getLocationOffset(int x, int y) {
                    return 0;
                }

                @Override
                public void cancelLatestCommittedText() {
                }

                @Override
                public String getSelectedText() {
                    return GenericStyledArea.this.getSelectedText();
                }
            });
        }
    }

    // Start/Length of the text under input method composition
    private int imStart;
    private int imLength;

    protected void handleInputMethodEvent(InputMethodEvent event) {
        if (isEditable() && !isDisabled()) {
            // remove previous input method text (if any) or selected text
            if (imLength != 0) {
                selectRange(imStart, imStart + imLength);
            }

            // Insert committed text
            if (event.getCommitted().length() != 0) {
                replaceText(getSelection(), event.getCommitted());
            }

            // Replace composed text
            imStart = getSelection().getStart();
            StringBuilder composed = new StringBuilder();

            for (InputMethodTextRun run : event.getComposed()) {
                composed.append(run.getText());
            }

            replaceText(getSelection(), composed.toString());
            imLength = composed.length();

            if (imLength != 0) {
                int pos = imStart;
                for (InputMethodTextRun run : event.getComposed()) {
                    int endPos = pos + run.getText().length();
                    pos = endPos;
                }

                // Set caret position in composed text
                int caretPos = event.getCaretPosition();
                if (caretPos >= 0 && caretPos < imLength) {
                    selectRange(imStart + caretPos, imStart + caretPos);
                }
            }
        }
    }

    private Node placeholder;
    private boolean positionPlaceholder = false;

    private void displayPlaceHolder(boolean show, Node newNode) {
        if (placeholder != null && (!show || newNode != placeholder)) {
            placeholder.layoutXProperty().unbind();
            placeholder.layoutYProperty().unbind();
            getChildren().remove(placeholder);
            placeholder = null;
            setClip(null);
        }
        if (newNode != null && show && newNode != placeholder) {
            configurePlaceholder(newNode);
            getChildren().add(newNode);
            placeholder = newNode;
        }
    }

    /**
     * Override this to customize the placeholder's layout.
     * <br>The default position is centered over the area.
     */
    protected void configurePlaceholder(Node placeholder) {
        positionPlaceholder = true;
    }

    /* ********************************************************************** *
     *                                                                        *
     * Queries                                                                *
     *                                                                        *
     * Queries are parameterized observables.                                 *
     *                                                                        *
     * ********************************************************************** */

    @Override
    public final double getViewportHeight() {
        return virtualFlow.getHeight();
    }

    @Override
    public final Optional<Integer> allParToVisibleParIndex(int allParIndex) {
        if (allParIndex < 0) {
            throw new IllegalArgumentException("The given paragraph index (allParIndex) cannot be negative but was " + allParIndex);
        }
        if (allParIndex >= getParagraphs().size()) {
            throw new IllegalArgumentException(String.format(
                    "Paragraphs' last index is [%s] but allParIndex was [%s]",
                    getParagraphs().size() - 1, allParIndex)
            );
        }
        List<Cell<Paragraph<PS, SEG, S>, ParagraphBox<PS, SEG, S>>> visibleList = virtualFlow.visibleCells();
        int firstVisibleParIndex = visibleList.get(0).getNode().getIndex();
        int targetIndex = allParIndex - firstVisibleParIndex;

        if (allParIndex >= firstVisibleParIndex && targetIndex < visibleList.size()) {
            if (visibleList.get(targetIndex).getNode().getIndex() == allParIndex) {
                return Optional.of(targetIndex);
            }
        }
        return Optional.empty();
    }

    @Override
    public final int visibleParToAllParIndex(int visibleParIndex) {
        if (visibleParIndex < 0) {
            throw new IllegalArgumentException("Visible paragraph index cannot be negative but was " + visibleParIndex);
        }
        if (visibleParIndex > 0 && visibleParIndex >= getVisibleParagraphs().size()) {
            throw new IllegalArgumentException(String.format(
                    "Visible paragraphs' last index is [%s] but visibleParIndex was [%s]",
                    getVisibleParagraphs().size() - 1, visibleParIndex)
            );
        }

        try {
            Cell<Paragraph<PS, SEG, S>, ParagraphBox<PS, SEG, S>> visibleCell = null;

            if (visibleParIndex > 0) visibleCell = virtualFlow.visibleCells().get(visibleParIndex);
            else visibleCell = virtualFlow.getCellIfVisible(virtualFlow.getFirstVisibleIndex())
                    .orElseGet(() -> virtualFlow.visibleCells().get(visibleParIndex));

            return visibleCell.getNode().getIndex();
        } catch (IndexOutOfBoundsException | NoSuchElementException EX) {
            return -1;
        }
    }

    @Override
    public CharacterHit hit(double x, double y) {
        // mouse position used, so account for padding
        double adjustedX = x - getInsets().getLeft();
        double adjustedY = y - getInsets().getTop();
        VirtualFlowHit<Cell<Paragraph<PS, SEG, S>, ParagraphBox<PS, SEG, S>>> hit = virtualFlow.hit(adjustedX, adjustedY);
        if (hit.isBeforeCells()) {
            return CharacterHit.insertionAt(0);
        } else if (hit.isAfterCells()) {
            return CharacterHit.insertionAt(getLength());
        } else {
            int parIdx = hit.getCellIndex();
            int parOffset = getParagraphOffset(parIdx);
            ParagraphBox<PS, SEG, S> cell = hit.getCell().getNode();
            Point2D cellOffset = hit.getCellOffset();
            CharacterHit parHit = cell.hit(cellOffset);
            return parHit.offset(parOffset);
        }
    }

    @Override
    public final int lineIndex(int paragraphIndex, int columnPosition) {
        Cell<Paragraph<PS, SEG, S>, ParagraphBox<PS, SEG, S>> cell = virtualFlow.getCell(paragraphIndex);
        return cell.getNode().getCurrentLineIndex(columnPosition);
    }

    @Override
    public int getParagraphLinesCount(int paragraphIndex) {
        return virtualFlow.getCell(paragraphIndex).getNode().getLineCount();
    }

    @Override
    public Optional<Bounds> getCharacterBoundsOnScreen(int from, int to) {
        if (from < 0) {
            throw new IllegalArgumentException("From is negative: " + from);
        }
        if (from > to) {
            throw new IllegalArgumentException(String.format("From is greater than to. from=%s to=%s", from, to));
        }
        if (to > getLength()) {
            throw new IllegalArgumentException(String.format("To is greater than area's length. length=%s, to=%s", getLength(), to));
        }

        if (from == to) {
            CaretNode cursor = new CaretNode("", this, from);
            int parIdx = offsetToPosition(from, Bias.Forward).getMajor();
            ParagraphBox<?, ?, ?> paragrafBox = virtualFlow.getCell(parIdx).getNode();
            paragrafBox.caretsProperty().add(cursor);
            Bounds cursorBounds = paragrafBox.getCaretBoundsOnScreen(cursor);
            paragrafBox.caretsProperty().remove(cursor);

            if (cursorBounds != null && !cursorBounds.isEmpty()) {
                Bounds emptyCharBounds = new BoundingBox(
                        cursorBounds.getMinX() + 1, cursorBounds.getMinY() + 1,
                        cursorBounds.getWidth() - 1, cursorBounds.getHeight() - 2
                );
                return Optional.of(emptyCharBounds);
            }
            return Optional.empty();
        }

        // no bounds exist if range is just a newline character
        if (getText(from, to).equals("\n")) {
            return Optional.empty();
        }

        // if 'from' is the newline character at the end of a multi-line paragraph, it returns a Bounds that whose
        //  minX & minY are the minX and minY of the paragraph itself, not the newline character. So, ignore it.
        int realFrom = getText(from, from + 1).equals("\n") ? from + 1 : from;

        Position startPosition = offsetToPosition(realFrom, Bias.Forward);
        int startRow = startPosition.getMajor();
        Position endPosition = startPosition.offsetBy(to - realFrom, Bias.Forward);
        int endRow = endPosition.getMajor();
        if (startRow == endRow) {
            return getRangeBoundsOnScreen(startRow, startPosition.getMinor(), endPosition.getMinor());
        } else {
            Optional<Bounds> rangeBounds = getRangeBoundsOnScreen(startRow, startPosition.getMinor(),
                    getParagraph(startRow).length());
            for (int i = startRow + 1; i <= endRow; i++) {
                Optional<Bounds> nextLineBounds = getRangeBoundsOnScreen(i, 0,
                        i == endRow
                                ? endPosition.getMinor()
                                : getParagraph(i).length()
                );
                if (nextLineBounds.isPresent()) {
                    if (rangeBounds.isPresent()) {
                        Bounds lineBounds = nextLineBounds.get();
                        rangeBounds = rangeBounds.map(b -> {
                            double minX = Math.min(b.getMinX(), lineBounds.getMinX());
                            double minY = Math.min(b.getMinY(), lineBounds.getMinY());
                            double maxX = Math.max(b.getMaxX(), lineBounds.getMaxX());
                            double maxY = Math.max(b.getMaxY(), lineBounds.getMaxY());
                            return new BoundingBox(minX, minY, maxX - minX, maxY - minY);
                        });
                    } else {
                        rangeBounds = nextLineBounds;
                    }
                }
            }
            return rangeBounds;
        }
    }

    @Override
    public final String getText(int start, int end) {
        return content.getText(start, end);
    }

    @Override
    public String getText(int paragraph) {
        return content.getText(paragraph);
    }

    @Override
    public String getText(IndexRange range) {
        return content.getText(range);
    }

    @Override
    public StyledDocument<PS, SEG, S> subDocument(int start, int end) {
        return content.subSequence(start, end);
    }

    @Override
    public StyledDocument<PS, SEG, S> subDocument(int paragraphIndex) {
        return content.subDocument(paragraphIndex);
    }

    @Override
    public IndexRange getParagraphSelection(Selection selection, int paragraph) {
        int startPar = selection.getStartParagraphIndex();
        int endPar = selection.getEndParagraphIndex();

        if (selection.getLength() == 0 || paragraph < startPar || paragraph > endPar) {
            return EMPTY_RANGE;
        }

        int start = paragraph == startPar ? selection.getStartColumnPosition() : 0;
        int end = paragraph == endPar ? selection.getEndColumnPosition() : getParagraphLength(paragraph) + 1;

        // force rangeProperty() to be valid
        selection.getRange();

        return new IndexRange(start, end);
    }

    @Override
    public S getStyleOfChar(int index) {
        return content.getStyleOfChar(index);
    }

    @Override
    public S getStyleAtPosition(int position) {
        return content.getStyleAtPosition(position);
    }

    @Override
    public IndexRange getStyleRangeAtPosition(int position) {
        return content.getStyleRangeAtPosition(position);
    }

    @Override
    public StyleSpans<S> getStyleSpans(int from, int to) {
        return content.getStyleSpans(from, to);
    }

    @Override
    public S getStyleOfChar(int paragraph, int index) {
        return content.getStyleOfChar(paragraph, index);
    }

    @Override
    public S getStyleAtPosition(int paragraph, int position) {
        return content.getStyleAtPosition(paragraph, position);
    }

    @Override
    public IndexRange getStyleRangeAtPosition(int paragraph, int position) {
        return content.getStyleRangeAtPosition(paragraph, position);
    }

    @Override
    public StyleSpans<S> getStyleSpans(int paragraph) {
        return content.getStyleSpans(paragraph);
    }

    @Override
    public StyleSpans<S> getStyleSpans(int paragraph, int from, int to) {
        return content.getStyleSpans(paragraph, from, to);
    }

    @Override
    public int getAbsolutePosition(int paragraphIndex, int columnIndex) {
        return content.getAbsolutePosition(paragraphIndex, columnIndex);
    }

    @Override
    public Position position(int row, int col) {
        return content.position(row, col);
    }

    @Override
    public Position offsetToPosition(int charOffset, Bias bias) {
        return content.offsetToPosition(charOffset, bias);
    }

    @Override
    public Bounds getVisibleParagraphBoundsOnScreen(int visibleParagraphIndex) {
        return getParagraphBoundsOnScreen(virtualFlow.visibleCells().get(visibleParagraphIndex));
    }

    @Override
    public Optional<Bounds> getParagraphBoundsOnScreen(int paragraphIndex) {
        return virtualFlow.getCellIfVisible(paragraphIndex).map(this::getParagraphBoundsOnScreen);
    }

    @Override
    public final <T extends Node & Caret> Optional<Bounds> getCaretBoundsOnScreen(T caret) {
        Optional<Bounds> caretBounds;
        try { // This is the default mechanism, but sometimes throws just like in followCaret()
            caretBounds = virtualFlow.getCellIfVisible(caret.getParagraphIndex())
                    .map(c -> c.getNode().getCaretBoundsOnScreen(caret));
        } catch (IllegalArgumentException EX) {
            // This is an alternative mechanism, to address https://github.com/FXMisc/RichTextFX/issues/1048
            caretBounds = Optional.ofNullable(caretSelectionBind.getUnderlyingCaret().getLayoutBounds());
        }
        return caretBounds;
    }


    /* ********************************************************************** *
     *                                                                        *
     * Actions                                                                *
     *                                                                        *
     * Actions change the state of this control.
     * They typically cause a  change of one or more observables and/or produce an event.             *
     *                                                                        *
     * ********************************************************************** */

    @Override
    public void scrollXToPixel(double pixel) {
        suspendVisibleParsWhile(() -> virtualFlow.scrollXToPixel(pixel));
    }

    @Override
    public void scrollYToPixel(double pixel) {
        suspendVisibleParsWhile(() -> virtualFlow.scrollYToPixel(pixel));
    }

    @Override
    public void scrollXBy(double deltaX) {
        suspendVisibleParsWhile(() -> virtualFlow.scrollXBy(deltaX));
    }

    @Override
    public void scrollYBy(double deltaY) {
        suspendVisibleParsWhile(() -> virtualFlow.scrollYBy(deltaY));
    }

    @Override
    public void scrollBy(Point2D deltas) {
        suspendVisibleParsWhile(() -> virtualFlow.scrollBy(deltas));
    }

    @Override
    public void showParagraphInViewport(int paragraphIndex) {
        suspendVisibleParsWhile(() -> virtualFlow.show(paragraphIndex));
    }

    @Override
    public void showParagraphAtTop(int paragraphIndex) {
        suspendVisibleParsWhile(() -> virtualFlow.showAsFirst(paragraphIndex));
    }

    @Override
    public void showParagraphAtBottom(int paragraphIndex) {
        suspendVisibleParsWhile(() -> virtualFlow.showAsLast(paragraphIndex));
    }

    @Override
    public void showParagraphRegion(int paragraphIndex, Bounds region) {
        suspendVisibleParsWhile(() -> virtualFlow.show(paragraphIndex, region));
    }

    public void showParagraphAtCenter(int paragraphIndex) {
        double offset = Math.floor(getHeight() / 2.0);
        suspendVisibleParsWhile(() -> virtualFlow.showAtOffset(paragraphIndex, offset));
    }

    // 滚动到光标处
    // 在覆盖默认用户交互行为时，要调用它
    @Override
    public void requestFollowCaret() {
        followCaretRequested = true;
        requestLayout();
    }

    @Override
    public void lineStart(SelectionPolicy policy) {
        moveTo(getCurrentParagraph(), getCurrentLineStartInParargraph(), policy);
    }

    @Override
    public void lineEnd(SelectionPolicy policy) {
        moveTo(getCurrentParagraph(), getCurrentLineEndInParargraph(), policy);
    }

    public int getCurrentLineStartInParargraph() {
        return virtualFlow.getCell(getCurrentParagraph()).getNode().getCurrentLineStartPosition(caretSelectionBind.getUnderlyingCaret());
    }

    public int getCurrentLineEndInParargraph() {
        return virtualFlow.getCell(getCurrentParagraph()).getNode().getCurrentLineEndPosition(caretSelectionBind.getUnderlyingCaret());
    }

    private double caretPrevY = -1;
    private LineSelection<PS, SEG, S> lineHighlighter;
    private ObjectProperty<Paint> lineHighlighterFill;

    /**
     * The default fill is "highlighter" yellow. It can also be styled using CSS with:<br>
     * <code>.styled-text-area .line-highlighter { -fx-fill: lime; }</code><br>
     * CSS selectors from Path, Shape, and Node can also be used.
     */
    public void setLineHighlighterFill(Paint highlight) {
        if (lineHighlighterFill != null && highlight != null) {
            lineHighlighterFill.set(highlight);
        } else {
            boolean lineHighlightOn = isLineHighlighterOn();
            if (lineHighlightOn) setLineHighlighterOn(false);

            if (highlight == null) lineHighlighterFill = null;
            else lineHighlighterFill = new SimpleObjectProperty(highlight);

            if (lineHighlightOn) setLineHighlighterOn(true);
        }
    }

    public boolean isLineHighlighterOn() {
        return lineHighlighter != null && selectionSet.contains(lineHighlighter);
    }

    /**
     * Highlights the line that the main caret is on.<br>
     * Line highlighting automatically follows the caret.
     */
    public void setLineHighlighterOn(boolean show) {
        if (show) {
            if (lineHighlighter != null) return;

            lineHighlighter = new LineSelection<>(this, lineHighlighterFill);

            Consumer<Bounds> caretListener = b ->
            {
                if (lineHighlighter != null && (b.getMinY() != caretPrevY || getCaretColumn() == 1)) {
                    if (getSelection().getLength() != 0) lineHighlighter.deselect();
                    else lineHighlighter.selectCurrentLine();
                    caretPrevY = b.getMinY();
                }
            };

            caretBoundsProperty().addListener((ob, ov, nv) -> nv.ifPresent(caretListener));
            getCaretBounds().ifPresent(caretListener);
            selectionProperty().addListener((ob, ov, nv) ->
            {
                if (lineHighlighter != null) {
                    if (nv.getLength() == 0) lineHighlighter.selectCurrentLine();
                    else if (ov.getLength() == 0) lineHighlighter.deselect();
                }
            });
            selectionSet.add(lineHighlighter);
        } else if (lineHighlighter != null) {
            selectionSet.remove(lineHighlighter);
            lineHighlighter.deselect();
            lineHighlighter = null;
            caretPrevY = -1;
        }
    }

    /**
     * Scrolls the text one line UP while maintaining the caret's
     * position on screen, so that it is now on the NEXT line.
     * <i>Note: If the caret isn't visible then this is a noop.</i>
     */
    public void nextLine(SelectionPolicy selectionPolicy) {
        scrollLine(+1, selectionPolicy);
    }

    /**
     * Scrolls the text one line DOWN while maintaining the caret's
     * position on screen, so that it is now on the PREVIOUS line.
     * <i>Note: If the caret isn't visible then this is a noop.</i>
     */
    public void prevLine(SelectionPolicy selectionPolicy) {
        scrollLine(-1, selectionPolicy);
    }

    /**
     * Scrolls the text (direction*caretHeight) while maintaining the caret's current
     * position on screen. <i>Note: If the caret isn't visible then this is a noop.</i>
     */
    private void scrollLine(int direction, SelectionPolicy selectionPolicy) {
        getCaretBoundsOnScreen(getCaretSelectionBind().getUnderlyingCaret())
                .map(caretBounds -> (caretBounds.getHeight() - 2) * direction)
                .ifPresent(deltaY -> scrollText(deltaY, selectionPolicy));
    }

    @Override
    public void prevPage(SelectionPolicy selectionPolicy) {
        // Paging up and we're in the first frame then move/select to start.
        if (firstVisibleParToAllParIndex() == 0) {
            caretSelectionBind.moveTo(0, selectionPolicy);
        } else page(-1, selectionPolicy);
    }

    @Override
    public void nextPage(SelectionPolicy selectionPolicy) {
        // Paging down and we're in the last frame then move/select to end.
        if (lastVisibleParToAllParIndex() == getParagraphs().size() - 1) {
            caretSelectionBind.moveTo(getLength(), selectionPolicy);
        } else page(+1, selectionPolicy);
    }

    /**
     * @param pgCount the number of pages to page up/down.
     *                <br>Negative numbers for paging up and positive for down.
     */
    private void page(int pgCount, SelectionPolicy selectionPolicy) {
        scrollText(pgCount * getViewportHeight(), selectionPolicy);
    }

    /**
     * Scrolls the text by deltaY while maintaining the caret's current position on screen.
     */
    private void scrollText(double deltaY, SelectionPolicy selectionPolicy) {
        // Use underlying caret to get the same behaviour as navigating up/down a line where the x position is sticky
        Optional<Bounds> cb = caretSelectionBind.getUnderlyingCaret().getCaretBounds();

        paging = true; // Prevent scroll from reverting back to the current caret position
        suspendVisibleParsWhile(() -> virtualFlow.scrollYBy(deltaY));

        cb.map(this::screenToLocal) // Place caret near the same on screen position as before
                .map(b -> hit(b.getMinX(), b.getMinY() + b.getHeight() / 2.0).getInsertionIndex())
                .ifPresent(i -> caretSelectionBind.moveTo(i, selectionPolicy));

        // Adjust scroll by a few pixels to get the caret at the exact on screen location as before
        cb.ifPresent(prev -> getCaretBounds().map(newB -> newB.getMinY() - prev.getMinY())
                .filter(delta -> delta != 0.0).ifPresent(delta -> scrollYBy(delta)));
    }

    @Override
    public void displaceCaret(int pos) {
        caretSelectionBind.displaceCaret(pos);
    }

    @Override
    public void setStyle(int from, int to, S style) {
        content.setStyle(from, to, style);
    }

    @Override
    public void setStyle(int paragraph, S style) {
        content.setStyle(paragraph, style);
    }

    @Override
    public void setStyle(int paragraph, int from, int to, S style) {
        content.setStyle(paragraph, from, to, style);
    }

    @Override
    public void setStyleSpans(int from, StyleSpans<? extends S> styleSpans) {
        content.setStyleSpans(from, styleSpans);
    }

    @Override
    public void setStyleSpans(int paragraph, int from, StyleSpans<? extends S> styleSpans) {
        content.setStyleSpans(paragraph, from, styleSpans);
    }

    @Override
    public void setParagraphStyle(int paragraph, PS paragraphStyle) {
        content.setParagraphStyle(paragraph, paragraphStyle);
    }

    /**
     * If you want to preset the style to be used for inserted text. Note that useInitialStyleForInsertion overrides this if true.
     */
    public final void setTextInsertionStyle(S txtStyle) {
        insertionTextStyle = txtStyle;
    }

    public final S getTextInsertionStyle() {
        return insertionTextStyle;
    }

    private S insertionTextStyle;

    // 打字插入新文本时，决定新字符的样式
    @Override
    public final S getTextStyleForInsertionAt(int pos) {
        // 显式指定了插入样式，比如“接下来的输入都用红色”
        if (insertionTextStyle != null) {
            return insertionTextStyle;
        }
        // 用构造时的初始样式
        else if (useInitialStyleForInsertion.get()) {
            return initialTextStyle;
        }
        // 继承插入位置已有的样式
        else {
            return content.getStyleAtPosition(pos);
        }
    }

    private PS insertionParagraphStyle;

    /**
     * If you want to preset the style to be used. Note that useInitialStyleForInsertion overrides this if true.
     */
    public final void setParagraphInsertionStyle(PS paraStyle) {
        insertionParagraphStyle = paraStyle;
    }

    public final PS getParagraphInsertionStyle() {
        return insertionParagraphStyle;
    }

    @Override
    public final PS getParagraphStyleForInsertionAt(int pos) {
        if (insertionParagraphStyle != null) {
            return insertionParagraphStyle;
        } else if (useInitialStyleForInsertion.get()) {
            return initialParagraphStyle;
        } else {
            return content.getParagraphStyleAtPosition(pos);
        }
    }

    @Override
    public void replaceText(int start, int end, String text) {
        StyledDocument<PS, SEG, S> doc = ReadOnlyStyledDocument.fromString(
                text, getParagraphStyleForInsertionAt(start), getTextStyleForInsertionAt(start), segmentOps
        );
        replace(start, end, doc);
    }

    @Override
    public void replace(int start, int end, SEG seg, S style) {
        if (style == null) style = getTextStyleForInsertionAt(start);
        StyledDocument<PS, SEG, S> doc = ReadOnlyStyledDocument.fromSegment(
                seg, getParagraphStyleForInsertionAt(start), style, segmentOps
        );
        replace(start, end, doc);
    }

    @Override
    public void replace(int start, int end, StyledDocument<PS, SEG, S> replacement) {
        content.replace(start, end, replacement);

        int newCaretPos = start + replacement.length();
        selectRange(newCaretPos, newCaretPos);
    }

    void replaceMulti(List<Replacement<PS, SEG, S>> replacements) {
        content.replaceMulti(replacements);

        // don't update selection as this is not the main method through which the area is updated
        // leave that up to the developer using it to determine what to do
    }

    @Override
    public MultiChangeBuilder<PS, SEG, S> createMultiChange() {
        return new MultiChangeBuilder<>(this);
    }

    @Override
    public MultiChangeBuilder<PS, SEG, S> createMultiChange(int initialNumOfChanges) {
        return new MultiChangeBuilder<>(this, initialNumOfChanges);
    }

    /**
     * Convenience method to fold (hide/collapse) the currently selected paragraphs,
     * into (i.e. excluding) the first paragraph of the range.
     *
     * @param styleMixin Given a paragraph style PS, return a <b>new</b> PS that will activate folding.
     *                   <p>
     *                   See {@link #fold(int, int, UnaryOperator)} for more info.
     */
    protected void foldSelectedParagraphs(UnaryOperator<PS> styleMixin) {
        IndexRange range = getSelection();
        fold(range.getStart(), range.getEnd(), styleMixin);
    }

    /**
     * Folds (hides/collapses) paragraphs from <code>start</code> to <code>
     * end</code>, into (i.e. excluding) the first paragraph of the range.
     *
     * @param styleMixin Given a paragraph style PS, return a <b>new</b> PS that will activate folding.
     *                   <p>
     *                   See {@link #fold(int, int, UnaryOperator)} for more info.
     */
    protected void foldParagraphs(int start, int end, UnaryOperator<PS> styleMixin) {
        start = getAbsolutePosition(start, 0);
        end = getAbsolutePosition(end, getParagraphLength(end));
        fold(start, end, styleMixin);
    }

    /**
     * Folds (hides/collapses) paragraphs from character position <code>startPos</code>
     * to <code>endPos</code>, into (i.e. excluding) the first paragraph of the range.
     * <p>
     * Folding is achieved with the help of paragraph styling, which is applied to the paragraph's
     * TextFlow object through the applyParagraphStyle BiConsumer (supplied in the constructor to
     * GenericStyledArea). When applyParagraphStyle is to apply fold styling it just needs to set
     * the TextFlow's visibility to collapsed for it to be folded. See {@code InlineCssTextArea},
     * {@code StyleClassedTextArea}, and {@code RichTextDemo} for different ways of doing this.
     * Also read the GitHub Wiki.
     * <p>
     * The UnaryOperator <code>styleMixin</code> must return a
     * different paragraph style Object to what was submitted.
     *
     * @param styleMixin Given a paragraph style PS, return a <b>new</b> PS that will activate folding.
     */
    protected void fold(int startPos, int endPos, UnaryOperator<PS> styleMixin) {
        ReadOnlyStyledDocument<PS, SEG, S> subDoc;
        UnaryOperator<Paragraph<PS, SEG, S>> mapper;

        subDoc = (ReadOnlyStyledDocument<PS, SEG, S>) subDocument(startPos, endPos);
        mapper = p -> p.setParagraphStyle(styleMixin.apply(p.getParagraphStyle()));

        for (int p = 1; p < subDoc.getParagraphCount(); p++) {
            subDoc = subDoc.replaceParagraph(p, mapper).get1();
        }

        replace(startPos, endPos, subDoc);
        recreateParagraphGraphic(offsetToPosition(startPos, Bias.Backward).getMajor());
        moveTo(startPos);
        foldCheck = true;
    }

    protected boolean foldCheck = false;

    private void skipOverFoldedParagraphs(ObservableValue<? extends Integer> ob, Integer prevParagraph, Integer newParagraph) {
        if (foldCheck && getCell(newParagraph).isFolded()) {
            // Prevent Ctrl+A and Ctrl+End breaking when the last paragraph is folded
            // github.com/FXMisc/RichTextFX/pull/965#issuecomment-706268116
            if (newParagraph == getParagraphs().size() - 1) return;

            int skip = (newParagraph - prevParagraph > 0) ? +1 : -1;
            int p = newParagraph + skip;

            while (p > 0 && p < getParagraphs().size()) {
                if (getCell(p).isFolded()) p += skip;
                else break;
            }

            if (p < 0 || p == getParagraphs().size()) p = prevParagraph;
            int col = Math.min(getCaretColumn(), getParagraphLength(p));

            if (getSelection().getLength() == 0) moveTo(p, col);
            else moveTo(p, col, SelectionPolicy.EXTEND);
        }
    }

    /**
     * Unfolds paragraphs <code>startingFrom</code> onwards for the currently folded block.
     * <p>
     * The UnaryOperator <code>styleMixin</code> must return a
     * different paragraph style Object to what was submitted.
     *
     * @param isFolded   Given a paragraph style PS check if it's folded.
     * @param styleMixin Given a paragraph style PS, return a <b>new</b> PS that excludes fold styling.
     */
    protected void unfoldParagraphs(int startingFrom, Predicate<PS> isFolded, UnaryOperator<PS> styleMixin) {
        LiveList<Paragraph<PS, SEG, S>> pList = getParagraphs();
        int to = startingFrom;

        while (++to < pList.size()) {
            if (!isFolded.test(pList.get(to).getParagraphStyle())) break;
        }

        if (--to > startingFrom) {
            ReadOnlyStyledDocument<PS, SEG, S> subDoc;
            UnaryOperator<Paragraph<PS, SEG, S>> mapper;

            int startPos = getAbsolutePosition(startingFrom, 0);
            int endPos = getAbsolutePosition(to, getParagraphLength(to));

            subDoc = (ReadOnlyStyledDocument<PS, SEG, S>) subDocument(startPos, endPos);
            mapper = p -> p.setParagraphStyle(styleMixin.apply(p.getParagraphStyle()));

            for (int p = 1; p < subDoc.getParagraphCount(); p++) {
                subDoc = subDoc.replaceParagraph(p, mapper).get1();
            }

            replace(startPos, endPos, subDoc);

            moveTo(startingFrom, getParagraphLength(startingFrom));
            recreateParagraphGraphic(startingFrom);
        }
    }

    //———————————————————————————————————————————Public API———————————————————————————————————————————

    @Override
    public void dispose() {
        if (undoManager != null) {
            undoManager.close();
        }
        subscriptions.unsubscribe();
        virtualFlow.dispose();
    }

    //———————————————————————————————————————————布局———————————————————————————————————————————

    @Override
    protected double computePrefHeight(double width) {
        double height = 0.0;
        Insets in = getInsets();
        for (int p = 0; p < getParagraphs().size(); p++) {
            height += getCell(p).prefHeight(-1);
        }
        return height + in.getTop() + in.getBottom();
    }

    @Override
    protected double computePrefWidth(double height) {
        double maxWidth = NodeConstants.MIN_TEXTAREA_WIDTH;
        Insets in = getInsets();
        for (int p = 0; p < getParagraphs().size(); p++) {
            double w = getCell(p).prefWidth(-1);
            maxWidth = Math.max(maxWidth, w);
        }
        return maxWidth + in.getLeft() + in.getRight();
    }

    @Override
    protected void layoutChildren() {
        Insets ins = getInsets();
        visibleParagraphs.suspendWhile(() -> {
            virtualFlow.resizeRelocate(
                    ins.getLeft(), ins.getTop(),
                    getWidth() - ins.getLeft() - ins.getRight(),
                    getHeight() - ins.getTop() - ins.getBottom());

            if (followCaretRequested && !paging) {
                try (Guard g = viewportDirty.suspend()) {
                    followCaret();
                }
            }
            followCaretRequested = false;
            paging = false;
        });

        Node holder = placeholder;
        if (holder != null && holder.isManaged()) {
            if (holder.isResizable()) holder.autosize();
            if (positionPlaceholder) Region.positionInArea
                    (
                            holder, 0, 0, getWidth(), getHeight(), getBaselineOffset(),
                            ins, placeHolderPos.getHpos(), placeHolderPos.getVpos(), isSnapToPixel()
                    );
        }
    }

    //———————————————————————————————————————————本包私有———————————————————————————————————————————

    /**
     * Returns the current line as a two-level index.
     * The major number is the paragraph index, the minor
     * number is the line number within the paragraph.
     * <p>
     * This method has a side-effect of bringing the current
     * paragraph to the viewport if it is not already visible.
     */
    TwoDimensional.Position currentLine() {
        int parIdx = getCurrentParagraph();
        Cell<Paragraph<PS, SEG, S>, ParagraphBox<PS, SEG, S>> cell = virtualFlow.getCell(parIdx);
        int lineIdx = cell.getNode().getCurrentLineIndex(caretSelectionBind.getUnderlyingCaret());
        return paragraphLineNavigator.position(parIdx, lineIdx);
    }

    void showCaretAtBottom() {
        int parIdx = getCurrentParagraph();
        Cell<Paragraph<PS, SEG, S>, ParagraphBox<PS, SEG, S>> cell = virtualFlow.getCell(parIdx);
        Bounds caretBounds = cell.getNode().getCaretBounds(caretSelectionBind.getUnderlyingCaret());
        double y = caretBounds.getMaxY();
        suspendVisibleParsWhile(() -> virtualFlow.showAtOffset(parIdx, getViewportHeight() - y));
    }

    void showCaretAtTop() {
        int parIdx = getCurrentParagraph();
        Cell<Paragraph<PS, SEG, S>, ParagraphBox<PS, SEG, S>> cell = virtualFlow.getCell(parIdx);
        Bounds caretBounds = cell.getNode().getCaretBounds(caretSelectionBind.getUnderlyingCaret());
        double y = caretBounds.getMinY();
        suspendVisibleParsWhile(() -> virtualFlow.showAtOffset(parIdx, -y));
    }

    /**
     * Returns x coordinate of the caret in the current paragraph.
     */
    final ParagraphBox.CaretOffsetX getCaretOffsetX(CaretNode caret) {
        return getCell(caret.getParagraphIndex()).getCaretOffsetX(caret);
    }

    CharacterHit hit(ParagraphBox.CaretOffsetX x, TwoDimensional.Position targetLine) {
        int parIdx = targetLine.getMajor();
        ParagraphBox<PS, SEG, S> cell = virtualFlow.getCell(parIdx).getNode();
        CharacterHit parHit = cell.hitTextLine(x, targetLine.getMinor());
        return parHit.offset(getParagraphOffset(parIdx));
    }

    CharacterHit hit(ParagraphBox.CaretOffsetX x, double y) {
        // don't account for padding here since height of virtualFlow is used, not area + potential padding
        VirtualFlowHit<Cell<Paragraph<PS, SEG, S>, ParagraphBox<PS, SEG, S>>> hit = virtualFlow.hit(0.0, y);
        if (hit.isBeforeCells()) {
            return CharacterHit.insertionAt(0);
        } else if (hit.isAfterCells()) {
            return CharacterHit.insertionAt(getLength());
        } else {
            int parIdx = hit.getCellIndex();
            int parOffset = getParagraphOffset(parIdx);
            ParagraphBox<PS, SEG, S> cell = hit.getCell().getNode();
            Point2D cellOffset = hit.getCellOffset();
            CharacterHit parHit = cell.hitText(x, cellOffset.getY());
            return parHit.offset(parOffset);
        }
    }

    final Optional<Bounds> getSelectionBoundsOnScreen(Selection<PS, SEG, S> selection) {
        if (selection.getLength() == 0) {
            return Optional.empty();
        }

        List<Bounds> bounds = new ArrayList<>(selection.getParagraphSpan());
        for (int i = selection.getStartParagraphIndex(); i <= selection.getEndParagraphIndex(); i++) {
            virtualFlow.getCellIfVisible(i)
                    .ifPresent(c -> c.getNode()
                            .getSelectionBoundsOnScreen(selection)
                            .ifPresent(bounds::add)
                    );
        }

        if (bounds.size() == 0) {
            return Optional.empty();
        }
        double minX = bounds.stream().mapToDouble(Bounds::getMinX).min().getAsDouble();
        double maxX = bounds.stream().mapToDouble(Bounds::getMaxX).max().getAsDouble();
        double minY = bounds.stream().mapToDouble(Bounds::getMinY).min().getAsDouble();
        double maxY = bounds.stream().mapToDouble(Bounds::getMaxY).max().getAsDouble();
        return Optional.of(new BoundingBox(minX, minY, maxX - minX, maxY - minY));
    }

    void clearTargetCaretOffset() {
        caretSelectionBind.clearTargetOffset();
    }

    ParagraphBox.CaretOffsetX getTargetCaretOffset() {
        return caretSelectionBind.getTargetOffset();
    }

    //———————————————————————————————————————————私有———————————————————————————————————————————

    private Cell<Paragraph<PS, SEG, S>, ParagraphBox<PS, SEG, S>> createCell(
            Paragraph<PS, SEG, S> paragraph,
            BiConsumer<TextFlow, PS> applyParagraphStyle,
            Function<StyledSegment<SEG, S>, Node> nodeFactory) {

        ParagraphBox<PS, SEG, S> box = new ParagraphBox<>(paragraph, applyParagraphStyle, nodeFactory);

        box.highlightTextFillProperty().bind(highlightTextFill);
        box.wrapTextProperty().bind(wrapTextProperty());
        box.graphicFactoryProperty().bind(paragraphGraphicFactoryProperty());
        box.graphicOffset.bind(virtualFlow.breadthOffsetProperty());

        EventStream<Integer> boxIndexValues = box.indexProperty().values().filter(i -> i != -1);

        Subscription firstParPseudoClass = boxIndexValues.subscribe(idx -> box.pseudoClassStateChanged(FIRST_PAR, idx == 0));
        Subscription lastParPseudoClass = EventStreams.combine(
                boxIndexValues,
                getParagraphs().sizeProperty().values()
        ).subscribe(in -> in.exec((i, n) -> box.pseudoClassStateChanged(LAST_PAR, i == n - 1)));

        // set up caret
        Function<CaretNode, Subscription> subscribeToCaret = caret -> {
            EventStream<Integer> caretIndexStream = EventStreams.nonNullValuesOf(caret.paragraphIndexProperty());

            // a new event stream needs to be created for each caret added, so that it will immediately
            // fire the box's current index value as an event, thereby running the code in the subscribe block
            // Reusing boxIndexValues will not fire its most recent event, leading to a caret not being added
            // Thus, we'll call the new event stream "fresh" box index values
            EventStream<Integer> freshBoxIndexValues = box.indexProperty().values().filter(i -> i != -1);

            return EventStreams.combine(caretIndexStream, freshBoxIndexValues)
                    .subscribe(t -> {
                        int caretParagraphIndex = t.get1();
                        int boxIndex = t.get2();
                        if (caretParagraphIndex == boxIndex) {
                            box.caretsProperty().add(caret);
                        } else {
                            box.caretsProperty().remove(caret);
                        }
                    });
        };
        Subscription caretSubscription = caretSet.addSubscriber(subscribeToCaret);

        // TODO: how should 'hasCaret' be handled now?
        Subscription hasCaretPseudoClass = EventStreams
                .combine(boxIndexValues, Val.wrap(currentParagraphProperty()).values())
                // box index (t1) == caret paragraph index (t2)
                .map(t -> t.get1().equals(t.get2()))
                .subscribe(value -> box.pseudoClassStateChanged(HAS_CARET, value));

        Function<Selection<PS, SEG, S>, Subscription> subscribeToSelection = selection -> {
            EventStream<Integer> startParagraphValues = EventStreams.nonNullValuesOf(selection.startParagraphIndexProperty());
            EventStream<Integer> endParagraphValues = EventStreams.nonNullValuesOf(selection.endParagraphIndexProperty());

            // see comment in caret section about why a new box index EventStream is needed
            EventStream<Integer> freshBoxIndexValues = box.indexProperty().values().filter(i -> i != -1);
            return EventStreams.combine(startParagraphValues, endParagraphValues, freshBoxIndexValues)
                    .subscribe(t -> {
                        int startPar = t.get1();
                        int endPar = t.get2();
                        int boxIndex = t.get3();
                        if (startPar <= boxIndex && boxIndex <= endPar) {
                            //   So that we don't add multiple paths for the same selection,
                            //   which leads to not removing the additional paths when selection is removed,
                            // this is a `Map#putIfAbsent(Key, Value)` implementation that creates the path lazily
                            SelectionPath p = box.selectionsProperty().get(selection);
                            if (p == null) {
                                // create & configure path
                                Val<IndexRange> range = Val.create(
                                        () -> box.getIndex() != -1
                                                ? getParagraphSelection(selection, box.getIndex())
                                                : EMPTY_RANGE,
                                        selection.rangeProperty()
                                );
                                SelectionPath path = new SelectionPath(range);
                                path.getStyleClass().add(selection.getSelectionName());
                                selection.configureSelectionPath(path);

                                box.selectionsProperty().put(selection, path);
                            }
                        } else {
                            box.selectionsProperty().remove(selection);
                        }
                    });
        };
        Subscription selectionSubscription = selectionSet.addSubscriber(subscribeToSelection);

        return new Cell<Paragraph<PS, SEG, S>, ParagraphBox<PS, SEG, S>>() {
            @Override
            public ParagraphBox<PS, SEG, S> getNode() {
                return box;
            }

            @Override
            public void updateIndex(int index) {
                box.setIndex(index);
            }

            @Override
            public void dispose() {
                box.highlightTextFillProperty().unbind();
                box.wrapTextProperty().unbind();
                box.graphicFactoryProperty().unbind();
                box.graphicOffset.unbind();
                box.dispose();

                firstParPseudoClass.unsubscribe();
                lastParPseudoClass.unsubscribe();

                caretSubscription.unsubscribe();
                hasCaretPseudoClass.unsubscribe();

                selectionSubscription.unsubscribe();
            }
        };
    }

    /**
     * Assumes this method is called within a {@link #suspendVisibleParsWhile(Runnable)} block
     */
    private void followCaret() {
        int parIdx = getCurrentParagraph();
        ParagraphBox<PS, SEG, S> paragrafBox = virtualFlow.getCell(parIdx).getNode();

        Bounds caretBounds;
        try {
            // This is the default mechanism, but is also needed for https://github.com/FXMisc/RichTextFX/issues/1017
            caretBounds = paragrafBox.getCaretBounds(caretSelectionBind.getUnderlyingCaret());
        } catch (IllegalArgumentException EX) {
            // This is an alternative mechanism, to address https://github.com/FXMisc/RichTextFX/issues/939
            caretBounds = caretSelectionBind.getUnderlyingCaret().getLayoutBounds();
        }

        double graphicWidth = paragrafBox.getGraphicPrefWidth();
        Bounds region = extendLeft(caretBounds, graphicWidth);
        double scrollX = virtualFlow.getEstimatedScrollX();

        // Ordinarily when a caret ends a selection in the target paragraph and scrolling left is required to follow
        // the caret then the selection won't be visible. So here we check for this scenario and adjust if needed.
        if (!isWrapText() && scrollX > 0.0 && getParagraphSelection(parIdx).getLength() > 0) {
            CaretNode selectionStart = new CaretNode("", this, getSelection().getStart());
            paragrafBox.caretsProperty().add(selectionStart);
            Bounds startBounds = paragrafBox.getCaretBounds(selectionStart);
            paragrafBox.caretsProperty().remove(selectionStart);

            if (startBounds.getMinX() - graphicWidth < scrollX) {
                region = extendLeft(startBounds, graphicWidth);
            }
        }

        // Addresses https://github.com/FXMisc/RichTextFX/issues/937#issuecomment-674319602
        if (parIdx == getParagraphs().size() - 1 && paragrafBox.getLineCount() == 1) {
            region = new BoundingBox // Correcting the region's height
                    (
                            region.getMinX(), region.getMinY(), region.getWidth(),
                            paragrafBox.getLayoutBounds().getHeight()
                    );
        }

        virtualFlow.show(parIdx, region);
    }

    private ParagraphBox<PS, SEG, S> getCell(int index) {
        return virtualFlow.getCell(index).getNode();
    }

    private EventStream<MouseOverTextEvent> mouseOverTextEvents(ObservableSet<ParagraphBox<PS, SEG, S>> cells, Duration delay) {
        return merge(cells, c -> c.stationaryIndices(delay).map(e -> e.unify(
                l -> l.map((pos, charIdx) -> MouseOverTextEvent.beginAt(c.localToScreen(pos), getParagraphOffset(c.getIndex()) + charIdx)),
                r -> MouseOverTextEvent.end())));
    }

    private int getParagraphOffset(int parIdx) {
        return position(parIdx, 0).toOffset();
    }

    private Bounds getParagraphBoundsOnScreen(Cell<Paragraph<PS, SEG, S>, ParagraphBox<PS, SEG, S>> cell) {
        Bounds nodeLocal = cell.getNode().getBoundsInLocal();
        Bounds nodeScreen = cell.getNode().localToScreen(nodeLocal);
        Bounds areaLocal = getBoundsInLocal();
        Bounds areaScreen = localToScreen(areaLocal);

        // use area's minX if scrolled right and paragraph's left is not visible
        double minX = nodeScreen.getMinX() < areaScreen.getMinX()
                ? areaScreen.getMinX()
                : nodeScreen.getMinX();
        // use area's minY if scrolled down vertically and paragraph's top is not visible
        double minY = nodeScreen.getMinY() < areaScreen.getMinY()
                ? areaScreen.getMinY()
                : nodeScreen.getMinY();
        // use area's width whether paragraph spans outside of it or not
        // so that short or long paragraph takes up the entire space
        double width = areaScreen.getWidth();
        // use area's maxY if scrolled up vertically and paragraph's bottom is not visible
        double maxY = nodeScreen.getMaxY() < areaScreen.getMaxY()
                ? nodeScreen.getMaxY()
                : areaScreen.getMaxY();
        return new BoundingBox(minX, minY, width, maxY - minY);
    }

    private Optional<Bounds> getRangeBoundsOnScreen(int paragraphIndex, int from, int to) {
        return virtualFlow.getCellIfVisible(paragraphIndex)
                .map(c -> c.getNode().getRangeBoundsOnScreen(from, to));
    }

    private void manageSubscription(Subscription subscription) {
        subscriptions = subscriptions.and(subscription);
    }

    private static Bounds extendLeft(Bounds b, double w) {
        if (w == 0) {
            return b;
        } else {
            return new BoundingBox(
                    b.getMinX() - w, b.getMinY(),
                    b.getWidth() + w, b.getHeight());
        }
    }

    private void suspendVisibleParsWhile(Runnable runnable) {
        Suspendable.combine(beingUpdated, visibleParagraphs).suspendWhile(runnable);
    }

}
