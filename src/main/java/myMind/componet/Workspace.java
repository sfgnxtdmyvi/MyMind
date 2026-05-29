package myMind.componet;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import lombok.Getter;
import myMind.controller.MenuController;
import myMind.controller.StyleWheelArcController;
import myMind.controller.SubjectController;

public class Workspace extends TabPane {
    @Getter
    private SubjectController subjectController;

    public Workspace() {
        //关闭按钮的显示策略
        //SELECTED_TAB：只在当前被选中的标签页显示
        //ALL_TABS：在所有标签页上都显示
        //UNAVAILABLE：完全不显示
        setTabClosingPolicy(TabClosingPolicy.ALL_TABS);
        getStyleClass().add("hide-tabs");
        addSubject();
        Platform.runLater(() -> subjectController.getSelectedNode().getTextArea().requestFocus());

        addListener();
    }

    private void addListener() {
        getSelectionModel().selectedItemProperty().addListener((observable, oldtab, newTab) -> {
            if (newTab == null) {
                return;
            }
            subjectController = ((Subject) newTab.getContent()).getSubjectController();
            MenuController.setSubjectController(subjectController);
            StyleWheelArcController.setSubjectController(subjectController);
        });

        getTabs().addListener((ListChangeListener.Change<? extends Tab> c) -> {
            //只有一个主题时，隐藏标签栏
            if (getTabs().size() == 1) {
                getStyleClass().add("hide-tabs");
            } else {
                getStyleClass().remove("hide-tabs");
            }
        });
    }

    public void addSubject() {
        subjectController = new SubjectController();
        Subject subject = subjectController.getSubject();
        MenuController.setSubjectController(subjectController);

        String subjectName = "主题" + (getTabs().size() + 1);
        Tab tab = new Tab(subjectName);
        tab.setContent(subject);

        getTabs().add(tab);
        getSelectionModel().select(tab);

        // todo 动态计算中心点
//        Platform.runLater(() -> {
//            double centerX = (getWidth() - SizeConstants.MIN_NODE_WIDTH) / 2.0;
//            double centerY = getHeight() / 2.0 - SizeConstants.MIN_NODE_HEIGHT;
//        });
        subjectController.initRootNode(670, 311);
        subjectController.getRootNode().getTextArea().textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.isEmpty()) {
                tab.setText(newValue);
            } else {
                tab.setText(subjectName);
            }
        });
    }
}
