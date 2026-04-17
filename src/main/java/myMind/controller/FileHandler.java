package myMind.controller;

import myMind.componet.MindNode;
import myMind.componet.NodeModel;
import myMind.constants.PosConstants;
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
        sb.append("{\n  \"nodes\":[\n");
        for (int i = 0; i < allNodes.size(); i++) {
            NodeModel n = allNodes.get(i);
            sb.append(String.format("    {\"id\":%d, \"text\":\"%s\", \"x\":%.2f, \"y\":%.2f, \"parentId\":%s}",
                    n.getId(), escapeJson(n.getText()), n.getX(), n.getY(),
                    n.getParent() == null ? "null" : String.valueOf(n.getParent().getId())));
            if (i < allNodes.size() - 1) sb.append(", ");
            sb.append("\n");
        }
        sb.append("  ]\n}");

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
            while ((line = br.readLine()) != null) content.append(line);
            String json = content.toString();
            // 简易解析 （生产环境建议用 JSON 库，这里手动解析足够演示）
            Map<Integer, NodeModel> loadedModels = new HashMap<>();
            Map<Integer, Integer> parentRelations = new HashMap<>();
            // 提取节点数组
            int nodesStart = json.indexOf("\"nodes\":");
            if (nodesStart == -1) throw new RuntimeException("无效文件格式");
            int arrStart = json.indexOf("[", nodesStart);
            int arrEnd = json.lastIndexOf("]");
            String nodesStr = json.substring(arrStart + 1, arrEnd);
            String[] nodeEntries = splitNodeEntries(nodesStr);
            for (String entry : nodeEntries) {
                int id = extractInt(entry, "\"id\":");
                String text = extractString(entry, "\"text\":");
                double x = extractDouble(entry, "\"x\":");
                double y = extractDouble(entry, "\"y\":");
                Integer parentId = extractIntOrNull(entry, "\"parentId\":");
                NodeModel model = new NodeModel(id, text, x, y, PosConstants.RIGHT);
                loadedModels.put(id, model);
                if (parentId != null) parentRelations.put(id, parentId);
            }
            // 重建树结构
            NodeModel newRoot = null;
            for (Map.Entry<Integer, NodeModel> entry : loadedModels.entrySet()) {
                Integer pid = parentRelations.get(entry.getKey());
                if (pid == null) {
                    newRoot = entry.getValue();
                } else {
                    NodeModel parent = loadedModels.get(pid);
                    if (parent != null) parent.addRightChild(entry.getValue());
                }
            }
            if (newRoot == null) throw new RuntimeException("没有根节点");
            // 替换当前所有内容
            nodeController.clearAll();
            nodeController.setRootModel(newRoot);
            // 重建视图
            nodeController.rebuildViewFromModel(newRoot);
            nodeController.refreshLines();
            AlertUtil.showAlert("成功", "加载完成");
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.showAlert("错误", "加载失败：" + e.getMessage());
        }
    }

    // 辅助解析方法
    private String[] splitNodeEntries(String nodesStr) {
        List<String> list = new ArrayList<>();
        int braceDepth = 0;
        StringBuilder cur = new StringBuilder();
        for (char c : nodesStr.toCharArray()) {
            if (c == '{') braceDepth++;
            if (c == '}') braceDepth--;
            cur.append(c);
            if (braceDepth == 0 && cur.length() > 0) {
                String trimmed = cur.toString().trim();
                if (!trimmed.isEmpty() && !trimmed.equals(", ")) {
                    if (trimmed.endsWith(", ")) trimmed = trimmed.substring(0, trimmed.length() - 1);
                    list.add(trimmed);
                }
                cur.setLength(0);
            }
        }
        return list.toArray(new String[0]);
    }

    private int extractInt(String str, String key) {
        int idx = str.indexOf(key);
        if (idx == -1) return 0;
        int start = idx + key.length();
        int end = start;
        while (end < str.length() && (Character.isDigit(str.charAt(end)) || str.charAt(end) == '-')) end++;
        return Integer.parseInt(str.substring(start, end));
    }

    private Integer extractIntOrNull(String str, String key) {
        int idx = str.indexOf(key);
        if (idx == -1) return null;
        int start = idx + key.length();
        if (str.substring(start).trim().startsWith("null")) return null;
        int end = start;
        while (end < str.length() && (Character.isDigit(str.charAt(end)) || str.charAt(end) == '-')) end++;
        return Integer.parseInt(str.substring(start, end));
    }

    private double extractDouble(String str, String key) {
        int idx = str.indexOf(key);
        if (idx == -1) return 0;
        int start = idx + key.length();
        int end = start;
        while (end < str.length() && (Character.isDigit(str.charAt(end)) || str.charAt(end) == '.' || str.charAt(end) == '-'))
            end++;
        return Double.parseDouble(str.substring(start, end));
    }

    private String extractString(String str, String key) {
        int idx = str.indexOf(key);
        if (idx == -1) return "";
        int start = str.indexOf("\"", idx + key.length()) + 1;
        int end = str.indexOf("\"", start);
        return str.substring(start, end).replace("\\\"", "\"");
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }
}
