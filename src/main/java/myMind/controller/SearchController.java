package myMind.controller;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import lombok.Setter;
import myMind.componet.MapNode;
import myMind.componet.MapTextArea;
import myMind.componet.MindMap;
import myMind.componet.Subject;
import org.fxmisc.richtext.StyleClassedTextArea;

import java.util.List;
import java.util.Locale;

public class SearchController {

    @FXML
    private VBox searchPanel;
    @FXML
    public TextField searchTextField;
    @FXML
    private Label statusLabel;
    @FXML
    public ScrollPane resultListScrollPane;
    @FXML
    private VBox resultList;

    @Setter
    private MindMap mindMap;

    @FXML
    public void showAndHide() {
        if(searchPanel.isVisible()){
            searchPanel.setVisible(false);
        }else {
            searchPanel.setVisible(true);
            searchTextField.requestFocus();
        }
    }

    @FXML
    private void searchTextFieldPressed(KeyEvent event) {
        if(event.getCode() == KeyCode.ENTER){
            search();
        }
    }

    @FXML
    private void search() {
        ObservableList<Node> children = resultList.getChildren();
        children.clear();
        // 临时禁用滚动条，避免它闪现
        resultListScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        String searchText = searchTextField.getText().toLowerCase(Locale.ROOT);
        if(searchText.isEmpty()){
            statusLabel.setVisible(false);
            return;
        }
        for (Tab tab : mindMap.getTabs()) {
            Subject subject = (Subject) tab.getContent();
            for (Node child : subject.getNodesLayer().getChildren()) {
                MapNode mapNode = (MapNode) child;
                StyleClassedTextArea nodeTextArea = mapNode.getTextArea();
                String nodeText = nodeTextArea.getText();
                String nodeTextLowerCase = nodeText.toLowerCase(Locale.ROOT);
                int index = nodeTextLowerCase.indexOf(searchText);
                if (index == -1) {
                    continue;
                }

                MapTextArea textArea = new MapTextArea();
                textArea.replaceText(nodeText);
                textArea.setEditable(false);
                textArea.setStyle(index, index + searchText.length(), List.of("high-light-text"));

                textArea.setOnMouseClicked(event -> {
                    mindMap.jumpToSubject(subject.getSubjectId());
                    SubjectController subjectController = (SubjectController) tab.getUserData();
                    subjectController.toCenter(mapNode);
                    subjectController.setSelectedNode(mapNode);
                });
                children.add(textArea);
                children.add(new Separator());
            }
        }
        if(!children.isEmpty()){
            children.remove(children.size() - 1);
        }
        Platform.runLater(() -> resultListScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED));

        statusLabel.setText("共 " + children.size() + " 个结果");
        statusLabel.setVisible(true);
    }
}
