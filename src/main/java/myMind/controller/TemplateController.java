package myMind.controller;

import com.alibaba.fastjson2.JSONObject;
import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import myMind.common.constants.ConfigConstants;
import myMind.common.constants.IOConstants;
import myMind.common.constants.PosConstants;
import myMind.common.constants.StyleSheetConstants;
import myMind.common.constants.TemplateConstants;
import myMind.common.pojo.Template;
import myMind.common.util.CloneNodeUtil;
import myMind.common.util.IOUtil;
import myMind.common.util.MessageUtil;
import myMind.componet.MapNode;
import myMind.componet.PrettyDialog;
import myMind.componet.Subject;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Map;

public class TemplateController {

    public static final ListProperty<Template> templates =
            new SimpleListProperty<>(FXCollections.observableList(new ArrayList<>()));

    @FXML
    public Menu templatesMenu;

    static {
        File file = new File(ConfigConstants.FILE_TEMPLATES);
        JSONObject templateJson = IOUtil.readJson(file);
        for (Map.Entry<String, Object> entry : templateJson.entrySet()) {
            JSONObject jsonObject = (JSONObject) entry.getValue();
            MapNode parentNode = IOUtil.buildNode(jsonObject, PosConstants.RIGHT);
            IOUtil.loadChild(jsonObject.getJSONArray(IOConstants.CHILDREN), parentNode, PosConstants.RIGHT);
            Template template = new Template(parentNode,
                    entry.getKey(),
                    jsonObject.getString(TemplateConstants.PREVIEW),
                    jsonObject.getDoubleValue(TemplateConstants.RATIO));
            templates.add(template);
        }
    }

    @FXML
    public void initialize() {
        File file = new File(ConfigConstants.FILE_TEMPLATES);
        refresh();

        // 当唯一数据源变动时，更新所有导图
        templates.addListener((ListChangeListener<Template>) change -> {
            refresh();

            // 保存
            JSONObject toSaveJson = new JSONObject();
            for (Template template : templates) {
                MapNode mapNode = template.getMapNode();
                JSONObject parentJson = IOUtil.saveNode(mapNode);
                IOUtil.saveChildren(parentJson, mapNode.getChildrenR(), IOConstants.CHILDREN, PosConstants.RIGHT);
                parentJson.put(TemplateConstants.PREVIEW, template.getPreview());
                parentJson.put(TemplateConstants.RATIO, template.getRatio());
                toSaveJson.put(template.getName(), parentJson);
            }
            IOUtil.saveJson(file, toSaveJson, false);
        });
    }

    private void refresh() {
        ObservableList<MenuItem> items = templatesMenu.getItems();
        items.clear();

        if (templates.isEmpty()) {
            items.add(new MenuItem("无模板"));
            return;
        }

        for (int i = 0; i < templates.size(); i++) {
            int index = i;
            Template template = templates.get(index);
            Menu templateMenu = new Menu(template.getName());
            templateMenu.setOnAction((event) -> {
                CloneNodeUtil.setCloneNode(template.getMapNode().clone());
                templatesMenu.hide();
            });

            String preview = template.getPreview();
            File imageFile = new File(ConfigConstants.DIR_IMAGES + preview);
            // 转换为 file: 开头的合法 URI
            String imageUrl = imageFile.toURI().toString();
            Menu previewMenu = buildPreviewMenu(templateMenu, imageUrl, template.getRatio(), index + 1);

            MenuItem deleteItem = new MenuItem("删除");
            deleteItem.setOnAction((event -> {
                templates.remove(index);
                IOUtil.deleteImage(preview);
                CloneNodeUtil.setCloneNode(null);
            }));

            MenuItem renameItem = new MenuItem("重命名");
            renameItem.setOnAction((event -> {
                renameTemplate(template, index);
                CloneNodeUtil.setCloneNode(null);
            }));

            templateMenu.getItems().addAll(previewMenu, deleteItem, renameItem);
            items.add(templateMenu);
        }
    }

    private static Menu buildPreviewMenu(Menu templateMenu, String imageUrl, double scale, int i) {
        Menu previewMenu = new Menu("预览");
        previewMenu.setOnAction(event -> templateMenu.fire());

        ImageView imageView = new ImageView(imageUrl);
        imageView.setScaleX(scale);
        imageView.setScaleY(scale);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setCache(true);

        StackPane content = new StackPane(imageView);
        content.setMinSize(TemplateConstants.WIDTH, TemplateConstants.HEIGHT);
        content.setPrefSize(TemplateConstants.WIDTH, TemplateConstants.HEIGHT);
        content.setMaxSize(TemplateConstants.WIDTH, TemplateConstants.HEIGHT);

        CustomMenuItem customMenuItem = new CustomMenuItem(content);
        // 添加 item 后会装上一个监听器，
        // 在 setAccelerator 时，这个监听器会认为之前已经有一个快捷键了，它会把旧的快捷键的逻辑交给新快捷键，
        // 如果此前并没有快捷键，也就没有对应的逻辑，监听器会把 null 交给新快捷键，即按下快捷键后什么都不做
        // 如果是初始化时先添加 item 再 setAccelerator 不会有这个问题，此时没有 Scene，快捷键会在之后添加
        customMenuItem.setAccelerator(KeyCombination.keyCombination("Ctrl+" + i));
        customMenuItem.getStyleClass().clear();
        previewMenu.getItems().add(customMenuItem);
        return previewMenu;
    }

