package myMind.controller;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import myMind.componet.MindNode;
import myMind.componet.NodeModel;
import myMind.componet.Workspace;
import myMind.constants.PosConstants;
import myMind.util.AlertUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileHandler {

    private static SubjectController subjectController;

    //保存为 JSON 文件
    public static void saveToFile(File file, SubjectController subjectController) {


        StringBuilder sb = new StringBuilder();

        try (FileWriter fw = new FileWriter(file)) {
            fw.write(sb.toString());
            AlertUtil.showAlert("成功", "思维导图已保存到 " + file.getName());
        } catch (IOException e) {
            AlertUtil.showAlert("错误", "保存失败：" + e.getMessage());
        }
    }

    //加载 JSON 文件并重建界面
    public static void loadFromFile(File file, SubjectController subjectController) {
//        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
//            StringBuilder content = new StringBuilder();
//            String line;
//            while ((line = br.readLine()) != null) {
//                content.append(line);
//            }
//            String jsonStr = content.toString();
//            Map<Integer, NodeModel> loadedModels = new HashMap<>();
//            Map<Integer, Integer> parentRelations = new HashMap<>();
//
//            // 提取节点数组
//            JSONObject json = JSONObject.parseObject(jsonStr);
//            JSONArray nodes = json.getJSONArray("nodes");
//
//            for (int i = 0; i < nodes.size(); i++) {
//                JSONObject node = nodes.getJSONObject(i);
//                int id = node.getInteger("id");
//                String text = node.getString("text");
//                double x = node.getInteger("x");
//                double y = node.getInteger("y");
//                Integer parentId = node.getInteger("parentId");
//
//                NodeModel model = new NodeModel(id, text, x, y, PosConstants.RIGHT);
//                loadedModels.put(id, model);
//                if (parentId != null) {
//                    parentRelations.put(id, parentId);
//                }
//            }
//
//            // 重建树结构
//            NodeModel newRoot = null;
//            for (Map.Entry<Integer, NodeModel> entry : loadedModels.entrySet()) {
//                Integer pid = parentRelations.get(entry.getKey());
//                if (pid == null) {
//                    newRoot = entry.getValue();
//                } else {
//                    NodeModel parent = loadedModels.get(pid);
//                    parent.addRightChild(entry.getValue());
//                }
//            }
//
//            subjectController.clearAll();
//            subjectController.setRootModel(newRoot);
//            subjectController.rebuildViewFromModel(newRoot);
//            subjectController.adjustChildrenYR();
//            subjectController.adjustChildrenYL();
//            subjectController.adjustChildrenX();
//            subjectController.adjustChildrenSize();
//            subjectController.refreshLines();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    public static void importFile(File file, SubjectController subjectController, Workspace workspace) {
        FileHandler.subjectController = subjectController;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line);
            }
            String jsonStr = content.toString();
            JSONObject json = JSONObject.parseObject(jsonStr);

            workspace.getTabs().clear();

            importSubjet(json, workspace);

            JSONObject subjects = json.getJSONObject("subjects");
            if (subjects != null) {
                for (int i = 0; i < subjects.size() - 1; i++) {
                    JSONObject subject = subjects.getJSONObject(Integer.toString(i));
                    importSubjet(subject, workspace);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void importSubjet(JSONObject json, Workspace workspace) {
        workspace.addSubject();
        FileHandler.subjectController = workspace.getCurrentController();

        JSONObject rootJson = json.getJSONObject("root");
        JSONObject children = rootJson.getJSONObject("children");
        JSONObject children2 = rootJson.getJSONObject("children2");

        NodeModel rootModel = subjectController.getRootModel();
        rootModel.setText(rootJson.getString("text"));
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

            NodeModel model = new NodeModel(subjectController.nextId(), text, 0, 0, PosConstants.RIGHT);
            parentModel.addRightChild(model);
            subjectController.addNode(fillMindNode(jsonNode, new MindNode(model, subjectController)));

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

            NodeModel model = new NodeModel(subjectController.nextId(), text, 0, 0, PosConstants.LEFT);
            parentModel.addLeftChild(model);
            subjectController.addNode(fillMindNode(jsonNode, new MindNode(model, subjectController)));

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
    public static String saveImage(BufferedImage bufferedImage, String imagePath) {
        if (imagePath == null) {
            imagePath = "D:\\MyMind\\iamges\\" + System.currentTimeMillis() + ".png";
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
