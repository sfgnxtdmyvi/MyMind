package myMind.common.manager;

import javafx.scene.control.Tab;
import lombok.Getter;
import lombok.Setter;
import myMind.componet.MindMap;
import myMind.componet.MindNode;
import myMind.componet.Subject;
import myMind.controller.SubjectController;

public class QuoteManager {

    @Getter
    @Setter
    private static boolean isQuoting;
    @Setter
    private static MindMap mindMap;
    @Getter
    @Setter
    private static MindNode srcNode;
    @Getter
    @Setter
    private static int subjectIndex;
    @Getter
    @Setter
    private static double subjectTranslateX;
    @Getter
    @Setter
    private static double subjectTranslateY;

    public static void back(){
        mindMap.getSelectionModel().select(subjectIndex);
        Tab tab = mindMap.getTabs().get(subjectIndex);
        SubjectController subjectController = (SubjectController) tab.getUserData();
        Subject subject = subjectController.getSubject();
        subject.setTranslateX(subjectTranslateX);
        subject.setTranslateY(subjectTranslateY);
    }
}
