package myMind.util;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import javafx.collections.ObservableList;
import javafx.scene.control.Tab;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.Getter;
import myMind.componet.MindMap;
import myMind.componet.MindNode;
import myMind.constants.FileConstants;
import myMind.constants.PosConstants;
import myMind.constants.SizeConstants;
import myMind.controller.SubjectController;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class FileUtil {
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
    private static final Map<String, ScheduledFuture<?>> fileSaveFutures = new ConcurrentHashMap<>();

    @Getter
    private static final LinkedList<String> recentFiles;

    private static File openFileChooser(int type, MindMap mindMap) {
        FileChooser fc = new FileChooser();
        fc.setInitialDirectory(new File(FileConstants.DIR_FILES));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("MyMind Files", "*.mm"));
        if (type == FileConstants.OPEN_TYPE) {
            return fc.showOpenDialog(mindMap.getScene().getWindow());
        } else {
            return fc.showSaveDialog(mindMap.getScene().getWindow());
        }
    }

    //—————————————————————————————————————————打开—————————————————————————————————————————

    public static void load(MindMap mindMap) {
        File file = openFileChooser(FileConstants.OPEN_TYPE, mindMap);
        if (file != null) {
            FileUtil.loadFile(file, mindMap);
        }
    }

    public static void loadFile(File file, MindMap mindMap) {
        if (mindMap.getFilePath() != null) {
            cancelSchedule(mindMap.getFilePath());
        }
        mindMap.getTabs().clear();

        JSONObject json = readFile(file);
        Stage stage = (Stage) mindMap.getScene().getWindow();
        stage.setTitle(file.getName().substring(0, file.getName().length() - 3));

        // 加载主题
        for (int i = 0; i < json.size(); i++) {
            JSONObject subject = json.getJSONObject(Integer.toString(i));
            MindNode rootNode = buildNode(subject, PosConstants.MIDDLE);
            mindMap.addSubject(rootNode);
            SubjectController subjectController = mindMap.getSubjectController();

            // 加载子节点
            loadChildR(subject.getJSONObject("childrenR"), rootNode, subjectController);
            loadChildL(subject.getJSONObject("childrenL"), rootNode, subjectController);

            subjectController.adjustChildrenSize();
            subjectController.adjustXY();
        }

        String absolutePath = file.getAbsolutePath();
        mindMap.setFilePath(absolutePath);
        scheduleAutoSave(absolutePath, mindMap);
        addRecentFile(file);
    }

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

    private static void loadChildR(JSONObject json, MindNode parentNode, SubjectController subjectController) {
        if (json == null) {
            return;
        }

        for (int i = 0; i < json.size(); i++) {
            JSONObject childrenJson = json.getJSONObject(Integer.toString(i));

            MindNode node = buildNode(childrenJson, PosConstants.RIGHT);
            parentNode.addChildR(node);
            subjectController.addNode(node);

            loadChildR(childrenJson.getJSONObject("childrenR"), node, subjectController);
        }
    }

    private static void loadChildL(JSONObject json, MindNode parentNode, SubjectController subjectController) {
        if (json == null) {
            return;
        }

        for (int i = 0; i < json.size(); i++) {
            JSONObject childrenJson = json.getJSONObject(Integer.toString(i));

            MindNode node = buildNode(childrenJson, PosConstants.LEFT);
            parentNode.addChildL(node);
            subjectController.addNode(node);

            loadChildL(childrenJson.getJSONObject("childrenL"), node, subjectController);
        }
    }

    private static MindNode buildNode(JSONObject json, byte pos) {
        String imageName = json.getString("imageName");
        MindNode node;
        if (imageName != null) {
            node = new MindNode(pos, imageName, json.getDouble("imageWidth"), json.getDouble("imageHeight"), buildTextArea(json));
        } else {
            node = new MindNode(pos, buildTextArea(json));
        }
        return node;
    }

    private static StyleClassedTextArea buildTextArea(JSONObject json) {
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
                cancelSchedule(mindMap.getFilePath());
            }
            String absolutePath = file.getAbsolutePath();
            scheduleAutoSave(absolutePath, mindMap);

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

    public static void saveFileScheduled(File file, MindMap mindMap) {
        JSONObject subjects = saveSubjects(mindMap);

        try (FileWriter fw = new FileWriter(file)) {
            fw.write(subjects.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static JSONObject saveSubjects(MindMap mindMap) {
        ObservableList<Tab> tabs = mindMap.getTabs();
        JSONObject subjects = new JSONObject();
        for (int i = 0; i < tabs.size(); i++) {
            SubjectController subjectController = (SubjectController) tabs.get(i).getUserData();
            MindNode rootNode = subjectController.getRootNode();

            JSONObject subject = saveNode(rootNode);
            saveChildrenR(subject, rootNode.getChildrenR());
            saveChildrenL(subject, rootNode.getChildrenL());

            subjects.put(Integer.toString(i), subject);
        }
        return subjects;
    }

    private static void saveChildrenR(JSONObject parentJson, List<MindNode> childrenR) {
        if (!childrenR.isEmpty()) {
            JSONObject childrenRJson = new JSONObject();
            for (int i = 0; i < childrenR.size(); i++) {
                MindNode node = childrenR.get(i);
                JSONObject childJson = saveNode(node);
                childrenRJson.put(Integer.toString(i), childJson);

                saveChildrenR(childJson, node.getChildrenR());
            }
            parentJson.put("childrenR", childrenRJson);
        }
    }

    private static void saveChildrenL(JSONObject parentJson, List<MindNode> childrenL) {
        if (!childrenL.isEmpty()) {
            JSONObject childrenLJson = new JSONObject();
            for (int i = 0; i < childrenL.size(); i++) {
                MindNode node = childrenL.get(i);
                JSONObject childJson = saveNode(node);
                childrenLJson.put(Integer.toString(i), childJson);

                saveChildrenL(childJson, node.getChildrenL());
            }
            parentJson.put("childrenL", childrenLJson);
        }
    }

    private static JSONObject saveNode(MindNode node) {
        JSONObject json = new JSONObject();

        // 文本
        StyleClassedTextArea textArea = node.getTextArea();
        String text = textArea.getText();
        json.put("text", text);
        // 样式
        if (!text.isEmpty()) {
            JSONArray styles = saveStyles(textArea);
            if (!styles.isEmpty()) {
                json.put("styles", styles);
            }
        }

        // 图片
        String imageName = node.getImageName();
        if (imageName != null) {
            json.put("imageName", imageName);
            ImageView image = node.getImageView();
            json.put("imageWidth", image.getFitWidth());
            json.put("imageHeight", image.getFitHeight());
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

    public static void scheduleAutoSave(String filePath, MindMap mindMap) {
        if (fileSaveFutures.containsKey(filePath)) {
            return;
        }

        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() ->
                saveFileScheduled(new File(filePath), mindMap), 1, 1, TimeUnit.SECONDS);
        fileSaveFutures.put(filePath, future);
    }

    public static void cancelSchedule(String filePath) {
        ScheduledFuture<?> future = fileSaveFutures.remove(filePath);
        if (future != null) {
            future.cancel(false);
        }
    }

    // ScheduledExecutorService 创建的是非守护线程，会阻止 JVM 自然退出，需要关闭
    public static void cancelSchedule() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }

    //—————————————————————————————————————————最近打开—————————————————————————————————————————
    static {
        recentFiles = new LinkedList<>();
        File file = new File(FileConstants.DIR_RECENT_FILES);

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
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FileConstants.DIR_RECENT_FILES))) {
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
        String imagePath = FileConstants.DIR_IMAGE + imageName;

        File output = new File(imagePath);
        try {
            ImageIO.write(bufferedImage, "png", output);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return imageName;
    }

    public static void deleteImage(String imageName) {
        File file = new File(FileConstants.DIR_IMAGE + imageName);
        if (file.exists()) {
            file.delete();
        }
    }

}
