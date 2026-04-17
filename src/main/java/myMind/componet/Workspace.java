package myMind.componet;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import myMind.constants.SizeConstants;
import myMind.controller.NodeController;

public class Workspace extends TabPane {

    public Workspace() {
        //关闭按钮的显示策略
        //SELECTED_TAB：只在当前被选中的标签页显示
        //ALL_TABS：在所有标签页上都显示
        //UNAVAILABLE：完全不显示
        setTabClosingPolicy(TabClosingPolicy.ALL_TABS);
        getStyleClass().add("hide-tabs");
        addNewTab();

        addListener();
    }

    private void addListener() {
        getTabs().addListener((ListChangeListener.Change<? extends Tab> c) -> {
            //只有一个主题时，隐藏标签栏
            if (getTabs().size() <= 1) {
                getStyleClass().add("hide-tabs");
            } else {
                getStyleClass().remove("hide-tabs");
            }
        });

        //新增主题
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
