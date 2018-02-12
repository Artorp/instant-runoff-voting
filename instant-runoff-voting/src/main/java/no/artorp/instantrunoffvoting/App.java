package no.artorp.instantrunoffvoting;

import static no.artorp.instantrunoffvoting.globals.ResourceLocations.FXML_MAIN_MENU;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import no.artorp.instantrunoffvoting.globals.Threads;

public class App extends Application {
	public static void main_proxy(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		FXMLLoader loader = new FXMLLoader(getClass().getResource(FXML_MAIN_MENU));
		MainMenuController controller = new MainMenuController(primaryStage);
		loader.setController(controller);
		Parent root = loader.load();
		Scene s = new Scene(root);
		primaryStage.setScene(s);
		primaryStage.show();
	}

	@Override
	public void stop() throws Exception {
		System.out.println("Interrupting all threads");
		long patience = 1000;
		for (Thread t : Threads.THREADS) {
			t.interrupt();
		}
		long startWait = System.currentTimeMillis();
		for (Thread t : Threads.THREADS) {
			long timeToWait = Math.max(0, startWait - System.currentTimeMillis() + patience);
			t.join(timeToWait);
		}
		System.out.println("Exiting...");
	}
}
