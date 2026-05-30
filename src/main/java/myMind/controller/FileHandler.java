package myMind.controller;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import javafx.collections.ObservableList;
import javafx.scene.control.Tab;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import lombok.Getter;
import myMind.componet.MindNode;
import myMind.model.NodeModel;
import myMind.componet.Subject;
import myMind.componet.Workspace;
import myMind.constants.PosConstants;
import myMind.constants.SizeConstants;
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
import java.util.ResourceBundle;

public class FileHandler {

    private static SubjectController subjectController;

    private Workspace workspace;
    @Getter
    private static String dirImage;
    private static String dirRecentFiles;

    static {
        ResourceBundle config = ResourceBundle.getBundle("config");
        dirImage = config.getString("directory.images");
        dirRecentFiles = config.getString("directory.recent_files");
    }

    private LinkedList<String> recentFiles;

    public FileHandler(Workspace workspace) {
        this.workspace = workspace;
    }

    //—————————————————————————————————————————保存—————————————————————————————————————————
    public void saveFile(File file) {
        ObservableList<Tab> tabs = workspace.getTabs();
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

        try (FileWriter fw = new FileWriter(file)) {
            fw.write(subjects.toString());
            MessageUtil.showMessage("保存成功");
        } catch (IOException e) {
            MessageUtil.showMessage("保存失败：" + e.getMessage());
        }
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

    //—————————————————————————————————————————打开—————————————————————————————————————————
    public void loadFile(File file) {
        JSONObject json = readFile(file);
        // 加载主题
        for (int i = 0; i < json.size(); i++) {
            workspace.addSubject();
            subjectController = workspace.getSubjectController();
            JSONObject subject = json.getJSONObject(Integer.toString(i));

            // 加载根节点
            NodeModel rootModel = subjectController.getRootModel();
            MindNode rootNode = subjectController.getRootNode();
            loadNode(subject, rootNode);

            // 加载子节点
            loadChildR(subject.getJSONObject("childrenR"), rootModel);
            loadChildL(subject.getJSONObject("childrenL"), rootModel);

            subjectController.adjustChildrenSize();
            subjectController.adjustXY();
        }

        addRecentFile(file.getName());
    }

    private void loadNode(JSONObject json, MindNode node) {
        // 文本样式
        node.getTextArea().replaceText(json.getString("text"));
        JSONArray styles = json.getJSONArray("styles");
        if (styles != null) {
            StyleClassedTextArea textArea = node.getTextArea();
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

        // 图片
        String imageName = json.getString("imageName");
        if (imageName != null) {
            node.loadImage(imageName, json.getDouble("imageWidth"), json.getDouble("imageHeight"));
        }
    }

    private void loadChildR(JSONObject children, NodeModel parentModel) {
        if (children == null) {
            return;
        }

        for (int i = 0; i < children.size(); i++) {
            JSONObject jsonNode = children.getJSONObject(Integer.toString(i));

            NodeModel model = new NodeModel(0, 0, PosConstants.RIGHT);
            parentModel.addChildR(model);
            MindNode node = new MindNode(model);
            loadNode(jsonNode, node);
            subjectController.addNode(node);

            loadChildR(jsonNode.getJSONObject("childrenR"), model);
        }
    }

    private void loadChildL(JSONObject children, NodeModel parentModel) {
        if (children == null) {
            return;
        }

        for (int i = 0; i < children.size(); i++) {
            JSONObject jsonNode = children.getJSONObject(Integer.toString(i));

            NodeModel model = new NodeModel(0, 0, PosConstants.LEFT);
            parentModel.addChildL(model);
            MindNode node = new MindNode(model);
            loadNode(jsonNode, node);
            subjectController.addNode(node);

            loadChildL(jsonNode.getJSONObject("childrenL"), model);
        }
    }

    //—————————————————————————————————————————导入—————————————————————————————————————————
    private JSONObject readFile(File file) {
        StringBuilder content = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line);
            }

            workspace.getTabs().clear();
            Stage stage = (Stage) workspace.getScene().getWindow();
            stage.setTitle(file.getName().substring(0, file.getName().length() - 3));
        } catch (Exception e) {
            MessageUtil.showMessage("读取失败：" + e.getMessage());
        }
        return JSONObject.parseObject(content.toString());
    }

    public void importFile(File file) {
        JSONObject json = readFile(file);

        importSubjet(json);

        JSONObject subjects = json.getJSONObject("subjects");
        if (subjects != null) {
            for (int i = 0; i < subjects.size() - 1; i++) {
                importSubjet(subjects.getJSONObject(Integer.toString(i)));
            }
        }
    }

    private void importSubjet(JSONObject json) {
        workspace.addSubject();
        subjectController = workspace.getSubjectController();

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
            JSONObject imageSize = json.getJSONObject("imageSize");
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

            NodeModel model = new NodeModel(0, 0, PosConstants.RIGHT);
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
    private void addRecentFile(String fileName) {
        LinkedList<String> recentFiles = getRecentFiles();

        recentFiles.remove(fileName);
        recentFiles.addFirst(fileName);
        if (recentFiles.size() > SizeConstants.MAX_RECENT_FILES) {
            recentFiles.removeLast();
        }

        saveRecentFiles(recentFiles);
    }

    public LinkedList<String> getRecentFiles() {
        if (recentFiles == null) {
            recentFiles = new LinkedList<>();
            File file = new File(dirRecentFiles);

            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    recentFiles.add(line);
                }
            } catch (IOException e) {
                MessageUtil.showMessage("读取失败：" + e.getMessage());
            }
        }

        return recentFiles;
    }

    private void saveRecentFiles(List<String> recentFiles) {
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

    public static void deleteImage(String imagePath) {
        File file = new File(imagePath);
        if (file.exists()) {
            file.delete();
        }
    }

}
