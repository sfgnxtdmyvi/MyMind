package myMind.manager;

import java.util.ResourceBundle;

public class ConfigManager {

    public static final String STYLE_WHEEL;
    public static final String DIR_FILES;
    public static final String DIR_IMAGE;
    public static final String DIR_RECENT_FILES;
    public static final String SHORTCUTS;

    static {
        ResourceBundle config = ResourceBundle.getBundle("properties/config");
        STYLE_WHEEL = config.getString("styleWheel");
        DIR_FILES = config.getString("directory.files");
        DIR_IMAGE = config.getString("directory.images");
        DIR_RECENT_FILES = config.getString("directory.recent_files");
        SHORTCUTS = config.getString("directory.shortcuts");
    }
}
