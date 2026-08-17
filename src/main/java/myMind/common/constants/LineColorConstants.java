package myMind.common.constants;

import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LineColorConstants {
    private static final List<Paint> lineColors = new ArrayList<>();

    static {
        Collections.addAll(lineColors,
//                Color.web("#fdb466"),
                Color.web("#64cab9"),
                Color.web("#f6b7dd"),
                Color.web("#a3daa6"),
                Color.web("#cd86e6"),
                Color.web("#b5b5f6"),
                Color.web("#5a86a8"),
                Color.web("#fa8d98"),
                Color.web("#62c9ce"),
                Color.web("#fda6b2"),
                Color.web("#fad677"),
                Color.web("#fa985e"));
    }

    public static Paint getColor(int index) {
        return lineColors.get(index % lineColors.size());
    }

}
