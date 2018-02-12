package no.artorp.instantrunoffvoting.server;

import java.util.List;

import com.google.gson.Gson;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import no.artorp.instantrunoffvoting.json_models.CandidatesPackage;
import no.artorp.instantrunoffvoting.json_models.CandidatesSyncError;
import no.artorp.instantrunoffvoting.json_models.VotingBegin;
import no.artorp.instantrunoffvoting.json_models.VotingLocked;
import no.artorp.instantrunoffvoting.json_models.VotingParticipation;
import no.artorp.instantrunoffvoting.json_models.WasKicked;
import no.artorp.instantrunoffvoting.json_models.WasRenamed;
import no.artorp.instantrunoffvoting.json_models.WelcomeMessage;

public class ObservableClient {
	
	enum State {
		WAITING_FOR_CANDIDATES("Waiting for candidates..."),
		RANKING_CANDIDATES("Currently ranking candidates"),
		HAS_SUBMITTED("Has submitted ballot");
		
		private String readableName;
		
		State(String name) {
			this.readableName = name;
		}
		
		@Override
		public String toString() {
			return this.readableName;
		}
	}
	
	private ClientThread client;
	
	private StringProperty name = new SimpleStringProperty();
	private ObjectProperty<State> state = new SimpleObjectProperty<>(State.WAITING_FOR_CANDIDATES);
	private ObjectProperty<List<String>> submittedBallot = new SimpleObjectProperty<List<String>>(null);
	private boolean receivedCandidates = false;

	public ObservableClient(ClientThread client) {
		this.client = client;
		this.name.setValue(client.getName());
	}
	
	
	public void sendWelcomeMessage(String serverName) {
		WelcomeMessage payload = new WelcomeMessage(serverName, client.getName());
		String json = new Gson().toJson(payload);
		System.out.println("About to send");
		client.sendMessage(json);
		System.out.println("Done sending");
	}
	
	public void sendCandidateList(List<String> candidateNames) {
		CandidatesPackage p = new CandidatesPackage(candidateNames);
		String json = new Gson().toJson(p);
		// TODO: Remove debug line
		System.out.println("Sending package to client ("+name.getValue()+"):\n" + json);
		client.sendMessage(json);
		receivedCandidates = true;
		state.setValue(State.RANKING_CANDIDATES);
	}
	
	public void sendResyncCandidateList(List<String> candidateNames) {
		CandidatesSyncError cse = new CandidatesSyncError(candidateNames);
		String json = new Gson().toJson(cse);
		client.sendMessage(json);
		receivedCandidates = true;
		state.setValue(State.RANKING_CANDIDATES);
	}
	
	public void sendMessage(String jsonString) {
		client.sendMessage(jsonString);
	}
	
	public void sendLocked(boolean locked) {
		VotingLocked vl = new VotingLocked(locked);
		String json = new Gson().toJson(vl);
		client.sendMessage(json);
	}
	
	public void sendVoteStartBallotCollected() {
		VotingBegin vb = new VotingBegin(VotingParticipation.BALLOT_COLLECTED);
		String json = new Gson().toJson(vb);
		client.sendMessage(json);
	}
	
	public void sendVoteStartNoBallotCollected() {
		VotingBegin vb = new VotingBegin(VotingParticipation.NO_BALLOT_COLLECTED);
		String json = new Gson().toJson(vb);
		client.sendMessage(json);
	}
	
	public void sendVoteStartInvalidBallot(List<String> candidates) {
		VotingBegin vb = new VotingBegin(VotingParticipation.INVALID_BALLOT, candidates);
		String json = new Gson().toJson(vb);
		client.sendMessage(json);
	}
	
	public void kick() {
		WasKicked wk = new WasKicked();
		String json = new Gson().toJson(wk);
		client.closeWithMessage(json);
	}
	
	public void rename(String newName) {
		WasRenamed wr = new WasRenamed(newName);
		String json = new Gson().toJson(wr);
		client.sendMessage(json);
		this.setName(newName);
	}
	
	public StringProperty nameProperty() {
		return this.name;
	}
	

	public String getName() {
		return this.nameProperty().get();
	}
	

	public void setName(final String name) {
		this.nameProperty().set(name);
	}
	

	public ObjectProperty<State> stateProperty() {
		return this.state;
	}
	

	public State getState() {
		return this.stateProperty().get();
	}
	

	public void setState(final State state) {
		this.stateProperty().set(state);
	}


	public boolean isReceivedCandidates() {
		return receivedCandidates;
	}


	public ObjectProperty<List<String>> submittedBallotProperty() {
		return this.submittedBallot;
	}
	


	public List<String> getSubmittedBallot() {
		return this.submittedBallotProperty().get();
	}


	public void setSubmittedBallot(final List<String> submittedBallot) {
		this.submittedBallotProperty().set(submittedBallot);
	}

}
