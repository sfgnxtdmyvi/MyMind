package myMind.controller;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import javafx.scene.image.ImageView;
import lombok.Setter;
import myMind.componet.MindNode;
import myMind.componet.NodeModel;
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

public class FileHandler {

    private static SubjectController subjectController;

    @Setter
    private static Workspace workspace;

    //保存为 JSON 文件
    public static void saveToFile(File file) {
        subjectController = workspace.getCurrentController();

        JSONObject root = new JSONObject();
        NodeModel rootModel = subjectController.getRootModel();
        MindNode mindNode = rootModel.getMindNode();
        StyleClassedTextArea textArea = mindNode.getTextArea();
        String text = textArea.getText();
        root.put("text", text);
        if (!text.isEmpty()) {
            root.put("styles", extractStyles(textArea, text.length()));
        }

        String imageName = mindNode.getImageName();
        if(imageName != null){
            root.put("imageName", imageName);
            ImageView image = mindNode.getImage();
            root.put("imageWidth", image.getFitWidth());
            root.put("imageHeight", image.getFitHeight());
        }

        JSONObject rightChildren = new JSONObject();
        JSONObject leftChildren = new JSONObject();
        root.put("rightChildren", rightChildren);
        root.put("leftChildren", leftChildren);

        try (FileWriter fw = new FileWriter(file)) {
            fw.write(root.toString());
            MessageUtil.show("保存成功");
        } catch (IOException e) {
            MessageUtil.show("保存失败");
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
            String jsonStr = content.toString();
            JSONObject json = JSONObject.parseObject(jsonStr);

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

    public static void importFile(File file) {
        subjectController = workspace.getCurrentController();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line);
            }
            String jsonStr = content.toString();
            JSONObject json = JSONObject.parseObject(jsonStr);

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

        JSONObject rootJson = json.getJSONObject("root");
        JSONObject children = rootJson.getJSONObject("children");
        JSONObject children2 = rootJson.getJSONObject("children2");

        NodeModel rootModel = subjectController.getRootModel();
        rootModel.getMindNode().getTextArea().replaceText(rootJson.getString("text"));
        fillMindNode(rootJson, rootModel.getMindNode());

        addRightChild(children, rootModel);
        addLeftChild(children2, rootModel);

        subjectController.adjustChildrenYR();
        subjectController.adjustChildrenYL();
        subjectController.adjustChildrenX();
        subjectController.adjustChildrenSize();
        subjectController.refreshLines();
    }

    private static void addRightChild(JSONObject children, NodeModel parentModel) {
        if (children == null) {
            return;
        }

        //children里有一个"objectClass": "NSArray"
        for (int i = 0; i < children.size() - 1; i++) {
            JSONObject jsonNode = children.getJSONObject(Integer.toString(i));
            String text = jsonNode.getString("text");

            NodeModel model = new NodeModel(0, 0, PosConstants.RIGHT);
            parentModel.addRightChild(model);
            subjectController.addNode(fillMindNode(jsonNode, new MindNode(model, subjectController, text)));

            addRightChild(jsonNode.getJSONObject("children"), model);
        }
    }

    private static void addLeftChild(JSONObject children, NodeModel parentModel) {
        if (children == null) {
            return;
        }

        for (int i = 0; i < children.size() - 1; i++) {
            JSONObject jsonNode = children.getJSONObject(Integer.toString(i));
            String text = jsonNode.getString("text");

            NodeModel model = new NodeModel(0, 0, PosConstants.LEFT);
            parentModel.addLeftChild(model);
            subjectController.addNode(fillMindNode(jsonNode, new MindNode(model, subjectController, text)));

            addLeftChild(jsonNode.getJSONObject("children"), model);
        }
    }

    private static MindNode fillMindNode(JSONObject json, MindNode node) {
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

        return node;
    }

    //—————————————————————————————————————————图片—————————————————————————————————————————
    public static String saveImage(BufferedImage bufferedImage, String imageName) {
        String imagePath = "D:\\MyMind\\iamges\\";
        if (imageName == null) {
            imageName = System.currentTimeMillis() + ".png";
            imagePath += imageName;
        }
        File output = new File(imagePath);
        try {
            ImageIO.write(bufferedImage, "png", output);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return imagePath;
    }

    public static void deleteImage(String imagePath) {
        File file = new File(imagePath);
        if (file.exists()) {
            file.delete();
        }
    }

}
