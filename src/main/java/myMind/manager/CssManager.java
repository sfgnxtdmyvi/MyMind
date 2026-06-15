package myMind.manager;

import lombok.Getter;
import lombok.Setter;
import myMind.constants.ConfigConstants;
import myMind.util.MessageUtil;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

public class CssManager {
    private static Properties props;

    @Getter
    @Setter
    private static String styleWheel;
    @Getter
    @Setter
    private static String tabStyle;

    static {
        try (InputStream input = new FileInputStream(ConfigConstants.DIR_CSS_PROPERTIES)) {
            props = new Properties();
            props.load(input);
        } catch (IOException e) {
            e.printStackTrace();
        }

        styleWheel = props.getProperty("styleWheel");
        tabStyle = props.getProperty("tabStyle");
    }

    public static void setCss(String style, String value) {
        try (OutputStream output = new FileOutputStream(ConfigConstants.DIR_CSS_PROPERTIES)) {
            props.setProperty(style, value);
            props.store(output, "");
        } catch (IOException e) {
            MessageUtil.showMessage("改变样式失败：" + e.getMessage());
        }
    }
}
