package no.artorp.instantrunoffvoting.client;

import java.io.IOException;
import java.net.Socket;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import javafx.util.Duration;
import no.artorp.instantrunoffvoting.Alerter;
import no.artorp.instantrunoffvoting.globals.NetworkDefaults;
import no.artorp.instantrunoffvoting.globals.ResourceLocations;
import no.artorp.instantrunoffvoting.utils.TimedTask;

public class JoinServerController {
	
	Stage myStage;
	
	@FXML TextField textfieldIp;
	@FXML TextField textfieldPort;
	@FXML Button buttonConnect;

	public JoinServerController(Stage stage) {
		this.myStage = stage;
	}

	@FXML
	public void initialize() {
		System.out.println("JoinServerController");
		
		textfieldPort.setText(NetworkDefaults.PORT);
		textfieldPort.setOnKeyPressed(ke -> {
			if (ke.getCode() == KeyCode.ENTER) {
				buttonConnect.fire();
			}
		});
		
		// bind "Connect..." button to valid ip
		buttonConnect.setDisable(true);
		textfieldIp.textProperty().addListener((observable, oldvalue, newvalue) -> {
			buttonConnect.setDisable(newvalue == null || "".equals(newvalue));
		});
		textfieldIp.setOnKeyPressed(ke -> {
			if (ke.getCode() == KeyCode.ENTER) {
				buttonConnect.fire();
			}
		});
		
		// connect action
		buttonConnect.setOnAction(ae -> {
			System.out.println("Connecting to " + getReadableIp());
			
			buttonConnect.setDisable(true);
			// Validate ip and port
			String ip = textfieldIp.getText().trim();
			String port = textfieldPort.getText().trim();
			int i_port;
			try {
				i_port = Integer.valueOf(port);
				if (i_port < 0 || i_port > 65535)
					throw new NullPointerException("Number outside range 0..65535");
			} catch (NumberFormatException e) {
				Alerter.showAlertError("Port must be an integer in range 0..65535");
				TimedTask.runLater(Duration.millis(500), () -> buttonConnect.setDisable(false));
				return;
			}
			// IP and port validated
			Socket server;
			try {
				server = new Socket(ip, i_port);
			} catch (IOException e) {
				e.printStackTrace();
				Alerter.showAlertError("Couldn't connect to server");
				TimedTask.runLater(Duration.millis(500), () -> buttonConnect.setDisable(false));
				return;
			}
			ClientSocketHandler socketHandler = new ClientSocketHandler(server);
			
			// Go to client candidate selection "lobby"
			FXMLLoader loader = new FXMLLoader(getClass().getResource(ResourceLocations.FXML_CLIENT_LOBBY));
			ClientLobbyController controller = new ClientLobbyController(socketHandler);
			loader.setController(controller);
			Parent root;
			try {
				root = loader.load();
			} catch (IOException e) {
				e.printStackTrace();
				try {
					server.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				Alerter.showAlertError("Error occured while loading the Lobby UI");
				return;
			}
			Scene s = new Scene(root);
			myStage.setScene(s);
		});
	}
	
	private String getReadableIp() {
		String ip = textfieldIp.getText();
		String port = textfieldPort.getText();
		StringBuilder sb = new StringBuilder(ip.length() + 1 + port.length());
		sb.append(ip).append(':').append(port);
		return sb.toString();
	}
}
