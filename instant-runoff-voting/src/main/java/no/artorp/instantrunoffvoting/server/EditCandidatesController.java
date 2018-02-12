package no.artorp.instantrunoffvoting.server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import no.artorp.instantrunoffvoting.Alerter;

public class EditCandidatesController {
	
	Stage myStage;
	
	@FXML TextArea textareaCandidates;
	@FXML Button buttonAssignNew;
	
	private List<String> candidates;
	private Iterator<String> previousCandidates;
	
	private boolean exitedSuccessfully = false;
	
	public EditCandidatesController(Stage stage, Iterator<String> candidates) {
		this.myStage = stage;
		this.previousCandidates = candidates;
	}

	public void initialize() {
		System.out.println("EditCandidatesController");
		
		StringBuilder sb = new StringBuilder();
		while (this.previousCandidates.hasNext()){
			sb.append(this.previousCandidates.next());
			sb.append('\n');
		}
		textareaCandidates.setText(sb.toString());
		
		buttonAssignNew.setOnAction(ae -> {
			// Create candidates from text
			candidates = new ArrayList<>();
			String[] names = textareaCandidates.getText().split("\\n");
			for (String name : names) {
				name = name.trim();
				if (name.equals("")) continue;
				if (candidates.contains(name)) {
					// Duplicate name
					Alerter.showAlertError("Duplicate candidate " + name);
					return;
				}
				candidates.add(name);
			}
			this.exitedSuccessfully = true;
			myStage.hide();
		});
	}

	/**
	 * A list of new candidates
	 * 
	 * @return the candidates
	 */
	public List<String> getCandidates() {
		return candidates;
	}

	/**
	 * Returns true if the user exited by saving, false otherwise
	 * 
	 * @return true if candidates were saved, false otherwise
	 */
	public boolean exitedSuccessfully() {
		return exitedSuccessfully;
	}
}
