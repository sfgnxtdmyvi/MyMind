package myMind.constants;

import java.awt.GraphicsEnvironment;

public class SizeConstants {
    public static final int MIN_NODE_WIDTH = 125;
    public static final int MAX_NODE_WIDTH = 922;
    public static final int MIN_NODE_HEIGHT = 50;

    public static final int MIN_TEXTAREA_HEIGHT = MIN_NODE_HEIGHT - 22;
    public static final int HALF_MIN_TEXTAREA_HEIGHT = MIN_TEXTAREA_HEIGHT / 2;

    public static final double SCALE = GraphicsEnvironment.getLocalGraphicsEnvironment()
            .getDefaultScreenDevice()
            .getDefaultConfiguration()
            .getDefaultTransform()
            .getScaleX();

    public static final int NODE_GAP_X = MIN_NODE_WIDTH / 4;
    public static final int NODE_GAP_Y = MIN_NODE_WIDTH / 5;
    public static final int ADD_LEFT_NODE_GAP_X = NODE_GAP_X + MIN_NODE_WIDTH;

    public static final int FONT_SIZE = 20;
    public static final int LINE_HEIGHT = (int) (FONT_SIZE * 1.35);

}
