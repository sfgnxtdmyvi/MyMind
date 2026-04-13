package myMind.controller;

import javafx.geometry.Point2D;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.stage.FileChooser;
import myMind.componet.MindPane;
import myMind.componet.MindNode;
import myMind.componet.NodeModel;
import myMind.constants.SizeConstants;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class MindController {
	private final MindPane mindPane = new MindPane(this);
	private final Map<Integer, MindNode> nodeMap = new HashMap<>();
	private NodeModel rootModel;
	private MindNode selectedNode = null;
	private final AtomicInteger idGenerator = new AtomicInteger(1);

	public MindPane getMindMapPane() {
		return mindPane;
	}

	public void initRootNode(double centerX, double centerY) {
		rootModel = new NodeModel(nextId(), "", centerX, centerY);
		addNode(rootModel);
	}

	public ToolBar createToolBar() {
		ToolBar toolBar = new ToolBar();

		Button addBtn = new Button("添加子结点");
		Button delBtn = new Button("删除结点");
		Button saveBtn = new Button("保存");
		Button loadBtn = new Button("加载");

		addBtn.setOnAction(e -> addChild());
		delBtn.setOnAction(e -> delete());
		saveBtn.setOnAction(e -> {
			FileChooser fc = new FileChooser();
			fc.setInitialFileName("mindmap.json");
			fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON 文件", "*.json"));
			File file = fc.showSaveDialog(mindPane.getScene().getWindow());
			if (file != null) saveToFile(file);
		});
		loadBtn.setOnAction(e -> {
			FileChooser fc = new FileChooser();
			fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON 文件", "*.json"));
			File file = fc.showOpenDialog(mindPane.getScene().getWindow());
			if (file != null) loadFromFile(file);
		});

		toolBar.getItems().addAll(addBtn, delBtn, new Separator(), saveBtn, loadBtn);
		return toolBar;
	}

	public void setSelectedNode(MindNode node) {
		selectedNode = node;
	}

	public void addChild() {
		NodeModel parentModel = selectedNode.getModel();
		double parentWidth = selectedNode.getWidth();

		// 基于已有最后一个子结点位置加上偏移
		List<NodeModel> children = parentModel.getChildren();
		double childX = parentModel.getX() + parentWidth + SizeConstants.NODE_GAP_X;
		double childY;
		if (children == null || children.isEmpty()) {
			childY = parentModel.getY();
		} else {
			NodeModel lastModel = children.get(children.size() - 1);
			double height = lastModel.getMindNode().getLayoutBounds().getHeight();
			double y = lastModel.getY();
			childY = lastModel.getY() + height + SizeConstants.NODE_GAP_Y;
		}

		NodeModel childModel = new NodeModel(nextId(), "", childX, childY);
		parentModel.addChild(childModel);
		addNode(childModel);
		double midY = parentModel.getMidY();
		double height = selectedNode.getHeight();
		double y = midY - height / 2;
		parentModel.setY(y);
		refreshLines();
	}

	public void addSibling() {
		NodeModel nodeModel = selectedNode.getModel();

		// 基于当前结点位置加上偏移
		double siblingX = nodeModel.getX();
		double siblingY = nodeModel.getY() + selectedNode.getLayoutBounds().getHeight() + SizeConstants.NODE_GAP_Y;

		NodeModel siblingModel = new NodeModel(nextId(), "", siblingX, siblingY);
		NodeModel parentModel = nodeModel.getParent();
		parentModel.addChild(siblingModel);
		addNode(siblingModel);
		double midY = parentModel.getMidY();
		double height = selectedNode.getHeight();
		double y = midY - height / 2;
		parentModel.setY(y);
		refreshLines();
	}

	public void delete() {
		NodeModel toDelete = selectedNode.getModel();
		if (toDelete == rootModel) {
			return;
		}

		// 递归删除所有子结点
		deleteNodeAndChildren(toDelete);
		// 从父结点中移除
		NodeModel parent = toDelete.getParent();
		if (parent != null) {
			parent.removeChild(toDelete);
		}
		removeNode(toDelete);
		selectedNode = null;
		refreshLines();
	}

	public void refreshLines() {
		Pane linesLayer = mindPane.getLinesLayer();
		linesLayer.getChildren().clear();

		// 找到每一个子结点的父结点，创建连接线
		for (MindNode childNode : nodeMap.values()) {
			NodeModel parentModel = childNode.getModel().getParent();

			if (parentModel != null) {
				MindNode parentNode = parentModel.getMindNode();
				if (parentNode != null) {
					Point2D start = getRightPoint(parentNode);
					Point2D end = getLeftPoint(childNode);

					Line line = new Line(start.getX(), start.getY(), end.getX(), end.getY());
					line.setStroke(Color.rgb(100, 100, 100));
					line.setStrokeWidth(2.5);
					line.setStrokeDashOffset(0);

					linesLayer.getChildren().add(line);
				}
			}
		}
	}

	// 保存为 JSON 文件 （简易格式）
	public void saveToFile(File file) {
		List<NodeModel> allNodes = new ArrayList<>(nodeMap.keySet().size());
		for (MindNode view : nodeMap.values()) {
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
			showAlert("成功", "思维导图已保存到 " + file.getName());
		} catch (IOException e) {
			showAlert("错误", "保存失败：" + e.getMessage());
		}
	}

	// 加载 JSON 文件并重建界面
	public void loadFromFile(File file) {
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			StringBuilder content = new StringBuilder();
			String line;
			while ((line = br.readLine()) != null) content.append(line);
			String json = content.toString();
			// 简易解析 （生产环境建议用 JSON 库，这里手动解析足够演示）
			Map<Integer, NodeModel> loadedModels = new HashMap<>();
			Map<Integer, Integer> parentRelations = new HashMap<>();
			// 提取结点数组
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
				NodeModel model = new NodeModel(id, text, x, y);
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
					if (parent != null) parent.addChild(entry.getValue());
				}
			}
			if (newRoot == null) throw new RuntimeException("没有根结点");
			// 替换当前所有内容
			clearAll();
			rootModel = newRoot;
			// 重建视图
			rebuildViewFromModel(rootModel);
			refreshLines();
			showAlert("成功", "加载完成");
		} catch (Exception e) {
			e.printStackTrace();
			showAlert("错误", "加载失败：" + e.getMessage());
		}
	}

	//———————————————————————————————————————————私有方法———————————————————————————————————————————
	private void addNode(NodeModel model) {
		MindNode node = new MindNode(model, this);
		nodeMap.put(model.getId(), node);
		mindPane.getNodesLayer().getChildren().add(node);
		setSelectedNode(node);

		// 强制刷新布局，确保尺寸计算正确，否则node.getHeight()返回0
		mindPane.applyCss();
		mindPane.layout();
	}

	private void removeNode(NodeModel model) {
		MindNode node = nodeMap.remove(model.getId());
		if (node != null) {
			mindPane.getNodesLayer().getChildren().remove(node);
		}
	}

	private void deleteNodeAndChildren(NodeModel node) {
		for (NodeModel child : new ArrayList<>(node.getChildren())) {
			deleteNodeAndChildren(child);
			removeNode(child);
		}
		node.getChildren().clear();
	}

	private Point2D getRightPoint(MindNode node) {
		double x = node.getLayoutX() + node.getWidth();
		double y = node.getLayoutY() + node.getHeight() / 2;
		return new Point2D(x, y);
	}

	private Point2D getLeftPoint(MindNode node) {
		double x = node.getLayoutX();
		double y = node.getLayoutY() + node.getHeight() / 2;
		return new Point2D(x, y);
	}

	private int nextId() {
		return idGenerator.getAndIncrement();
	}

	private void clearAll() {
		mindPane.getNodesLayer().getChildren().clear();
		mindPane.getLinesLayer().getChildren().clear();
		nodeMap.clear();
		selectedNode = null;
	}

	private void rebuildViewFromModel(NodeModel node) {
		addNode(node);
		for (NodeModel child : node.getChildren()) {
			rebuildViewFromModel(child);
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

	private void showAlert(String title, String msg) {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(msg);
		alert.showAndWait();
	}
}