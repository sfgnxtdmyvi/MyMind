package myMind.controller;

import com.alibaba.fastjson2.JSONObject;
import myMind.componet.MindNode;
import myMind.componet.NodeModel;
import myMind.constants.PosConstants;
import myMind.constants.SizeConstants;
import myMind.util.AlertUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class FileHandler {

    private final NodeController nodeController;

    public FileHandler(NodeController nodeController) {
        this.nodeController = nodeController;
    }

    //保存为 JSON 文件
    public void saveToFile(File file) {


        StringBuilder sb = new StringBuilder();

        try (FileWriter fw = new FileWriter(file)) {
            fw.write(sb.toString());
            AlertUtil.showAlert("成功", "思维导图已保存到 " + file.getName());
        } catch (IOException e) {
            AlertUtil.showAlert("错误", "保存失败：" + e.getMessage());
        }
    }

    //加载 JSON 文件并重建界面
    public void loadFromFile(File file) {
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
//            nodeController.clearAll();
//            nodeController.setRootModel(newRoot);
//            nodeController.rebuildViewFromModel(newRoot);
//            nodeController.adjustChildrenYR();
//            nodeController.adjustChildrenYL();
//            nodeController.adjustChildrenX();
//            nodeController.adjustChildrenSize();
//            nodeController.refreshLines();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    public void importFile(File file) {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line);
            }
            String jsonStr = content.toString();

            nodeController.clearAll();
            // 提取节点数组
            JSONObject json = JSONObject.parseObject(jsonStr);
            JSONObject rootJson = json.getJSONObject("root");
            JSONObject children = rootJson.getJSONObject("children");
            JSONObject children2 = rootJson.getJSONObject("children2");

            double centerX = (nodeController.getSubject().getWidth() - SizeConstants.MIN_NODE_WIDTH) / 2.0;
            double centerY = nodeController.getSubject().getHeight() / 2.0 - SizeConstants.MIN_NODE_HEIGHT;
            NodeModel rootModel = new NodeModel(0, rootJson.getString("text"), centerX, centerY, PosConstants.MIDDLE);
            nodeController.addNode(rootModel);

            addRightChild(children, rootModel);
            addLeftChild(children2, rootModel);

            nodeController.setRootModel(rootModel);
            nodeController.adjustChildrenYR();
            nodeController.adjustChildrenYL();
            nodeController.adjustChildrenX();
            nodeController.adjustChildrenSize();
            nodeController.refreshLines();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addRightChild(JSONObject children, NodeModel parentModel) {
        if (children == null) {
            return;
        }

        //children里有一个"objectClass": "NSArray"
        for (int i = 0; i < children.size() - 1; i++) {
            JSONObject jsonNode = children.getJSONObject(Integer.toString(i));
            String text = jsonNode.getString("text");

            NodeModel model = new NodeModel(nodeController.nextId(), text, 0, 0, PosConstants.RIGHT);
            parentModel.addRightChild(model);

            MindNode node;
            String imageName = jsonNode.getString("imageName");
            if(imageName != null){
                String imagePath = "C:\\Users\\k8255\\AppData\\Roaming\\MindLine\\Images\\" + imageName;
                JSONObject imageSize = jsonNode.getJSONObject("imageSize");
                node = new MindNode(model, nodeController, imagePath, imageSize.getDouble("width"), imageSize.getDouble("height"));
            }else {
                node = new MindNode(model, nodeController);
            }

            nodeController.addNode(node);

            addRightChild(jsonNode.getJSONObject("children"), model);
        }
    }

    private void addLeftChild(JSONObject children, NodeModel parentModel) {
        if (children == null) {
            return;
        }

        for (int i = 0; i < children.size() - 1; i++) {
            JSONObject jsonNode = children.getJSONObject(Integer.toString(i));
            String text = jsonNode.getString("text");

            NodeModel model = new NodeModel(nodeController.nextId(), text, 0, 0, PosConstants.LEFT);
            nodeController.addNode(model);
            parentModel.addLeftChild(model);

            addLeftChild(jsonNode.getJSONObject("children"), model);
        }
    }


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
