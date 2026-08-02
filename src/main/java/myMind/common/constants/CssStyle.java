package myMind.common.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum CssStyle {

    TAB_LIGHT("TAB_LIGHT", "-tab-header-background-color: #ededed;" +
            "-tab-background-color: #f4f4f4;" +
            "-tab-selected-background-color: #ffffff;" +
            "-tab-selected-border-color: #5381dd;");

    private final String styleName;
    private final String value;

    /**
     * 通过配置文件中的样式名字获取样式
     */
    public static String getStyle(String styleName) {
        for (CssStyle css : CssStyle.values()) {
            if (css.getStyleName().equals(styleName)) {
                return css.getValue();
            }
        }

        return "";
    }
}
