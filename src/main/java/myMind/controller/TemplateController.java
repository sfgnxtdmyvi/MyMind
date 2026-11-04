package myMind.controller;

import com.alibaba.fastjson2.JSONObject;
import javafx.beans.property.MapProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCombination;
import myMind.common.constants.ConfigConstants;
import myMind.common.constants.FileConstants;
import myMind.common.constants.PosConstants;
import myMind.common.util.CloneNodeUtil;
import myMind.common.util.FileUtil;
import myMind.componet.MapNode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TemplateController {

    public static final MapProperty<String, MapNode> mapProperty =
            new SimpleMapProperty<>(FXCollections.observableHashMap());
    public static final List<String> orderList = new ArrayList<>();

    @FXML
    public Menu template;

    @FXML
    public void initialize() {
        // 加载
        File file = new File(ConfigConstants.FILE_TEMPLATES);
        JSONObject templateJson = FileUtil.readJson(file);
        for (Map.Entry<String, Object> entry : templateJson.entrySet()) {
            JSONObject jsonObject = (JSONObject) entry.getValue();
            MapNode parentNode = FileUtil.buildNode(jsonObject, PosConstants.RIGHT);
            FileUtil.loadChild(jsonObject.getJSONArray(FileConstants.CHILDREN_R), parentNode, PosConstants.RIGHT);
            FileUtil.loadChild(jsonObject.getJSONArray(FileConstants.CHILDREN_L), parentNode, PosConstants.RIGHT);
            orderList.add(entry.getKey());
            mapProperty.put(entry.getKey(), parentNode);
        }
        refreshTemplate();

        mapProperty.addListener((MapChangeListener<String, MapNode>) change -> {
            refreshTemplate();

            // 保存
            JSONObject toSaveJson = new JSONObject();
            for (String key : orderList) {
                MapNode mapNode = mapProperty.get(key);

                JSONObject parentJson = FileUtil.saveNode(mapNode);
                FileUtil.saveChildren(parentJson, mapNode.getChildrenR(), FileConstants.CHILDREN_R, PosConstants.RIGHT);
                FileUtil.saveChildren(parentJson, mapNode.getChildrenL(), FileConstants.CHILDREN_L, PosConstants.LEFT);

                toSaveJson.put(key, parentJson);
            }

            FileUtil.saveJson(file, toSaveJson, false);
        });
    }

    private void refreshTemplate() {
        ObservableList<MenuItem> items = template.getItems();

        if (mapProperty.isEmpty()) {
            items.add(new MenuItem("无模板"));
            template.setUserData("1");
            return;
        } else if (template.getUserData() != null) {
            items.remove(0);
            template.setUserData(null);
        }

        for (int i = 0; i < items.size(); i++) {
            String key = orderList.get(i);
            items.get(i).setOnAction((event) ->
                    CloneNodeUtil.setCloneNode(mapProperty.get(key).clone()));
        }
        for (int i = items.size(); i < orderList.size(); i++) {
            String key = orderList.get(i);
            MenuItem item = new MenuItem(key);
            item.setOnAction((event) -> CloneNodeUtil.setCloneNode(mapProperty.get(key).clone()));
            // 添加 item 后会装上一个监听器，
            // 在 setAccelerator 时，这个监听器会认为之前已经有一个快捷键了，它会把旧的快捷键的逻辑交给新快捷键，
            // 如果此前并没有快捷键，也就没有对应的逻辑，监听器会把 null 交给新快捷键，即按下快捷键后什么都不做
            // 如果是初始化时先添加 item 再 setAccelerator 不会有这个问题，此时没有 Scene，快捷键会在之后添加
            item.setAccelerator(KeyCombination.keyCombination("Ctrl+" + (items.size() + 1)));
            items.add(item);
        }
    }

    public static void addTemplate(MapNode mapNode) {
        String key = "模板" + (mapProperty.size() + 1);
        orderList.add(key);
        mapProperty.put(key, mapNode);
    }

}
