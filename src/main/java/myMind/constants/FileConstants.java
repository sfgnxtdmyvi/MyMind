package myMind.constants;

import java.util.ResourceBundle;

public class FileConstants {

    public static final byte SAVE_TYPE = 0;
    public static final byte OPEN_TYPE = 1;

    public static final String DIR_FILES;
    public static final String DIR_IMAGE;
    public static final String DIR_RECENT_FILES;

    static {
        ResourceBundle config = ResourceBundle.getBundle("config");
        DIR_FILES = config.getString("directory.files");
        DIR_IMAGE = config.getString("directory.images");
        DIR_RECENT_FILES = config.getString("directory.recent_files");
    }

}
