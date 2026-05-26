package myMind.util;

import javafx.animation.PauseTransition;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * 消息提示标签
 */
public class MessageUtil {
    private static Label label;
    private static PauseTransition hideTimer;
    private static double parentPaneWidth;

    /**
     * 初始化并添加到父容器
     */
    public static void init(Pane parentPane, Stage stage) {
        Label label = new Label();
        label.getStyleClass().add("message");
        label.setVisible(false);
        label.setManaged(false);
        parentPane.getChildren().add(label);


        PauseTransition hideTimer = new PauseTransition(Duration.seconds(3));
        hideTimer.setOnFinished(event -> {
            label.setVisible(false);
            label.setManaged(false);
        });

        stage.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                MessageUtil.label = label;
                MessageUtil.hideTimer = hideTimer;
                MessageUtil.parentPaneWidth = parentPane.getWidth();
            }
        });
    }

    /**
     * 显示缩放比例
     *
     * @param scale 缩放比例 (0.1 - 3.0)
     */
    public static void showScale(double scale) {
        int percentage = (int) Math.round(scale * 100);
        showMessage(percentage + "%");
    }

    /**
     * 显示消息
     *
     * @param message 消息
     */
    public static void showMessage(String message) {
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);

        // 中间上方
        label.setLayoutX((parentPaneWidth - label.prefWidth(-1)) / 2);
        label.setLayoutY(label.prefHeight(-1) * 2);

        hideTimer.stop();
        hideTimer.play();
    }
}
