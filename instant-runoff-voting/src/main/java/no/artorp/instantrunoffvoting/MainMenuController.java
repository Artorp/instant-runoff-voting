package no.artorp.instantrunoffvoting;

import java.io.IOException;
import java.net.URL;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import no.artorp.instantrunoffvoting.client.JoinServerController;
import no.artorp.instantrunoffvoting.globals.ResourceLocations;
import no.artorp.instantrunoffvoting.server.HostServerController;

public class MainMenuController {
	
	Stage myStage;
	Scene clientScene = null;
	Scene serverScene = null;
	
	@FXML
	Button buttonHost;
	
	@FXML
	Button buttonJoin;

	public MainMenuController(Stage stage) {
		this.myStage = stage;
	}
	
	@FXML
	public void initialize() {
		System.out.println("MainMenuController");
		
		buttonHost.setOnKeyPressed(ke -> {
			if (ke.getCode() == KeyCode.ENTER) {
				buttonHost.fire();
			}
		});
		buttonHost.setOnAction(ae -> {
			System.out.println("Hosting a server...");
			try {
				setupHostScene();
				myStage.setScene(serverScene);
			} catch (IOException e) {
				e.printStackTrace();
				Alerter.showAlertError(e.getMessage());
			}
		});
		
		buttonJoin.setOnKeyPressed(ke -> {
			if (ke.getCode() == KeyCode.ENTER) {
				buttonJoin.fire();
			}
		});
		buttonJoin.setOnAction(ae -> {
			System.out.println("Joining a server...");
			try {
				setupJoinScene();
				myStage.setScene(clientScene);
			} catch (IOException e) {
				e.printStackTrace();
				Alerter.showAlertError(e.getMessage());
			}
		});
	}
	
	private void setupJoinScene() throws IOException {
		if (clientScene == null) {
			URL location = getClass().getResource(ResourceLocations.FXML_JOIN_SERVER);
			FXMLLoader loader = new FXMLLoader(location);
			JoinServerController controller = new JoinServerController(myStage);
			loader.setController(controller);
			Parent root = loader.load();
			this.clientScene = new Scene(root);
		}
	}
	
	private void setupHostScene() throws IOException {
		if (serverScene == null) {
			URL location = getClass().getResource(ResourceLocations.FXML_HOST_SERVER);
			FXMLLoader loader = new FXMLLoader(location);
			HostServerController controller = new HostServerController(myStage);
			loader.setController(controller);
			Parent root = loader.load();
			this.serverScene = new Scene(root);
		}
	}

}
