package myMind.controller;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.stage.FileChooser;

import java.io.File;

public class MenuBarController {
	
	private final NodeController nodeController;
	private final FileController fileController;
	
	public MenuBarController(NodeController nodeController, FileController fileController) {
		this.nodeController = nodeController;
		this.fileController = fileController;
	}
	
	public MenuBar createMenuBar() {
		MenuBar menuBar = new MenuBar();
		Menu fileMenu = new Menu("文件");
		Menu editMenu = new Menu("编辑");
		
		MenuItem addItem = new MenuItem("添加子结点");
		MenuItem delItem = new MenuItem("删除结点");
		MenuItem saveItem = new MenuItem("保存");
		MenuItem loadItem = new MenuItem("加载");
		
		fileMenu.getItems().addAll(saveItem, loadItem);
		editMenu.getItems().addAll(addItem, delItem);
		menuBar.getMenus().addAll(fileMenu, editMenu);
		
		addItem.setOnAction(e -> nodeController.addChild());
		delItem.setOnAction(e -> nodeController.delete());
		saveItem.setOnAction(e -> handleSave());
		loadItem.setOnAction(e -> handleLoad());
		
		return menuBar;
	}
	
	private void handleSave() {
		FileChooser fc = new FileChooser();
		fc.setInitialFileName("mindmap.json");
		fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON 文件", "*.json"));
		File file = fc.showSaveDialog(nodeController.getMindMapPane().getScene().getWindow());
		if (file != null) {
			fileController.saveToFile(file);
		}
	}
	
	private void handleLoad() {
		FileChooser fc = new FileChooser();
		fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON 文件", "*.json"));
		File file = fc.showOpenDialog(nodeController.getMindMapPane().getScene().getWindow());
		if (file != null) {
			fileController.loadFromFile(file);
		}
	}
}
