package myMind.util;

import lombok.Getter;

import java.awt.*;

public class ScreenUtil {
    private static Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

    //在类上的@Getter 注解只会为实例字段（非 static）生成 getter 方法
    @Getter
    private static int width = (int) screenSize.getWidth();
    @Getter
    private static int height = (int) screenSize.getHeight();
    @Getter
    private static int widthCenter = width / 2;
    @Getter
    private static int heightCenter = height / 2;

}
