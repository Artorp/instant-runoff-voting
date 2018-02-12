package no.artorp.instantrunoffvoting.json_models;

import java.util.List;

/**
 * An ordered list of candidates, sent from client to server.
 * 
 * A client may choose to send multiple ballots for as long as the voting isn't
 * locked, only the latest ballot is valid.
 * 
 * 
 */
public class CandidatesBallot extends JsonPackage {
	private List<String> candidatesOrdered;
	
	public CandidatesBallot(List<String> candidates) {
		messageType = MessageType.BALLOT;
		candidatesOrdered = candidates;
	}
	
	public List<String> getCandidatesOrdered() {
		return candidatesOrdered;
	}
}
