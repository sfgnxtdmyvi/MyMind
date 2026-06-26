package myMind.controller;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.IndexRange;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Arc;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Popup;
import lombok.Setter;
import org.fxmisc.richtext.StyleClassedTextArea;

import java.util.ArrayList;
import java.util.List;

public class StyleWheelArcController {
    @FXML
    private Popup styleWheel;
    @FXML
    private Text BText;

    @Setter
    private static SubjectController subjectController;

    @FXML
    public void initialize() {
        ObservableList<Node> contentList = styleWheel.getContent();
        Pane content = (Pane) contentList.get(0);
        for (var node : content.getChildren()) {
            if (node instanceof Arc arc) {
                arc.setOnMouseClicked(this::toggleStyle);
                arc.setOnMouseEntered(this::expandArc);
                arc.setOnMouseExited(this::shrinkArc);
            }

            if (node instanceof Circle circle) {
                circle.setOnMouseClicked(this::toggleStyle);
                circle.setOnMouseEntered(this::expandCircle);
                circle.setOnMouseExited(this::shrinkCircle);
            }
        }
        styleWheel.setAutoHide(true);
        styleWheel.setOnHidden(event -> subjectController.getSelectedNode().getTextArea().deselect());
    }

    @FXML
    private void toggleStyle(MouseEvent event) {
        StyleClassedTextArea textArea = subjectController.getSelectedNode().getTextArea();

        Object source = event.getSource();
        String style;
        if (source instanceof Arc) {
            style = (String) ((Arc) source).getUserData();
        } else {
            style = (String) ((Circle) source).getUserData();
        }

        IndexRange selection = textArea.getSelection();
        int start = selection.getStart();
        // getStyleAtPosition(p) is equivalent to getStyleOfChar(p-1)
        // 用 getStyleAtPosition 获取的是指定位置的前一个位置的样式
        // List<String> styles = new ArrayList<>(textArea.getStyleAtPosition(start + 1));
        List<String> styles = new ArrayList<>(textArea.getStyleOfChar(start));
        // 点击加粗：有加粗则取消，无加粗则加粗
        // 点击颜色：改成点击的颜色，有加粗和引用则保留，黑色则不用添加
        boolean containsBold = styles.contains("bold-text");
        boolean containsQuote = styles.contains("quote");
        if (style.equals("bold-text")) {
            if (containsBold) {
                styles.remove("bold-text");
            } else {
                styles.add("bold-text");
            }
        } else {
            styles.clear();
            if (!style.equals("black-text")) {
                styles.add(style);
            }
            if (containsBold) {
                styles.add("bold-text");
            }
            if(containsQuote){
                styles.add("quote");
            }
        }
        textArea.setStyle(start, selection.getEnd(), styles);
        styleWheel.hide();
    }

    //—————————————————————————————————————————轮盘悬浮行为—————————————————————————————————————————
    @FXML
    public void expandArc(MouseEvent event) {
        Arc arc = (Arc) event.getSource();
        // 往外扩大
        arc.setRadiusX(50);
        arc.setRadiusY(50);
        // 往两边扩大
        arc.setStartAngle(arc.getStartAngle() - 5);
        arc.setLength(70);

        // 不能直接移除，再添加到倒数第三的位置
        // 移除会触发 mouseExited，添加会触发 mouseEntered，导致死循环
        // toFront() 内部通过 childrenTriggerPermutation 控制不触发事件调度，但是要 remove 和 add 3次
        // viewOrder 的值越小，节点越靠前（绘制在更上层）。默认值是 0
        arc.setViewOrder(-1);
    }

    @FXML
    public void shrinkArc(MouseEvent event) {
        Arc arc = (Arc) event.getSource();
        arc.setRadiusX(40);
        arc.setRadiusY(40);
        arc.setLength(60);
        arc.setStartAngle(arc.getStartAngle() + 5);
        arc.setViewOrder(0);
    }

    @FXML
    public void expandCircle(MouseEvent event) {
        Circle circle = (Circle) event.getSource();
        circle.setRadius(20);
        BText.setFont(new Font("System Bold", 20));
        BText.setX(118);
        BText.setY(132);
    }

    @FXML
    public void shrinkCircle(MouseEvent event) {
        Circle circle = (Circle) event.getSource();
        circle.setRadius(15);
        BText.setFont(new Font("System Bold", 16));
        BText.setX(120);
        BText.setY(130);
    }
}