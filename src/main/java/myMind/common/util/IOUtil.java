package myMind.common.util;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.Getter;
import myMind.common.constants.ConfigConstants;
import myMind.common.constants.IOConstants;
import myMind.common.constants.PosConstants;
import myMind.common.manager.ReferenceManager;
import myMind.componet.MapNode;
import myMind.componet.MindMap;
import myMind.componet.styledArea.StyleClassedTextArea;
import myMind.controller.SubjectController;
import org.fxmisc.richtext.model.ReadOnlyStyledDocument;
import org.fxmisc.richtext.model.SegmentOps;
import org.fxmisc.richtext.model.SimpleEditableStyledDocument;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class IOUtil {

    @Getter
    private static final LinkedList<String> recentFiles;

    public static File openFileChooser(int type, MindMap mindMap) {
        FileChooser fc = new FileChooser();
        fc.setInitialDirectory(new File(ConfigConstants.DIR_FILES));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("MyMind Files", "*.mm"));
        if (type == IOConstants.OPEN_TYPE) {
            return fc.showOpenDialog(mindMap.getScene().getWindow());
        } else {
            return fc.showSaveDialog(mindMap.getScene().getWindow());
        }
    }

    //—————————————————————————————————————————打开—————————————————————————————————————————

    public static JSONObject readJson(File file) {
        StringBuilder content = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line);
            }
        } catch (Exception e) {
            System.out.println("读取失败：" + e.getMessage());
        }
        return JSONObject.parseObject(content.toString());
    }

    public static void load(File file, MindMap mindMap) {
        JSONObject json = readJson(file);
        Stage stage = (Stage) mindMap.getScene().getWindow();
        stage.setTitle(file.getName().substring(0, file.getName().length() - 3));
        ReferenceManager.prepare();

        // 加载主题
        for (String key : json.keySet()) {
            JSONObject subject = (JSONObject) json.get(key);
            MapNode rootNode = buildNode(subject, PosConstants.MIDDLE);
            mindMap.addSubject(rootNode, Long.parseLong(key));
            SubjectController subjectController = mindMap.getSubjectController();

            // 加载子节点
            loadChild(subject.getJSONArray(IOConstants.CHILDREN_R), rootNode, subjectController, PosConstants.RIGHT);
            loadChild(subject.getJSONArray(IOConstants.CHILDREN_L), rootNode, subjectController, PosConstants.LEFT);

            // 等得到真正的高度
            ObservableList<Node> children = subjectController.getSubject().getNodesLayer().getChildren();
            MapNode lastNode = (MapNode) children.get(children.size() - 1);
            subjectController.adjustAsync(lastNode, () -> {
                subjectController.adjustXY();
                children.forEach((node -> ((MapNode) node).addSizeListener()));
            });
        }

        ReferenceManager.link();
        String absolutePath = file.getAbsolutePath();
        mindMap.setFilePath(absolutePath);
        addRecentFile(file);
    }

    private static void loadChild(JSONArray childrenJsonArray, MapNode parentNode, SubjectController subjectController, byte pos) {
        if (childrenJsonArray == null) {
            return;
        }

        for (Object o : childrenJsonArray) {
            JSONObject childrenJson = (JSONObject) o;
            MapNode node = buildNode(childrenJson, pos);
            parentNode.addChild(node, pos);
            subjectController.addNode(node, false);

            loadChild(childrenJson.getJSONArray(IOConstants.CHILDREN), node, subjectController, pos);
        }
    }

    public static void loadChild(JSONArray childrenJsonArray, MapNode parentNode, byte pos) {
        if (childrenJsonArray == null) {
            return;
        }

        for (Object o : childrenJsonArray) {
            JSONObject childrenJson = (JSONObject) o;
            MapNode node = buildNode(childrenJson, pos);
            parentNode.addChild(node, pos);

            loadChild(childrenJson.getJSONArray(IOConstants.CHILDREN), node, pos);
        }
    }

    public static MapNode buildNode(JSONObject json, byte pos) {
        MapNode node = new MapNode(pos, json.getLong(IOConstants.ID), buildTextArea(json));

        String imageName = json.getString(IOConstants.IMAGE_NAME);
        if (imageName != null) {
            node.setImage(imageName, json.getDouble(IOConstants.IMAGE_WIDTH), json.getDouble(IOConstants.IMAGE_HEIGHT));
        }

        if (json.getLong(IOConstants.OUTGOING_REFERENCE) != null) {
            ReferenceManager.addIncomingReference(node, json.getLong(IOConstants.OUTGOING_REFERENCE));
        }
        if (json.getLong(IOConstants.SUBJECT_ID) != null) {
            node.setSubjectId(json.getLong(IOConstants.SUBJECT_ID));
            ReferenceManager.addOutgoingReference(node);
        }

        return node;
    }

    private static StyleClassedTextArea buildTextArea(JSONObject json) {
        // 带文本的只读文档
        ReadOnlyStyledDocument<Collection<String>, String, Collection<String>> initialDoc =
                ReadOnlyStyledDocument.fromString(
                        json.getString(IOConstants.TEXT),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        SegmentOps.styledTextOps((s1, s2) -> Optional.empty())
                );

        SimpleEditableStyledDocument<Collection<String>, Collection<String>> doc =
                new SimpleEditableStyledDocument<>(initialDoc);

        // 设置样式
        JSONArray styles = json.getJSONArray(IOConstants.STYLES);
        if (styles != null) {
            for (int i = 0; i < styles.size(); i++) {
                JSONObject styleItem = styles.getJSONObject(i);
                JSONArray styleArray = styleItem.getJSONArray(IOConstants.STYLE);
                List<String> styleList = new ArrayList<>();
                for (int j = 0; j < styleArray.size(); j++) {
                    styleList.add(styleArray.getString(j));
                }
                doc.setStyle(styleItem.getIntValue(IOConstants.START),
                        styleItem.getIntValue(IOConstants.END),
                        styleList);
            }
        }

        StyleClassedTextArea textArea = new StyleClassedTextArea(doc, true);
        // 解决奇怪 bug，如果没有手动换行，只有前两行能看到，其他是空白的，点击后，才能正常显示
        textArea.layout();
        return textArea;
    }

    //—————————————————————————————————————————保存—————————————————————————————————————————

    public static void saveJson(File file, JSONObject jsonObject, boolean showMessage) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write(jsonObject.toString());
            if (showMessage) {
                MessageUtil.showMessage("保存成功");
            }
        } catch (IOException e) {
            MessageUtil.showMessage("保存失败：" + e.getMessage());
        }
    }

    public static void save(MindMap mindMap) {
        if (mindMap.getFilePath() == null) {
            saveAs(mindMap);
        } else {
            saveFile(new File(mindMap.getFilePath()), mindMap, true);
        }
    }

    /**
     * 保存到新文件
     */
    public static void saveAs(MindMap mindMap) {
        File file = openFileChooser(IOConstants.SAVE_TYPE, mindMap);
        // 取消时，file 为 null
        if (file != null) {
            saveFile(file, mindMap, true);

            String absolutePath = file.getAbsolutePath();
            addRecentFile(file);
            mindMap.setFilePath(absolutePath);
            Stage stage = (Stage) mindMap.getScene().getWindow();
            stage.setTitle(file.getName().substring(0, file.getName().length() - 3));
        }
    }

    public static void saveFile(File file, MindMap mindMap, boolean showMessage) {
        saveJson(file, saveSubjects(mindMap), showMessage);
    }

    private static JSONObject saveSubjects(MindMap mindMap) {
        ObservableList<Tab> tabs = mindMap.getTabs();
        JSONObject subjects = new JSONObject();
        for (Tab tab : tabs) {
            SubjectController subjectController = (SubjectController) tab.getUserData();
            MapNode rootNode = subjectController.getRootNode();

            JSONObject subjectJson = saveNode(rootNode);
            saveChildren(subjectJson, rootNode.getChildrenR(), IOConstants.CHILDREN_R, PosConstants.RIGHT);
            saveChildren(subjectJson, rootNode.getChildrenL(), IOConstants.CHILDREN_L, PosConstants.LEFT);

            subjects.put(String.valueOf(subjectController.getSubject().getSubjectId()), subjectJson);
        }
        return subjects;
    }

    public static void saveChildren(JSONObject parentJson, List<MapNode> children, String key, byte pos) {
        if (children == null || children.isEmpty()) {
            return;
        }
        JSONArray childrenJson = parentJson.putArray(key);
        for (MapNode node : children) {
            JSONObject childJson = saveNode(node);
            childrenJson.add(childJson);

            saveChildren(childJson, node.getChildren(pos), IOConstants.CHILDREN, pos);
        }
    }

    public static JSONObject saveNode(MapNode node) {
        JSONObject json = new JSONObject();
        json.put(IOConstants.ID, node.getNodeId());
        // 文本
        StyleClassedTextArea textArea = node.getTextArea();
        String text = textArea.getText();
        json.put(IOConstants.TEXT, text);
        // 样式
        if (!text.isEmpty()) {
            JSONArray styles = saveStyles(textArea);
            if (!styles.isEmpty()) {
                json.put(IOConstants.STYLES, styles);
            }
        }

        // 图片
        if (node.getImageName() != null) {
            json.put(IOConstants.IMAGE_NAME, node.getImageName());
            ImageView image = node.getImageView();
            json.put(IOConstants.IMAGE_WIDTH, image.getFitWidth());
            json.put(IOConstants.IMAGE_HEIGHT, image.getFitHeight());
        }

        // 引用
        if (node.getOutgoingReference() != null) {
            json.put(IOConstants.OUTGOING_REFERENCE, node.getOutgoingReference().getNodeId());
        }
        if (node.getSubjectId() != 0) {
            json.put(IOConstants.SUBJECT_ID, node.getSubjectId());
        }

        return json;
    }

    public static JSONArray saveStyles(StyleClassedTextArea textArea) {
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
                    styleItem.put(IOConstants.START, start);
                    styleItem.put(IOConstants.END, i);
                    styleItem.put(IOConstants.STYLE, lastStyles);

                    styles.add(styleItem);
                }

                lastStyles = currentStyles;
                start = i;
            }
        }

        // 由于最后一段不会变化，额外保存
        if (!lastStyles.isEmpty()) {
            JSONObject styleItem = new JSONObject();
            styleItem.put(IOConstants.START, start);
            styleItem.put(IOConstants.END, length);
            styleItem.put(IOConstants.STYLE, lastStyles);

            styles.add(styleItem);
        }

        return styles;
    }

    //—————————————————————————————————————————最近打开—————————————————————————————————————————

    static {
        recentFiles = new LinkedList<>();
        File file = new File(ConfigConstants.FILE_RECENT_FILES);
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                recentFiles.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void addRecentFile(File file) {
        // 导图可能在多级目录下，不能通过根目录名 + file.getName()读取
        String string = file.getName().substring(0, file.getName().length() - 3) + "=" + file.getAbsolutePath();
        recentFiles.remove(string);
        recentFiles.addFirst(string);
        if (recentFiles.size() > IOConstants.MAX_RECENT_FILES) {
            recentFiles.removeLast();
        }

        saveRecentFiles();
    }

    public static void saveRecentFiles() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(ConfigConstants.FILE_RECENT_FILES))) {
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
        String imagePath = ConfigConstants.DIR_IMAGES + imageName;

        File output = new File(imagePath);
        try {
            ImageIO.write(bufferedImage, "png", output);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return imageName;
    }

    public static void deleteImage(String imageName) {
        File file = new File(ConfigConstants.DIR_IMAGES + imageName);
        if (file.exists()) {
            file.delete();
        }
    }

    //—————————————————————————————————————————批量处理—————————————————————————————————————————

    /**
     * 删除没有任何导图被使用的图片
     */
    public static void deleteUnusefulImage() {
        // 所有导图的图片
        Set<String> fileNameSet = new HashSet<>();
        addFileImage(new File(ConfigConstants.DIR_FILES), fileNameSet);

        int count = 0;
        File dirImage = new File(ConfigConstants.DIR_IMAGES);
        for (File file : dirImage.listFiles()) {
            if (!fileNameSet.contains(file.getName())) {
                try {
                    Files.move(file.toPath(), new File(ConfigConstants.DIR_DELETE_IMAGES + file.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    MessageUtil.showMessage(e.getMessage());
                }
                count++;
            }
        }

        if (count > 0) {
            MessageUtil.showMessage("删除了 " + count + " 张图片");
        } else {
            MessageUtil.showMessage("没有多余的图片");
        }
    }

    private static void addFileImage(File file, Set<String> fileNameSet) {
        for (File f : file.listFiles()) {
            if (f.isDirectory()) {
                addFileImage(f, fileNameSet);
            } else {
                // 把一个导图的所有图片添加到 fileNameSet 中
                JSONObject json = readJson(f);
                for (Object value : json.values()) {
                    JSONObject rootNode = (JSONObject) value;
                    addImage(fileNameSet, rootNode);
                    addImage(rootNode.getJSONArray(IOConstants.CHILDREN_R), fileNameSet);
                    addImage(rootNode.getJSONArray(IOConstants.CHILDREN_L), fileNameSet);
                }
            }
        }
    }

    private static void addImage(JSONArray children, Set<String> fileNameSet) {
        if (children == null) {
            return;
        }
        for (Object value : children) {
            JSONObject node = (JSONObject) value;
            addImage(fileNameSet, node);
            addImage(node.getJSONArray(IOConstants.CHILDREN), fileNameSet);
        }
    }

    private static void addImage(Set<String> fileNameSet, JSONObject node) {
        String imageName = node.getString(IOConstants.IMAGE_NAME);
        if (imageName != null) {
            fileNameSet.add(imageName);
        }
    }

    public static Set<Long> ids;

    /**
     * 更新导图文件结构
     */
    public static void updateMap() {
        ids = new HashSet<>();
        File dir = new File(ConfigConstants.DIR_FILES);
        updateFile(dir);
        MessageUtil.showMessage("更新完成");
    }

    private static void updateFile(File dir) {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                updateFile(file);
            } else {
                JSONObject mindMap = readJson(file);

                for (String key : mindMap.keySet()) {
                    Long id = Long.valueOf(key);
                    if (ids.contains(id)) {
                        System.out.print(id + "、");
                    } else {
                        ids.add(Long.valueOf(key));
                    }
                    JSONObject subject = (JSONObject) mindMap.get(key);
                    updateFile(subject, IOConstants.CHILDREN_R);
                    updateFile(subject, IOConstants.CHILDREN_L);
                }

//                try (FileWriter fw = new FileWriter(file)) {
//                    fw.write(mindMap.toString());
//                } catch (IOException e) {
//                    MessageUtil.showMessage("更新失败：" + e.getMessage());
//                }
            }
        }
    }

    private static void updateFile(JSONObject parentJson, String key) {
        JSONArray children = parentJson.getJSONArray(key);
        if (children == null) {
            return;
        }

        for (Object value : children) {
            JSONObject json = (JSONObject) value;
            Long id = json.getLong(IOConstants.ID);
            if (ids.contains(id)) {
                System.out.print(id + "、");
            } else {
                ids.add(id);
            }
            updateFile(json, IOConstants.CHILDREN);
        }
    }

}
