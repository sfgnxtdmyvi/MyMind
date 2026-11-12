package myMind.common.constants;

import javafx.stage.Screen;

import java.util.ResourceBundle;

public class ConfigConstants {

    public static final String DIR_FILES;
    public static final String DIR_IMAGES;
    public static final String DIR_DELETE_IMAGES;

    public static final String FILE_CSS_PROPERTIES;

    public static final String FILE_RECENT_FILES;
    public static final String FILE_SHORTCUTS;
    public static final String FILE_TEMPLATES;

    public static final double SCALE = Screen.getPrimary().getOutputScaleX();

    static {
        ResourceBundle config = ResourceBundle.getBundle("properties/config");
        DIR_FILES = config.getString("dir.files");
        DIR_IMAGES = config.getString("dir.images");
        DIR_DELETE_IMAGES = config.getString("dir.deleteImages");

        FILE_CSS_PROPERTIES = config.getString("file.css.properties");

        FILE_RECENT_FILES = config.getString("file.recent_files");
        FILE_SHORTCUTS = config.getString("file.shortcuts");
        FILE_TEMPLATES = config.getString("file.templates");
    }
}
