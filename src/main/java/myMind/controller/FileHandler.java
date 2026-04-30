package myMind.controller;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import myMind.componet.MindNode;
import myMind.componet.NodeModel;
import myMind.constants.PosConstants;
import myMind.constants.SizeConstants;
import myMind.util.AlertUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileHandler {

    private final NodeController nodeController;

    public FileHandler(NodeController nodeController) {
        this.nodeController = nodeController;
    }

    //保存为 JSON 文件
    public void saveToFile(File file) {
        List<NodeModel> allNodes = new ArrayList<>(nodeController.getNodeMap().size());
        for (MindNode view : nodeController.getNodeMap().values()) {
            allNodes.add(view.getModel());
        }

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
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line);
            }
            String jsonStr = content.toString();
            Map<Integer, NodeModel> loadedModels = new HashMap<>();
            Map<Integer, Integer> parentRelations = new HashMap<>();

            // 提取节点数组
            JSONObject json = JSONObject.parseObject(jsonStr);
            JSONArray nodes = json.getJSONArray("nodes");

            for (int i = 0; i < nodes.size(); i++) {
                JSONObject node = nodes.getJSONObject(i);
                int id = node.getInteger("id");
                String text = node.getString("text");
                double x = node.getInteger("x");
                double y = node.getInteger("y");
                Integer parentId = node.getInteger("parentId");

                NodeModel model = new NodeModel(id, text, x, y, PosConstants.RIGHT);
                loadedModels.put(id, model);
                if (parentId != null) {
                    parentRelations.put(id, parentId);
                }
            }

            // 重建树结构
            NodeModel newRoot = null;
            for (Map.Entry<Integer, NodeModel> entry : loadedModels.entrySet()) {
                Integer pid = parentRelations.get(entry.getKey());
                if (pid == null) {
                    newRoot = entry.getValue();
                } else {
                    NodeModel parent = loadedModels.get(pid);
                    parent.addRightChild(entry.getValue());
                }
            }

            nodeController.clearAll();
            nodeController.setRootModel(newRoot);
            nodeController.rebuildViewFromModel(newRoot);
            nodeController.adjustChildrenYR();
            nodeController.adjustChildrenYL();
            nodeController.adjustChildrenX();
            nodeController.adjustChildrenSize();
            nodeController.refreshLines();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void importFile(File file) {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line);
            }
            String jsonStr = content.toString();

            // 提取节点数组
            JSONObject json = JSONObject.parseObject(jsonStr);
            JSONObject rootJson = json.getJSONObject("root");
            JSONObject children = rootJson.getJSONObject("children");

            double centerX = (nodeController.getSubject().getWidth() - SizeConstants.MIN_NODE_WIDTH) / 2.0;
            double centerY = nodeController.getSubject().getHeight() / 2.0 - SizeConstants.MIN_NODE_HEIGHT;
            NodeModel rootModel = new NodeModel(-1, rootJson.getString("text"), centerX, centerY, PosConstants.MIDDLE);
            //children里有一个"objectClass": "NSArray"
            for (int i = 0; i < children.size()-1; i++) {
                JSONObject node = children.getJSONObject(""+i);
                int id = i;
                String text = node.getString("text");

                NodeModel model = new NodeModel(id, text, 0,0, PosConstants.RIGHT);
                rootModel.addRightChild(model);
            }

            nodeController.clearAll();
            nodeController.setRootModel(rootModel);
            nodeController.rebuildViewFromModel(rootModel);
            nodeController.adjustChildrenYR();
            nodeController.adjustChildrenYL();
            nodeController.adjustChildrenX();
            nodeController.adjustChildrenSize();
            nodeController.refreshLines();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
