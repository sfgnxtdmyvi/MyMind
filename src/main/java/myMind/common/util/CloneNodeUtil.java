package myMind.common.util;

import myMind.componet.MapNode;

public class CloneNodeUtil {
    private static final ThreadLocal<MapNode> nodeHolder = new ThreadLocal<>();

    public static void setNode(MapNode node) {
        nodeHolder.set(node);
    }

    public static MapNode getNode() {
        MapNode mapNode = nodeHolder.get();
        nodeHolder.remove();
        return mapNode;
    }
}
