package myMind.common.manager;

import javafx.scene.control.Tab;
import lombok.Getter;
import lombok.Setter;
import myMind.componet.MapNode;
import myMind.componet.MindMap;
import myMind.componet.Subject;
import myMind.controller.SubjectController;

import java.util.HashMap;
import java.util.Map;

public class ReferenceManager {

    @Getter
    @Setter
    private static boolean isReferencing;

    @Setter
    private static MindMap mindMap;
    private static Long subjectId;
    private static double subjectTranslateX;
    private static double subjectTranslateY;

    @Getter
    private static MapNode srcNode;

    /**
     * key：引用其他节点的节点，value：被引用的节点的 id
     */
    private static Map<MapNode, Long> incomingReferences;
    /**
     * key：被引用的节点的 id，value：被引用的节点
     */
    private static Map<Long, MapNode> outgoingReferences;

    /**
     * 记录原位置
     */
    public static void setSrc(MindMap mindMap, Subject subject, MapNode srcNode) {
        ReferenceManager.isReferencing = true;
        setSrc(mindMap, subject);
        ReferenceManager.srcNode = srcNode;
    }

    public static void setSrc(MindMap mindMap, Subject subject) {
        ReferenceManager.mindMap = mindMap;
        ReferenceManager.subjectId = subject.getSubjectId();
        ReferenceManager.subjectTranslateX = subject.getTranslateX();
        ReferenceManager.subjectTranslateY = subject.getTranslateY();
    }

    /**
     * 回到引用处
     */
    public static void back() {
        if (subjectId == null) {
            return;
        }

        Tab tab = mindMap.jumpToSubject(subjectId);
        SubjectController subjectController = (SubjectController) tab.getUserData();
        Subject subject = subjectController.getSubject();
        subject.setTranslateX(subjectTranslateX);
        subject.setTranslateY(subjectTranslateY);
        subjectId = null;
    }

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
