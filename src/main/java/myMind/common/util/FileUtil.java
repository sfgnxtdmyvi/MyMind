package myMind.common.util;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import javafx.collections.ObservableList;
import javafx.scene.control.Tab;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.Getter;
import myMind.common.constants.ConfigConstants;
import myMind.common.constants.FileConstants;
import myMind.common.constants.NodeConstants;
import myMind.common.constants.PosConstants;
import myMind.common.constants.SizeConstants;
import myMind.common.manager.ReferenceManager;
import myMind.componet.MapNode;
import myMind.componet.MapTextArea;
import myMind.componet.MindMap;
import myMind.controller.SubjectController;
import org.fxmisc.richtext.StyleClassedTextArea;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class FileUtil {
    @Getter
    private static final LinkedList<String> recentFiles;

    public static File openFileChooser(int type, MindMap mindMap) {
        FileChooser fc = new FileChooser();
        fc.setInitialDirectory(new File(ConfigConstants.DIR_FILES));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("MyMind Files", "*.mm"));
        if (type == FileConstants.OPEN_TYPE) {
            return fc.showOpenDialog(mindMap.getScene().getWindow());
        } else {
            return fc.showSaveDialog(mindMap.getScene().getWindow());
        }
    }

    //—————————————————————————————————————————打开—————————————————————————————————————————
    private static JSONObject readFile(File file) {
        StringBuilder content = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line);
            }
        } catch (Exception e) {
            MessageUtil.showMessage("读取失败：" + e.getMessage());
        }
        return JSONObject.parseObject(content.toString());
    }

    public static void load(File file, MindMap mindMap) {
        JSONObject json = readFile(file);
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
            loadChildR(subject.getJSONObject(FileConstants.CHILDREN_R), rootNode, subjectController);
            loadChildL(subject.getJSONObject(FileConstants.CHILDREN_L), rootNode, subjectController);

            subjectController.adjustChildrenSize();
            subjectController.adjustXY();
        }

        ReferenceManager.link();
        String absolutePath = file.getAbsolutePath();
        mindMap.setFilePath(absolutePath);
        ScheduleUtil.scheduleAutoSave(absolutePath, mindMap);
        addRecentFile(file);
    }

    private static void loadChildR(JSONObject json, MapNode parentNode, SubjectController subjectController) {
        if (json == null) {
            return;
        }

        for (int i = 0; i < json.size(); i++) {
            JSONObject childrenJson = json.getJSONObject(Integer.toString(i));

            MapNode node = buildNode(childrenJson, PosConstants.RIGHT);
            parentNode.addChildR(node);
            subjectController.addNode(node);

            loadChildR(childrenJson.getJSONObject(FileConstants.CHILDREN_R), node, subjectController);
        }
    }

    private static void loadChildL(JSONObject json, MapNode parentNode, SubjectController subjectController) {
        if (json == null) {
            return;
        }

        for (int i = 0; i < json.size(); i++) {
            JSONObject childrenJson = json.getJSONObject(Integer.toString(i));

            MapNode node = buildNode(childrenJson, PosConstants.LEFT);
            parentNode.addChildL(node);
            subjectController.addNode(node);

            loadChildL(childrenJson.getJSONObject(FileConstants.CHILDREN_L), node, subjectController);
        }
    }

    private static MapNode buildNode(JSONObject json, byte pos) {
        String imageName = json.getString(FileConstants.IMAGE_NAME);
        MapNode node = new MapNode(pos, json.getLong(FileConstants.ID), buildTextArea(json));

        if (imageName != null) {
            node.setImage(imageName, json.getDouble(FileConstants.IMAGE_WIDTH), json.getDouble(FileConstants.IMAGE_HEIGHT));
        }

        if (json.getLong(FileConstants.OUTGOING_REFERENCE) != null) {
            ReferenceManager.addIncomingReference(node, json.getLong(FileConstants.OUTGOING_REFERENCE));
        }
        if (json.getLong(FileConstants.SUBJECT_ID) != null) {
            node.setSubjectId(json.getLong(FileConstants.SUBJECT_ID));
            ReferenceManager.addOutgoingReference(node);
        }

        return node;
    }

    private static StyleClassedTextArea buildTextArea(JSONObject json) {
        // 带文本的只读文档
        ReadOnlyStyledDocument<Collection<String>, String, Collection<String>> initialDoc =
                ReadOnlyStyledDocument.fromString(
                        json.getString(FileConstants.TEXT),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        SegmentOps.styledTextOps((s1, s2) -> Optional.empty())
                );

        SimpleEditableStyledDocument<Collection<String>, Collection<String>> doc =
                new SimpleEditableStyledDocument<>(initialDoc);

        // 设置样式
        JSONArray styles = json.getJSONArray(FileConstants.STYLES);
        if (styles != null) {
            for (int i = 0; i < styles.size(); i++) {
                JSONObject styleItem = styles.getJSONObject(i);
                JSONArray styleArray = styleItem.getJSONArray(FileConstants.STYLE);
                List<String> styleList = new ArrayList<>();
                for (int j = 0; j < styleArray.size(); j++) {
                    styleList.add(styleArray.getString(j));
                }
                doc.setStyle(styleItem.getIntValue(FileConstants.START),
                        styleItem.getIntValue(FileConstants.END),
                        styleList);
            }
        }

        StyleClassedTextArea textArea = new MapTextArea(doc, true);
        // 解决奇怪 bug，如果没有手动换行，只有前两行能看到，其他是空白的，点击后，才能正常显示
        textArea.setMaxWidth(NodeConstants.MAX_TEXTAREA_WIDTH);
        textArea.layout();
        return textArea;
    }

    //—————————————————————————————————————————保存—————————————————————————————————————————

    public static void save(MindMap mindMap) {
        if (mindMap.getFilePath() == null) {
            saveAs(mindMap);
        } else {
            saveFile(new File(mindMap.getFilePath()), mindMap);
        }
    }

    /**
     * 保存到新文件
     */
    public static void saveAs(MindMap mindMap) {
        File file = openFileChooser(FileConstants.SAVE_TYPE, mindMap);
        // 取消时，file 为 null
        if (file != null) {
            saveFile(file, mindMap);

            if (mindMap.getFilePath() != null) {
                ScheduleUtil.cancelSchedule(mindMap.getFilePath());
            }
            String absolutePath = file.getAbsolutePath();
            ScheduleUtil.scheduleAutoSave(absolutePath, mindMap);

            addRecentFile(file);
            mindMap.setFilePath(absolutePath);
            Stage stage = (Stage) mindMap.getScene().getWindow();
            stage.setTitle(file.getName().substring(0, file.getName().length() - 3));
        }
    }

    public static void saveFile(File file, MindMap mindMap) {
        JSONObject subjects = saveSubjects(mindMap);

        try (FileWriter fw = new FileWriter(file)) {
            fw.write(subjects.toString());
            MessageUtil.showMessage("保存成功");
        } catch (IOException e) {
            MessageUtil.showMessage("保存失败：" + e.getMessage());
        }
    }

    public static void saveFileSilence(File file, MindMap mindMap) {
        JSONObject subjects = saveSubjects(mindMap);

        try (FileWriter fw = new FileWriter(file)) {
            fw.write(subjects.toString());
        } catch (IOException e) {
            MessageUtil.showMessage("自动保存失败：" + e.getMessage());
        }
    }

    private static JSONObject saveSubjects(MindMap mindMap) {
        ObservableList<Tab> tabs = mindMap.getTabs();
        JSONObject subjects = new JSONObject();
        for (int i = 0; i < tabs.size(); i++) {
            SubjectController subjectController = (SubjectController) tabs.get(i).getUserData();
            MapNode rootNode = subjectController.getRootNode();

            JSONObject subjectJson = saveNode(rootNode);
            saveChildrenR(subjectJson, rootNode.getChildrenR());
            saveChildrenL(subjectJson, rootNode.getChildrenL());

            subjects.put(String.valueOf(subjectController.getSubject().getSubjectId()), subjectJson);
        }
        return subjects;
    }

    private static void saveChildrenR(JSONObject parentJson, List<MapNode> childrenR) {
        if (!childrenR.isEmpty()) {
            JSONObject childrenRJson = new JSONObject();
            for (int i = 0; i < childrenR.size(); i++) {
                MapNode node = childrenR.get(i);
                JSONObject childJson = saveNode(node);
                childrenRJson.put(Integer.toString(i), childJson);

                saveChildrenR(childJson, node.getChildrenR());
            }
            parentJson.put(FileConstants.CHILDREN_R, childrenRJson);
        }
    }

    private static void saveChildrenL(JSONObject parentJson, List<MapNode> childrenL) {
        if (!childrenL.isEmpty()) {
            JSONObject childrenLJson = new JSONObject();
            for (int i = 0; i < childrenL.size(); i++) {
                MapNode node = childrenL.get(i);
                JSONObject childJson = saveNode(node);
                childrenLJson.put(Integer.toString(i), childJson);

                saveChildrenL(childJson, node.getChildrenL());
            }
            parentJson.put(FileConstants.CHILDREN_L, childrenLJson);
        }
    }

    private static JSONObject saveNode(MapNode node) {
        JSONObject json = new JSONObject();
        json.put(FileConstants.ID, node.getNodeId());
        // 文本
        StyleClassedTextArea textArea = node.getTextArea();
        String text = textArea.getText();
        json.put(FileConstants.TEXT, text);
        // 样式
        if (!text.isEmpty()) {
            JSONArray styles = saveStyles(textArea);
            if (!styles.isEmpty()) {
                json.put(FileConstants.STYLES, styles);
            }
        }

        // 图片
        if (node.getImageName() != null) {
            json.put(FileConstants.IMAGE_NAME, node.getImageName());
            ImageView image = node.getImageView();
            json.put(FileConstants.IMAGE_WIDTH, image.getFitWidth());
            json.put(FileConstants.IMAGE_HEIGHT, image.getFitHeight());
        }

        // 引用
        if (node.getOutgoingReference() != null) {
            json.put(FileConstants.OUTGOING_REFERENCE, node.getOutgoingReference().getNodeId());
        }
        if (node.getSubjectId() != 0) {
            json.put(FileConstants.SUBJECT_ID, node.getSubjectId());
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
                    styleItem.put(FileConstants.START, start);
                    styleItem.put(FileConstants.END, i);
                    styleItem.put(FileConstants.STYLE, lastStyles);

                    styles.add(styleItem);
                }

                lastStyles = currentStyles;
                start = i;
            }
        }

        // 由于最后一段不会变化，额外保存
        if (!lastStyles.isEmpty()) {
            JSONObject styleItem = new JSONObject();
            styleItem.put(FileConstants.START, start);
            styleItem.put(FileConstants.END, length);
            styleItem.put(FileConstants.STYLE, lastStyles);

            styles.add(styleItem);
        }

        return styles;
    }

    //—————————————————————————————————————————最近打开—————————————————————————————————————————
    static {
        recentFiles = new LinkedList<>();
        File file = new File(ConfigConstants.DIR_RECENT_FILES);
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
        if (recentFiles.size() > SizeConstants.MAX_RECENT_FILES) {
            recentFiles.removeLast();
        }

        saveRecentFiles();
    }

    public static void saveRecentFiles() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(ConfigConstants.DIR_RECENT_FILES))) {
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
        String imagePath = ConfigConstants.DIR_IMAGE + imageName;

        File output = new File(imagePath);
        try {
            ImageIO.write(bufferedImage, "png", output);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return imageName;
    }

    public static void deleteImage(String imageName) {
        File file = new File(ConfigConstants.DIR_IMAGE + imageName);
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
        File dirImage = new File(ConfigConstants.DIR_IMAGE);
        for (File file : dirImage.listFiles()) {
            if (!fileNameSet.contains(file.getName())) {
                file.delete();
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
                JSONObject json = readFile(f);
                for (Object value : json.values()) {
                    JSONObject rootNode = (JSONObject) value;
                    addImage(fileNameSet, rootNode);
                    addImageR(rootNode.getJSONObject(FileConstants.CHILDREN_R), fileNameSet);
                    addImageL(rootNode.getJSONObject(FileConstants.CHILDREN_L), fileNameSet);
                }
            }
        }
    }

    private static void addImageR(JSONObject childrenR, Set<String> fileNameSet) {
        if (childrenR == null) {
            return;
        }
        for (Object value : childrenR.values()) {
            JSONObject node = (JSONObject) value;
            addImage(fileNameSet, node);
            addImageR(node.getJSONObject(FileConstants.CHILDREN_R), fileNameSet);
        }
    }

    private static void addImageL(JSONObject childrenL, Set<String> fileNameSet) {
        if (childrenL == null) {
            return;
        }
        for (Object value : childrenL.values()) {
            JSONObject node = (JSONObject) value;
            addImage(fileNameSet, node);
            addImageL(node.getJSONObject(FileConstants.CHILDREN_L), fileNameSet);
        }
    }

    private static void addImage(Set<String> fileNameSet, JSONObject node) {
        String imageName = node.getString(FileConstants.IMAGE_NAME);
        if (imageName != null) {
            fileNameSet.add(imageName);
        }
    }

    /**
     * 删除每个节点末尾的句号
     */
    public static void deletePeriod() {
        deletePeriodFile(new File(ConfigConstants.DIR_FILES));
        MessageUtil.showMessage("处理完毕");
    }

    private static void deletePeriodFile(File parentFile) {
        for (File file : parentFile.listFiles()) {
            if (file.isDirectory()) {
                deletePeriodFile(file);
            } else {
                JSONObject json = readFile(file);
                for (Object value : json.values()) {
                    JSONObject subject = (JSONObject) value;
                    String text = subject.getString(FileConstants.TEXT);
                    subject.put(FileConstants.TEXT, FormatUtil.deletePeriod(text));
                    deletePeriodNodeR(subject.getJSONObject(FileConstants.CHILDREN_R));
                    deletePeriodNodeL(subject.getJSONObject(FileConstants.CHILDREN_L));
                }
                try (FileWriter fw = new FileWriter(file)) {
                    fw.write(json.toString());
                } catch (IOException e) {
                    MessageUtil.showMessage("处理失败：" + e.getMessage());
                }
            }
        }
    }

    private static void deletePeriodNodeR(JSONObject json) {
        if (json == null) {
            return;
        }
        for (int i = 0; i < json.size(); i++) {
            JSONObject childJson = json.getJSONObject(Integer.toString(i));
            String text = childJson.getString(FileConstants.TEXT);
            childJson.put(FileConstants.TEXT, FormatUtil.deletePeriod(text));
            deletePeriodNodeR(childJson.getJSONObject(FileConstants.CHILDREN_R));
        }
    }

    private static void deletePeriodNodeL(JSONObject json) {
        if (json == null) {
            return;
        }
        for (int i = 0; i < json.size(); i++) {
            JSONObject childJson = json.getJSONObject(Integer.toString(i));
            String text = childJson.getString(FileConstants.TEXT);
            childJson.put(FileConstants.TEXT, FormatUtil.deletePeriod(text));
            deletePeriodNodeL(childJson.getJSONObject(FileConstants.CHILDREN_L));
        }
    }

    /**
     * 更新导图文件结构
     */
    public static void updateMap(MindMap mindMap) {
        File dir = new File(ConfigConstants.DIR_FILES);
        updateFile(dir);
    }

    private static void updateFile(File dir) {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                updateFile(file);
            } else {
                JSONObject mindMap = readFile(file);
                JSONObject newFormat = new JSONObject();
                for (String key : mindMap.keySet()) {
                    newFormat.put(String.valueOf(IdGenerator.nextId()), mindMap.get(key));
                }
                try (FileWriter fw = new FileWriter(file)) {
                    fw.write(newFormat.toString());
                } catch (IOException e) {
                    MessageUtil.showMessage("更新失败：" + e.getMessage());
                }
            }
        }
    }
}
