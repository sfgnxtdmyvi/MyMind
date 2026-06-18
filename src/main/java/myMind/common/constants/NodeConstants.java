package myMind.common.constants;

import java.awt.GraphicsEnvironment;

public class NodeConstants {

    public static final double MIN_NODE_WIDTH = 130;
    public static final double MIN_NODE_HEIGHT = 53;

    public static final double MIN_TEXTAREA_WIDTH = 1;
    public static final double MAX_TEXTAREA_WIDTH = 1000;
    public static final double MIN_TEXTAREA_HEIGHT = MIN_NODE_HEIGHT - 22;
    public static final double HALF_MIN_TEXTAREA_HEIGHT = MIN_TEXTAREA_HEIGHT / 2;

    public static final int PADDING = 12;
    public static final int BORDER_AND_PADDING = 2 + PADDING * 2;

    public static final double GAP_X = MIN_NODE_WIDTH / 4;
    public static final double GAP_Y = MIN_NODE_WIDTH / 5;
    public static final double ADD_LEFT_NODE_GAP_X = GAP_X + MIN_NODE_WIDTH;

    public static final double TRANSLATE_RATE = 0.7453;
    public static final double NODE_TRANSLATE = MIN_NODE_HEIGHT * TRANSLATE_RATE;
    public static final double FIVE_NODE_TRANSLATE = NODE_TRANSLATE * 5;
    public static final double FOUR_NODE_TRANSLATE = NODE_TRANSLATE * 4;

    public static final double SCALE = GraphicsEnvironment.getLocalGraphicsEnvironment()
            .getDefaultScreenDevice()
            .getDefaultConfiguration()
            .getDefaultTransform()
            .getScaleX();

    public static final int FONT_SIZE = 20;

    // 拖拽缩放
    public static final double RESIZE_THRESHOLD = 10.0;
    public static final double BUTTON_THRESHOLD = 15.0;

    public static double CENTER_X;
    public static double CENTER_Y;

    public static String ADD = "+";
    public static String CLOSE = "×";
    public static String EXPAND_R = "▸";
    public static String EXPAND_L = "◂";

}
