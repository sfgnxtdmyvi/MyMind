package myMind.util;

import myMind.componet.MindNode;
import myMind.componet.NodeModel;

import java.util.HashMap;
import java.util.Map;

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

    private static final ThreadLocal<Map<NodeModel, MindNode>> mapHolder = ThreadLocal.withInitial(() -> new HashMap<>());

    public static void putMap(NodeModel model, MindNode node) {
        mapHolder.get().put(model, node);
    }

    public static Map<NodeModel, MindNode> getMap() {
        Map<NodeModel, MindNode> map = mapHolder.get();
        mapHolder.remove();
        mapHolder.set(new HashMap<>());
        return map;
    }
}
