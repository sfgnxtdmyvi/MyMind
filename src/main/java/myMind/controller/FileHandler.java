package myMind.controller;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import javafx.collections.ObservableList;
import javafx.scene.control.Tab;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import lombok.Getter;
import myMind.componet.MindMap;
import myMind.componet.MindNode;
import myMind.componet.Subject;
import myMind.constants.PosConstants;
import myMind.constants.SizeConstants;
import myMind.model.NodeModel;
import myMind.util.MessageUtil;
import org.fxmisc.richtext.StyleClassedTextArea;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class FileHandler {
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
    private static final Map<String, ScheduledFuture<?>> fileSaveFutures = new ConcurrentHashMap<>();

    private SubjectController subjectController;

    private final MindMap mindMap;
    @Getter
    private static final String dirImage;
    private static final String dirRecentFiles;

    static {
        ResourceBundle config = ResourceBundle.getBundle("config");
        dirImage = config.getString("directory.images");
        dirRecentFiles = config.getString("directory.recent_files");
    }

    @Getter
    private static final LinkedList<String> recentFiles;

    public FileHandler(MindMap mindMap) {
        this.mindMap = mindMap;
    }

    //—————————————————————————————————————————打开—————————————————————————————————————————
    private JSONObject readFile(File file) {
        if (mindMap.getFilePath() != null) {
            CancelSchedule(mindMap.getFilePath());
        }
        StringBuilder content = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line);
            }

            mindMap.getTabs().clear();
            Stage stage = (Stage) mindMap.getScene().getWindow();
            stage.setTitle(file.getName().substring(0, file.getName().length() - 3));
        } catch (Exception e) {
            MessageUtil.showMessage("读取失败：" + e.getMessage());
        }
        return JSONObject.parseObject(content.toString());
    }

    public void loadFile(File file) {
        JSONObject json = readFile(file);
        // 加载主题
        for (int i = 0; i < json.size(); i++) {
            NodeModel rootModel = new NodeModel(670, 311, PosConstants.MIDDLE);
            JSONObject subject = json.getJSONObject(Integer.toString(i));
            MindNode rootNode = buildNode(subject, rootModel);
            mindMap.addSubject(rootNode);
            subjectController = mindMap.getSubjectController();

            // 加载子节点
            loadChildR(subject.getJSONObject("childrenR"), rootModel);
            loadChildL(subject.getJSONObject("childrenL"), rootModel);

            subjectController.adjustChildrenSize();
            subjectController.adjustXY();
        }

        Subject firstSubject = (Subject) mindMap.getTabs().get(0).getContent();
        subjectController = firstSubject.getSubjectController();
        mindMap.setSubjectController(subjectController);
        mindMap.setSubject(firstSubject);
        MenuController.setSubjectController(subjectController);
        StyleWheelArcController.setSubjectController(subjectController);

        String absolutePath = file.getAbsolutePath();
        mindMap.setFilePath(absolutePath);
        scheduleAutoSave(absolutePath);
        addRecentFile(file);
    }

    private void loadChildR(JSONObject children, NodeModel parentModel) {
        if (children == null) {
            return;
        }

        for (int i = 0; i < children.size(); i++) {
            JSONObject json = children.getJSONObject(Integer.toString(i));

            NodeModel model = new NodeModel(PosConstants.RIGHT);
            parentModel.addChildR(model);
            MindNode node = buildNode(json, model);
            subjectController.addNode(node);

            loadChildR(json.getJSONObject("childrenR"), model);
        }
    }

    private void loadChildL(JSONObject children, NodeModel parentModel) {
        if (children == null) {
            return;
        }

        for (int i = 0; i < children.size(); i++) {
            JSONObject json = children.getJSONObject(Integer.toString(i));

            NodeModel model = new NodeModel(0, 0, PosConstants.LEFT);
            parentModel.addChildL(model);
            MindNode node = buildNode(json, model);
            subjectController.addNode(node);

            loadChildL(json.getJSONObject("childrenL"), model);
        }
    }

    private MindNode buildNode(JSONObject json, NodeModel model) {
        String imageName = json.getString("imageName");
        MindNode node;
        if (imageName != null) {
            node = new MindNode(model, imageName, json.getDouble("imageWidth"), json.getDouble("imageHeight"), buildTextArea(json));
        } else {
            node = new MindNode(model, buildTextArea(json));
        }
        return node;
    }

    private StyleClassedTextArea buildTextArea(JSONObject json) {
        StyleClassedTextArea textArea = new StyleClassedTextArea();
        textArea.getStyleClass().add("text-area");
        textArea.setWrapText(true);
        textArea.replaceText(json.getString("text"));

        JSONArray styles = json.getJSONArray("styles");
        if (styles != null) {
            for (int i = 0; i < styles.size(); i++) {
                JSONObject styleItem = styles.getJSONObject(i);
                JSONArray styleArray = styleItem.getJSONArray("style");
                List<String> styleList = new ArrayList<>();
                for (int j = 0; j < styleArray.size(); j++) {
                    styleList.add(styleArray.getString(j));
                }

                textArea.setStyle(styleItem.getIntValue("start"),
                        styleItem.getIntValue("end"),
                        styleList);
            }
        }

        return textArea;
    }

    //—————————————————————————————————————————保存—————————————————————————————————————————
    public void saveFile(File file) {
        JSONObject subjects = buildJson();

        try (FileWriter fw = new FileWriter(file)) {
            fw.write(subjects.toString());
            MessageUtil.showMessage("保存成功");
        } catch (IOException e) {
            MessageUtil.showMessage("保存失败：" + e.getMessage());
        }
    }

    public void saveFileScheduled(File file) {
        JSONObject subjects = buildJson();

        try (FileWriter fw = new FileWriter(file)) {
            fw.write(subjects.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private JSONObject buildJson() {
        ObservableList<Tab> tabs = mindMap.getTabs();
        JSONObject subjects = new JSONObject();
        for (int i = 0; i < tabs.size(); i++) {
            Tab tab = tabs.get(i);
            subjectController = ((Subject) tab.getContent()).getSubjectController();
            NodeModel rootModel = subjectController.getRootModel();

            JSONObject subject = saveNode(rootModel);
            saveChildrenR(subject, rootModel.getChildrenR());
            saveChildrenL(subject, rootModel.getChildrenL());

            subjects.put(Integer.toString(i), subject);
        }
        return subjects;
    }

    private JSONObject saveNode(NodeModel model) {
        JSONObject json = new JSONObject();

        MindNode mindNode = subjectController.getNode(model);
        // 文本
        StyleClassedTextArea textArea = mindNode.getTextArea();
        String text = textArea.getText();
        json.put("text", text);
        // 样式
        if (!text.isEmpty()) {
            JSONArray styles = extractStyles(textArea);
            if (!styles.isEmpty()) {
                json.put("styles", styles);
            }
        }

        // 图片
        String imageName = mindNode.getImageName();
        if (imageName != null) {
            json.put("imageName", imageName);
            ImageView image = mindNode.getImage();
            json.put("imageWidth", image.getFitWidth());
            json.put("imageHeight", image.getFitHeight());
        }

        return json;
    }

    private void saveChildrenR(JSONObject parentJson, List<NodeModel> childrenR) {
        if (!childrenR.isEmpty()) {
            JSONObject childrenRJson = new JSONObject();
            for (int i = 0; i < childrenR.size(); i++) {
                NodeModel childModel = childrenR.get(i);
                JSONObject childJson = saveNode(childModel);
                childrenRJson.put(Integer.toString(i), childJson);

                saveChildrenR(childJson, childModel.getChildrenR());
            }
            parentJson.put("childrenR", childrenRJson);
        }
    }

    private void saveChildrenL(JSONObject parentJson, List<NodeModel> childrenL) {
        if (!childrenL.isEmpty()) {
            JSONObject childrenLJson = new JSONObject();
            for (int i = 0; i < childrenL.size(); i++) {
                NodeModel childModel = childrenL.get(i);
                JSONObject childJson = saveNode(childModel);
                childrenLJson.put(Integer.toString(i), childJson);

                saveChildrenL(childJson, childModel.getChildrenL());
            }
            parentJson.put("childrenL", childrenLJson);
        }
    }

    public JSONArray extractStyles(StyleClassedTextArea textArea) {
        JSONArray styles = new JSONArray();
        int start = 0;
        Collection<String> lastStyles = textArea.getStyleOfChar(0);
        int length = textArea.getLength();

        for (int i = 1; i < length; i++) {
            Collection<String> currentStyles = textArea.getStyleOfChar(i);

            // 样式变化时保存前一段
            if (!currentStyles.equals(lastStyles)) {
                if (!lastStyles.isEmpty()) {
                    JSONObject styleItem = new JSONObject();
                    styleItem.put("start", start);
                    styleItem.put("end", i);
                    styleItem.put("style", lastStyles);

                    styles.add(styleItem);
                }

                lastStyles = currentStyles;
                start = i;
            }
        }

        // 由于最后一段不会变化，额外保存
        if (!lastStyles.isEmpty()) {
            JSONObject styleItem = new JSONObject();
            styleItem.put("start", start);
            styleItem.put("end", length);
            styleItem.put("style", lastStyles);

            styles.add(styleItem);
        }

        return styles;
    }

    //—————————————————————————————————————————定时保存—————————————————————————————————————————

    public void scheduleAutoSave(String filePath) {
        if (fileSaveFutures.containsKey(filePath)) {
            return;
        }

        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() ->
                saveFileScheduled(new File(filePath)), 1, 1, TimeUnit.SECONDS);
        fileSaveFutures.put(filePath, future);
    }

    public void CancelSchedule(String filePath) {
        ScheduledFuture<?> future = fileSaveFutures.remove(filePath);
        if (future != null) {
            future.cancel(false);
        }
    }

    // ScheduledExecutorService 创建的是非守护线程，会阻止 JVM 自然退出，需要关闭
    public void CancelSchedule() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }

    //—————————————————————————————————————————导入—————————————————————————————————————————

    public void importFile(File file) {
        JSONObject json = readFile(file);

        importSubjet(json);

        JSONObject subjects = json.getJSONObject("subjects");
        if (subjects != null) {
            for (int i = 0; i < subjects.size() - 1; i++) {
                importSubjet(subjects.getJSONObject(Integer.toString(i)));
            }
        }
        mindMap.getSelectionModel().select(0);
    }

    private void importSubjet(JSONObject json) {
        mindMap.addSubject();
        subjectController = mindMap.getSubjectController();

        JSONObject rootJson = json.getJSONObject("root");
        NodeModel rootModel = subjectController.getRootModel();
        MindNode rootNode = subjectController.getRootNode();
        importNode(rootJson, rootNode);

        importChildR(rootJson.getJSONObject("children"), rootModel);
        importChildL(rootJson.getJSONObject("children2"), rootModel);

        subjectController.adjustChildrenSize();
        subjectController.adjustXY();
    }

    private void importNode(JSONObject json, MindNode node) {
        // 文本样式
        node.getTextArea().replaceText(json.getString("text"));
        JSONArray style = json.getJSONArray("style");
        if (style != null) {
            StyleClassedTextArea textArea = node.getTextArea();
            for (int i = 0; i < style.size(); i++) {
                JSONObject styleItem = style.getJSONObject(i);
                Boolean bold = styleItem.getBoolean("bold");
                List<String> styleList = new ArrayList<>();
                String color = styleItem.getString("color");
                if (bold != null) {
                    styleList.add("bold-text");
                } else if (color != null) {
                    if (color.equals("#FF0000")) {
                        styleList.add("red-text");
                    } else if (color.equals("#FF8C00")) {
                        styleList.add("orange-text");
                    }
                }

                textArea.setStyle(styleItem.getIntValue("start"),
                        styleItem.getIntValue("end"),
                        styleList);
            }
        }

        // 图片
        String imageName = json.getString("imageName");
        if (imageName != null) {
            JSONObject imageSize = json.getJSONObject("imageResize");
            if (imageSize == null) {
                imageSize = json.getJSONObject("imageSize");
            }
            node.importImage(imageName, imageSize.getDouble("width"), imageSize.getDouble("height"));
        }
    }

    private void importChildR(JSONObject children, NodeModel parentModel) {
        if (children == null) {
            return;
        }

        //children里有一个"objectClass": "NSArray"
        for (int i = 0; i < children.size() - 1; i++) {
            JSONObject jsonNode = children.getJSONObject(Integer.toString(i));

            NodeModel model = new NodeModel(PosConstants.RIGHT);
            parentModel.addChildR(model);
            MindNode node = new MindNode(model);
            importNode(jsonNode, node);
            subjectController.addNode(node);

            importChildR(jsonNode.getJSONObject("children"), model);
        }
    }

    private void importChildL(JSONObject children, NodeModel parentModel) {
        if (children == null) {
            return;
        }

        for (int i = 0; i < children.size() - 1; i++) {
            JSONObject jsonNode = children.getJSONObject(Integer.toString(i));

            NodeModel model = new NodeModel(0, 0, PosConstants.LEFT);
            parentModel.addChildL(model);
            MindNode node = new MindNode(model);
            importNode(jsonNode, node);
            subjectController.addNode(node);

            importChildL(jsonNode.getJSONObject("children"), model);
        }
    }

    //—————————————————————————————————————————最近打开—————————————————————————————————————————
    static {
        recentFiles = new LinkedList<>();
        File file = new File(dirRecentFiles);

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                recentFiles.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addRecentFile(File file) {
        // 导图可能在多级目录下，不能通过根目录名 + file.getName()读取
        String string = file.getName().substring(0, file.getName().length() - 3) + "=" + file.getAbsolutePath();
        recentFiles.remove(string);
        recentFiles.addFirst(string);
        if (recentFiles.size() > SizeConstants.MAX_RECENT_FILES) {
            recentFiles.removeLast();
        }

        saveRecentFiles();
    }

    public void saveRecentFiles() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(dirRecentFiles))) {
            for (String fileName : recentFiles) {
                bw.write(fileName);
                bw.newLine();
            }
        } catch (IOException e) {
            MessageUtil.showMessage("保存失败：" + e.getMessage());
        }
    }

    //—————————————————————————————————————————图片—————————————————————————————————————————
    public static String saveImage(BufferedImage bufferedImage, String imageName) {
        if (imageName == null) {
            imageName = System.currentTimeMillis() + ".png";
        }
        String imagePath = dirImage + imageName;

        File output = new File(imagePath);
        try {
            ImageIO.write(bufferedImage, "png", output);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return imageName;
    }

    public static void deleteImage(String imageName) {
        File file = new File(dirImage + imageName);
        if (file.exists()) {
            file.delete();
        }
    }

}
