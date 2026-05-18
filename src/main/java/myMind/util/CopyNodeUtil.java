package myMind.util;

import myMind.componet.MindNode;

public class CopyNodeUtil {
    private static final ThreadLocal<MindNode> threadLocal = new ThreadLocal<>();
    public static void set(MindNode node) {
        threadLocal.set(node);
    }
    public static MindNode get() {
        MindNode mindNode = threadLocal.get();
        threadLocal.remove();
        return mindNode;
    }
}
