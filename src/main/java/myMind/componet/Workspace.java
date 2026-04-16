package myMind.componet;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import myMind.constants.SizeConstants;
import myMind.controller.NodeController;

public class Workspace extends TabPane {

    public Workspace() {
        addNewTab();

        setOnKeyPressed(e -> {
            if (e.isControlDown() && e.getCode() == KeyCode.M) {
                addNewTab();
                e.consume();
            }
        });
    }

    public void addNewTab() {
        NodeController nodeController = new NodeController();
        Subject subject = nodeController.getSubject();

        int index = getTabs().size() + 1;
        Tab tab = new Tab("主题" + index);
        tab.setContent(subject);

        getTabs().add(tab);
        getSelectionModel().select(tab);

        Platform.runLater(() -> {
            double centerX = (getWidth() - SizeConstants.MIN_NODE_WIDTH) / 2.0;
            double centerY = getHeight() / 2.0 - SizeConstants.MIN_NODE_HEIGHT;
            nodeController.initRootNode(centerX, centerY);
        });
    }

    /**
     * 获取当前选中标签页的控制器
     */
    public NodeController getCurrentController() {
        Tab selectedTab = getSelectionModel().getSelectedItem();
        return ((Subject) selectedTab.getContent()).getController();
    }
}
