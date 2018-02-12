package no.artorp.instantrunoffvoting.server;

import java.io.IOException;
import java.net.ServerSocket;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import no.artorp.instantrunoffvoting.Alerter;
import no.artorp.instantrunoffvoting.globals.NetworkDefaults;
import no.artorp.instantrunoffvoting.globals.ResourceLocations;

public class HostServerController {

	Stage myStage;

	// FXML injected fields
	@FXML TextField textfieldName;
	@FXML TextField textfieldPort;
	@FXML CheckBox checkboxAutoPort;
	@FXML Button buttonHost;
	@FXML Label labelHost;
	

	public HostServerController(Stage stage) {
		this.myStage = stage;
	}

	@FXML
	public void initialize() {
		System.out.println("HostServerController");

		textfieldName.setText("The best server");
		textfieldPort.setText(NetworkDefaults.PORT);
		checkboxAutoPort.selectedProperty().addListener((obs, oldv, newv) -> {
			textfieldPort.setDisable(newv);
		});
		
		textfieldName.setOnKeyPressed(ke -> {
			if (ke.getCode() == KeyCode.ENTER) {
				buttonHost.fire();
			}
		});
		textfieldPort.setOnKeyPressed(ke -> {
			if (ke.getCode() == KeyCode.ENTER) {
				buttonHost.fire();
			}
		});

		buttonHost.setOnAction(ae -> {
			// Disable options while waiting for connection to be established
			disableGui();
			updateLabel("Creating server socket...", false);
			
			// Get data
			String name = textfieldName.getText();
			String port = textfieldPort.getText();
			if (checkboxAutoPort.isSelected()) {
				port = null;
			}
			
			this.establishConnection(name, port);
			
		});
	}
	
	private void disableGui() {
		textfieldName.setDisable(true);
		textfieldPort.setDisable(true);
		checkboxAutoPort.setDisable(true);
		buttonHost.setDisable(true);
	}
	
	private void enableGui() {
		textfieldName.setDisable(false);
		textfieldPort.setDisable(checkboxAutoPort.isSelected());
		checkboxAutoPort.setDisable(false);
		buttonHost.setDisable(false);
	}
	
	private void updateLabel(String s, boolean error) {
		labelHost.setStyle(error ? "-fx-text-fill:red;" : "");
		labelHost.setText(s);
	}
	
	private void establishConnection(String serverName, String port) {
		int i_port;
		if (port == null) {
			i_port = 0;
		} else {
			try {
				i_port = Integer.valueOf(port);
			} catch (NumberFormatException e) {
				enableGui();
				updateLabel("Port must be integer in range 0..65535", true);
				return;
			}
			if (i_port < 0 || i_port > 65535) {
				enableGui();
				updateLabel("Port must be integer in range 0..65535", true);
				return;
			}
		}
		ServerSocket serverSocket;
		try {
			serverSocket = new ServerSocket(i_port);
			serverSocket.setSoTimeout(1000);
		} catch (IOException e) {
			e.printStackTrace();
			enableGui();
			updateLabel("Error when creating socket.", true);
			return;
		}
		
		// Serversocket successfully created, go to next window
		HostLobbyController controller = new HostLobbyController(myStage, serverSocket, serverName);
		FXMLLoader loader = new FXMLLoader(getClass().getResource(ResourceLocations.FXML_HOST_LOBBY));
		loader.setController(controller);
		Parent root;
		try {
			root = loader.load();
		} catch (IOException e) {
			e.printStackTrace();
			Alerter.showAlertError("Error encountered while creating the lobby UI");
			return;
		}
		Scene s = new Scene(root);
		myStage.setScene(s);
		myStage.centerOnScreen();
	}

}
