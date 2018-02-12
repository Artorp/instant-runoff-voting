package no.artorp.instantrunoffvoting;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class Alerter {

	/**
	 * Create an {@link Alert} and show it. Will add to
	 * {@link Platform#runLater(Runnable)} if not on JavaFX thread.
	 * 
	 * @param message
	 *            the content of the alert
	 */
	public static void showAlertError(String message) {
		Alert alert = new Alert(AlertType.ERROR);
		alert.setTitle("An error has occured");
		alert.setHeaderText(null);
		alert.setContentText(message);
		if (Platform.isFxApplicationThread()) {
			alert.showAndWait();
		} else {
			Platform.runLater(() -> alert.showAndWait());
		}
	}
}
