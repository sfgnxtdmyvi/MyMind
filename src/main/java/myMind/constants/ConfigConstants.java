package myMind.constants;

import java.util.ResourceBundle;

public class ConfigConstants {

    public static final String DIR_FILES;
    public static final String DIR_IMAGE;
    public static final String DIR_RECENT_FILES;
    public static final String DIR_NOTES;
    public static final String DIR_SHORTCUTS;
    public static final String DIR_CSS_PROPERTIES;

    static {
        ResourceBundle config = ResourceBundle.getBundle("properties/config");
        DIR_FILES = config.getString("dir.files");
        DIR_IMAGE = config.getString("dir.images");
        DIR_RECENT_FILES = config.getString("dir.recent_files");
        DIR_SHORTCUTS = config.getString("dir.shortcuts");
        DIR_NOTES = config.getString("dir.notes");
        DIR_CSS_PROPERTIES = config.getString("dir.css.properties");
    }
}
