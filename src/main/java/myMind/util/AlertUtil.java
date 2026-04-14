package myMind.util;

import javafx.scene.control.Alert;

public class AlertUtil {
	public static void showAlert(String title, String msg) {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(msg);
		alert.showAndWait();
	}
}
