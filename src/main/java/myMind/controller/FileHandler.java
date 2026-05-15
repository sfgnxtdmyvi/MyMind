package myMind.controller;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import javafx.collections.ObservableList;
import javafx.scene.control.Tab;
import javafx.scene.image.ImageView;
import lombok.Setter;
import myMind.componet.MindNode;
import myMind.componet.NodeModel;
import myMind.componet.Subject;
import myMind.componet.Workspace;
import myMind.constants.PosConstants;
import myMind.util.MessageUtil;
import org.fxmisc.richtext.StyleClassedTextArea;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;

public class FileHandler {

    private static SubjectController subjectController;

    @Setter
    private static Workspace workspace;
    private static String imageDir;

    static {
        ResourceBundle config = ResourceBundle.getBundle("config");
        imageDir = config.getString("directory.image");
    }

    //保存为 JSON 文件
    public static void saveToFile(File file) {
        ObservableList<Tab> tabs = workspace.getTabs();

        JSONObject subjects = new JSONObject();
        for (int i = 0; i < tabs.size(); i++) {
            Tab tab = tabs.get(i);
            subjectController = ((Subject) tab.getContent()).getSubjectController();
            NodeModel rootModel = subjectController.getRootModel();

            JSONObject subject = saveSubject(rootModel);
            subjects.put(Integer.toString(i), subject);
        }

        try (FileWriter fw = new FileWriter(file)) {
            fw.write(subjects.toString());
            MessageUtil.show("保存成功");
        } catch (IOException e) {
            MessageUtil.show("保存失败");
        }
    }

    private static JSONObject saveSubject(NodeModel model) {
        JSONObject rootJson = saveNode(model);
        saveRightChildren(rootJson, model.getRightChildren());
        saveLeftChildren(rootJson, model.getLeftChildren());
        return rootJson;
    }

    private static JSONObject saveNode(NodeModel model) {
        JSONObject json = new JSONObject();

        MindNode mindNode = model.getMindNode();
        StyleClassedTextArea textArea = mindNode.getTextArea();
        String text = textArea.getText();
        json.put("text", text);
        if (!text.isEmpty()) {
            JSONArray styles = extractStyles(textArea, text.length());
            if (!styles.isEmpty()) {
                json.put("styles", extractStyles(textArea, text.length()));
            }
        }

        String imageName = mindNode.getImageName();
        if (imageName != null) {
            json.put("imageName", imageName);
            ImageView image = mindNode.getImage();
            json.put("imageWidth", image.getFitWidth());
            json.put("imageHeight", image.getFitHeight());
        }

        return json;
    }

    private static void saveRightChildren(JSONObject parentJson, List<NodeModel> rightChildren) {
        if (!rightChildren.isEmpty()) {
            JSONObject rightChildrenJson = new JSONObject();
            for (int i = 0; i < rightChildren.size(); i++) {
                NodeModel childModel = rightChildren.get(i);
                JSONObject childJson = saveNode(childModel);
                saveRightChildren(childJson, childModel.getRightChildren());
                rightChildrenJson.put(Integer.toString(i), childJson);
            }
            parentJson.put("rightChildren", rightChildrenJson);
        }
    }

    private static void saveLeftChildren(JSONObject parentJson, List<NodeModel> leftChildren) {
        if (!leftChildren.isEmpty()) {
            JSONObject leftChildrenJson = new JSONObject();
            for (int i = 0; i < leftChildren.size(); i++) {
                NodeModel childModel = leftChildren.get(i);
                JSONObject childJson = saveNode(childModel);
                saveLeftChildren(childJson, childModel.getLeftChildren());
                leftChildrenJson.put(Integer.toString(i), childJson);
            }
            parentJson.put("leftChildren", leftChildrenJson);
        }
    }

    public static JSONArray extractStyles(StyleClassedTextArea textArea, int length) {
        JSONArray styles = new JSONArray();

        int start = 0;
        Collection<String> lastStyles = textArea.getStyleOfChar(0);

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

    //加载 JSON 文件并重建界面
    public static void loadFromFile(File file) {
        subjectController = workspace.getCurrentController();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line);
            }
            JSONObject json = JSONObject.parseObject(content.toString());

            workspace.getTabs().clear();