    public static void addTemplate(MapNode mapNode) {
        MapNode cloneNode = mapNode.clone();
        if (cloneNode.getPos() == PosConstants.LEFT) {
            cloneNode.setPos(PosConstants.RIGHT);
            cloneNode.setChildrenR(new ArrayList<>());
            for (MapNode child : cloneNode.getChildren(PosConstants.LEFT)) {
                child.transPosAt(-1, PosConstants.LEFT, PosConstants.RIGHT);
            }
        }

        Template template = snapshot(cloneNode);
        template.setMapNode(cloneNode);
        template.setName("模板" + (templates.size() + 1));
        templates.add(template);
    }

    /**
     * 生成快照
     */
    private static Template snapshot(MapNode mapNode) {
        SubjectController subjectController = new SubjectController();
        Subject subject = subjectController.getSubject();

        subject.addClone(mapNode);
        subjectController.setRootNode(mapNode);

        Pane pane = new Pane(subject);
        Scene scene = new Scene(pane);
        scene.getStylesheets().addAll(StyleSheetConstants.SNAPSHOT);

        byte pos = mapNode.getPos();
        subjectController.setOnAction(mapNode);
        subjectController.setOnActionChildren(mapNode, pos);
        subjectController.adjustChildrenXY(mapNode, pos);
        subjectController.refreshLines(pos);

        WritableImage image = subject.snapshot(buildParams(subject), null);
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
        double ratio = Math.min(TemplateConstants.WIDTH / image.getWidth(), TemplateConstants.HEIGHT / image.getHeight());
        ratio = Math.floor(ratio * 100) / 100;
        ratio = Math.min(ratio, 0.5);
        return new Template(IOUtil.saveImage(bufferedImage, null), ratio);
    }

    /**
     * 计算快照尺寸
     */
    private static SnapshotParameters buildParams(Subject subject) {
        // 内容的尺寸
        Bounds contentBounds = null;
        for (Node node : subject.getNodesLayer().getChildren()) {
            Bounds b = node.getBoundsInParent();
            if (contentBounds == null) {
                contentBounds = b;
            } else {
                double minX = Math.min(contentBounds.getMinX(), b.getMinX());
                double minY = Math.min(contentBounds.getMinY(), b.getMinY());
                double maxX = Math.max(contentBounds.getMaxX(), b.getMaxX());
                double maxY = Math.max(contentBounds.getMaxY(), b.getMaxY());
                contentBounds = new BoundingBox(minX, minY, maxX - minX, maxY - minY);
            }
        }

        // 留出边距，保留边框
        Rectangle2D viewport = new Rectangle2D(
                contentBounds.getMinX() - 2,
                contentBounds.getMinY() - 2,
                contentBounds.getWidth() + 4,
                contentBounds.getHeight() + 4
        );

        SnapshotParameters params = new SnapshotParameters();
        params.setViewport(viewport);
        return params;
    }

    private void renameTemplate(Template template, int index) {
        PrettyDialog dialog = new PrettyDialog("重命名");
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setPrefWidth(300);
        dialogPane.setPrefHeight(170);

        Label label = new Label("新名称：");
        Font font = new Font(15);
        label.setFont(font);
        TextField textField = new TextField(template.getName());
        textField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.length() > 10) {
                textField.setText(newVal.substring(0, 10));
                MessageUtil.showMessage("最长为10个字符");
            }
        });
        textField.setFont(font);
        Platform.runLater(textField::requestFocus);
        VBox vBox = new VBox(17, label, textField);
        dialogPane.setContent(vBox);

        dialog.showAndWait().ifPresent(input -> {
            if (input != ButtonType.OK) {
                return;
            }
            String newName = textField.getText().trim();
            if (newName.isEmpty() || newName.equals(template.getName())) return;

            boolean exists = templates.stream().anyMatch(t -> t.getName().equals(newName));
            if (exists) {
                MessageUtil.showMessage("名称已存在");
                return;
            }

            template.setName(newName);
            templates.set(index, template);
        });
    }

}
