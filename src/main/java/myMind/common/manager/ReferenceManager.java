package myMind.common.manager;

import lombok.Getter;
import lombok.Setter;
import myMind.componet.MapNode;

import java.util.HashMap;
import java.util.Map;

public class ReferenceManager {

    @Getter
    @Setter
    private static boolean isReferencing;

    @Getter
    @Setter
    private static MapNode srcNode;

    /**
     * key：引用其他节点的节点，value：被引用的节点的 id
     */
    private static Map<MapNode, Long> incomingReferences;
    /**
     * key：被引用的节点的 id，value：被引用的节点
     */
    private static Map<Long, MapNode> outgoingReferences;

    //———————————————————————————————————————————加载———————————————————————————————————————————

    public static void prepare() {
        incomingReferences = new HashMap<>();
        outgoingReferences = new HashMap<>();
    }

    public static void addIncomingReference(MapNode node, long nodeId) {
        incomingReferences.put(node, nodeId);
    }

    public static void addOutgoingReference(MapNode node) {
        outgoingReferences.put(node.getNodeId(), node);
    }

    public static void link() {
        for (Map.Entry<MapNode, Long> entry : incomingReferences.entrySet()) {
            MapNode srcNode = entry.getKey();
            MapNode targetNode = outgoingReferences.get(entry.getValue());
            srcNode.setOutgoingReference(targetNode);
            targetNode.addIncomingReference(srcNode);
        }
        incomingReferences = null;
        outgoingReferences = null;
    }

}
