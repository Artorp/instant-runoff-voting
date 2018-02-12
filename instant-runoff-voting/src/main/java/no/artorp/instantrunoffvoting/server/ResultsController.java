package no.artorp.instantrunoffvoting.server;

import java.util.List;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import no.artorp.instantrunoffvoting.votinglogic.VotingSimulator;

public class ResultsController {
	
	private VotingSimulator votingSim;
	
	@FXML
	private Label labelRounds;
	@FXML
	private TextArea textareaResults;
	@FXML
	private Button buttonPrev;
	@FXML
	private Button buttonNext;
	@FXML
	private Label labelPageLegend;
	
	// Strings
	private final static String ROUND_N = "Round #%s";
	private final static String PAGE_LEGEND = "%s / %s";
	
	private IntegerProperty pageIndex = new SimpleIntegerProperty(0);
	
	private List<String> rounds;
	
	public ResultsController(List<String> candidates, List<List<String>> ballots) {
		votingSim = new VotingSimulator(candidates, ballots);
	}
	
	public void initialize() {
		System.out.println("ResultsController");
		votingSim.runSimulation();
		rounds = votingSim.getRounds();
		textareaResults.setText(rounds.get(0));
		
		buttonPrev.setDisable(true);
		buttonPrev.setOnAction(ae -> {
			pageIndex.set(pageIndex.get() - 1);
		});
		buttonNext.setDisable(rounds.size() == 1);
		buttonNext.setOnAction(ae -> {
			pageIndex.set(pageIndex.get() + 1);
		});
		
		// Handle page turn
		pageIndex.addListener((obs, oldvalue, newvalue) -> {
			int newv = newvalue.intValue();
			buttonPrev.setDisable(newv == 0);
			buttonNext.setDisable(newv == rounds.size() - 1);
			textareaResults.setText(rounds.get(newv));
			// update labels
			labelRounds.setText(String.format(ROUND_N, newv + 1));
			labelPageLegend.setText(String.format(PAGE_LEGEND, newv + 1, rounds.size()));
		});
		labelPageLegend.setText(String.format(PAGE_LEGEND, 1, rounds.size()));
	}

}
