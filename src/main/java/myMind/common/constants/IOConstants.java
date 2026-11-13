package myMind.common.constants;

import javafx.scene.image.Image;

public class IOConstants {

    public static final byte SAVE_TYPE = 0;
    public static final byte OPEN_TYPE = 1;

    public static final String OUTGOING_REFERENCE = "outgoingReference";
    public static final String SUBJECT_ID = "subjectId";
    public static final String ID = "id";

    public static final String CHILDREN_R = "childrenR";
    public static final String CHILDREN_L = "childrenL";
    public static final String CHILDREN = "children";

    public static final String IMAGE_NAME = "imageName";
    public static final String IMAGE_WIDTH = "imageWidth";
    public static final String IMAGE_HEIGHT = "imageHeight";

    public static final String TEXT = "text";
    public static final String STYLES = "styles";
    public static final String STYLE = "style";
    public static final String START = "start";
    public static final String END = "end";

    public static final Image ICON = new Image(IOConstants.class.getResourceAsStream("/icon.png"));

}
