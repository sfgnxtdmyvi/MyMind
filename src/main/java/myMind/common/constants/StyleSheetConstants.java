package myMind.common.constants;

import java.util.List;

public class StyleSheetConstants {

    public static final List<String> MAIN = List.of(
            StyleSheetConstants.class.getResource("/css/base.css").toExternalForm(),
            StyleSheetConstants.class.getResource("/css/node.css").toExternalForm(),
            StyleSheetConstants.class.getResource("/css/search-panel.css").toExternalForm(),
            StyleSheetConstants.class.getResource("/css/style-wheel.css").toExternalForm(),
            StyleSheetConstants.class.getResource("/css/title-bar.css").toExternalForm()
    );

    public static final List<String> SNAPSHOT = List.of(
            StyleSheetConstants.class.getResource("/css/base.css").toExternalForm(),
            StyleSheetConstants.class.getResource("/css/node.css").toExternalForm()
    );

    public static final String DIALOG = StyleSheetConstants.class.getResource("/css/dialog.css").toExternalForm();

}
