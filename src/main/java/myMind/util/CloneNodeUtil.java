package myMind.util;

import myMind.componet.MindNode;

public class CloneNodeUtil {
    private static final ThreadLocal<MindNode> nodeHolder = new ThreadLocal<>();

    public static void setNode(MindNode node) {
        nodeHolder.set(node);
    }

    public static MindNode getNode() {
        MindNode mindNode = nodeHolder.get();
        nodeHolder.remove();
        return mindNode;
    }
}