            for (int i = 0; i < json.size(); i++) {
                workspace.addSubject();
                subjectController = workspace.getCurrentController();
                JSONObject subject = json.getJSONObject(Integer.toString(i));

                NodeModel rootModel = subjectController.getRootModel();
                rootModel.getMindNode().getTextArea().replaceText(subject.getString("text"));
                loadNode(subject, rootModel.getMindNode());

                JSONObject rightChildren = subject.getJSONObject("rightChildren");
                JSONObject leftChildren = subject.getJSONObject("leftChildren");
                loadRightChild(rightChildren, rootModel);
                loadLeftChild(leftChildren, rootModel);

                subjectController.adjustChildrenYR();
                subjectController.adjustChildrenYL();
                subjectController.adjustChildrenX();
                subjectController.adjustChildrenSize();
                subjectController.refreshLines();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void loadRightChild(JSONObject children, NodeModel parentModel) {
        if (children == null) {
            return;
        }

        for (int i = 0; i < children.size(); i++) {
            JSONObject jsonNode = children.getJSONObject(Integer.toString(i));
            String text = jsonNode.getString("text");

            NodeModel model = new NodeModel(0, 0, PosConstants.RIGHT);
            parentModel.addRightChild(model);
            MindNode node = new MindNode(model, subjectController, text);
            loadNode(jsonNode, node);
            subjectController.addNode(node);

            loadRightChild(jsonNode.getJSONObject("rightChildren"), model);
        }
    }
    private static void loadLeftChild(JSONObject children, NodeModel parentModel) {
        if (children == null) {
            return;
        }

        for (int i = 0; i < children.size(); i++) {
            JSONObject jsonNode = children.getJSONObject(Integer.toString(i));
            String text = jsonNode.getString("text");

            NodeModel model = new NodeModel(0, 0, PosConstants.LEFT);
            parentModel.addLeftChild(model);
            MindNode node = new MindNode(model, subjectController, text);
            loadNode(jsonNode, node);
            subjectController.addNode(node);

            loadLeftChild(jsonNode.getJSONObject("leftChildren"), model);
        }
    }

    private static void loadNode(JSONObject json, MindNode node) {
        // 填充图片
        String imageName = json.getString("imageName");
        if (imageName != null) {
            String imagePath = imageDir + imageName;
            node.addImage(imagePath, json.getDouble("imageWidth"), json.getDouble("imageHeight"));
        }

        // 文本样式
        JSONArray styles = json.getJSONArray("styles");
        if (styles != null) {
            for (int i = 0; i < styles.size(); i++) {
                JSONObject styleItem = styles.getJSONObject(i);
                JSONArray styleArray = styleItem.getJSONArray("style");
                List<String> styleList = new ArrayList<>();
                for (int j = 0; j < styleArray.size(); j++) {
                    styleList.add(styleArray.getString(j));
                }

                node.getTextArea().setStyle(styleItem.getIntValue("start"),
                        styleItem.getIntValue("end"),
                        styleList);
            }
        }
    }

    public static void importFile(File file) {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line);
            }
            JSONObject json = JSONObject.parseObject(content.toString());

            workspace.getTabs().clear();

            importSubjet(json);

            JSONObject subjects = json.getJSONObject("subjects");
            if (subjects != null) {
                for (int i = 0; i < subjects.size() - 1; i++) {
                    JSONObject subject = subjects.getJSONObject(Integer.toString(i));
                    importSubjet(subject);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void importSubjet(JSONObject json) {
        workspace.addSubject();
        subjectController = workspace.getCurrentController();

        JSONObject rootJson = json.getJSONObject("root");

        NodeModel rootModel = subjectController.getRootModel();
        rootModel.getMindNode().getTextArea().replaceText(rootJson.getString("text"));
        importNode(rootJson, rootModel.getMindNode());

        JSONObject children = rootJson.getJSONObject("children");
        JSONObject children2 = rootJson.getJSONObject("children2");
        importRightChild(children, rootModel);
        importLeftChild(children2, rootModel);

        subjectController.adjustChildrenYR();
        subjectController.adjustChildrenYL();
        subjectController.adjustChildrenX();
        subjectController.adjustChildrenSize();
        subjectController.refreshLines();
    }

    private static void importRightChild(JSONObject children, NodeModel parentModel) {
        if (children == null) {
            return;
        }

        //children里有一个"objectClass": "NSArray"
        for (int i = 0; i < children.size() - 1; i++) {
            JSONObject jsonNode = children.getJSONObject(Integer.toString(i));
            String text = jsonNode.getString("text");

            NodeModel model = new NodeModel(0, 0, PosConstants.RIGHT);
            parentModel.addRightChild(model);
            MindNode node = new MindNode(model, subjectController, text);
            importNode(jsonNode, node);
            subjectController.addNode(node);

            importRightChild(jsonNode.getJSONObject("children"), model);
        }
    }

    private static void importLeftChild(JSONObject children, NodeModel parentModel) {
        if (children == null) {
            return;
        }

        for (int i = 0; i < children.size() - 1; i++) {
            JSONObject jsonNode = children.getJSONObject(Integer.toString(i));
            String text = jsonNode.getString("text");

            NodeModel model = new NodeModel(0, 0, PosConstants.LEFT);
            parentModel.addLeftChild(model);
            MindNode node = new MindNode(model, subjectController, text);
            importNode(jsonNode, node);
            subjectController.addNode(node);

            importLeftChild(jsonNode.getJSONObject("children"), model);
        }
    }

    private static void importNode(JSONObject json, MindNode node) {
        // 填充图片
        String imageName = json.getString("imageName");
        if (imageName != null) {
            String imagePath = "C:\\Users\\k8255\\AppData\\Roaming\\MindLine\\Images\\" + imageName;
            JSONObject imageSize = json.getJSONObject("imageSize");
            node.addImage(imagePath, imageSize.getDouble("width"), imageSize.getDouble("height"));
        }

        // 文本样式
        JSONArray style = json.getJSONArray("style");
        if (style != null) {
            for (int i = 0; i < style.size(); i++) {
                JSONObject styleItem = style.getJSONObject(i);
                Boolean bold = styleItem.getBoolean("bold");
                List<String> styleList = new ArrayList<>();
                String color = styleItem.getString("color");
                if (bold != null) {
                    styleList.add("bold-text");
                } else if (color != null && color.equals("#FF0000")) {
                    styleList.add("red-text");
                }

                node.getTextArea().setStyle(styleItem.getIntValue("start"),
                        styleItem.getIntValue("end"),
                        styleList);
            }
        }
    }

    //—————————————————————————————————————————图片—————————————————————————————————————————
    public static String saveImage(BufferedImage bufferedImage, String imageName) {
        if (imageName == null) {
            imageName = System.currentTimeMillis() + ".png";
        }
        String imagePath = imageDir + imageName;

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
