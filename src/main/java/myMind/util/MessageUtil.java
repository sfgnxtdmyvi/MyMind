package myMind.util;

import javafx.animation.PauseTransition;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

/**
 * 消息提示标签
 */
public class MessageUtil {
    private static final Label label;
    private static Pane parentPane;
    private static PauseTransition hideTimer;

    static {
        label = new Label();
        label.getStyleClass().add("message");
        label.setVisible(false);
        label.setManaged(false);

        hideTimer = new PauseTransition(Duration.seconds(3));
        hideTimer.setOnFinished(event -> {
            label.setVisible(false);
            label.setManaged(false);
        });
    }

    /**
     * 初始化并添加到父容器
     */
    public static void init(Pane parentPane) {
        MessageUtil.parentPane = parentPane;
        parentPane.getChildren().add(label);
    }

    /**
     * 显示缩放比例
     *
     * @param scale 缩放比例 (0.1 - 3.0)
     */
    public static void show(double scale) {
        if (parentPane == null) {
            return;
        }

        int percentage = (int) Math.round(scale * 100);
        label.setText(percentage + "%");
        show();
    }

    /**
     * 显示消息
     *
     * @param message 消息
     */
    public static void show(String message) {
        if (parentPane == null) {
            return;
        }

        label.setText(message);
        show();
    }

    private static void show() {
        label.setVisible(true);
        label.setManaged(true);

        // 中间上方
        double labelWidth = label.prefWidth(-1);
        double labelHeight = label.prefHeight(-1);
        label.setLayoutX((parentPane.getWidth() - labelWidth) / 2);
        label.setLayoutY(labelHeight * 2);

        // 重置定时器
        hideTimer.stop();
        hideTimer.play();
    }
}
