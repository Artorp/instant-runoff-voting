package no.artorp.instantrunoffvoting.json_models;

import java.util.List;

/**
 * A list of candidates, sent from server to client
 * 
 * Is sent to all clients the first time a candidate list is created
 * If a candidate list has been created, send to all new clients
 * If the candidate list is updated, send updated list to all clients
 * 
 * A client that receives the new client list must assume the new list is the valid one
 */
public class CandidatesPackage extends JsonPackage {
	private List<String> candidates;
	
	public CandidatesPackage(List<String> candidates) {
		messageType = MessageType.CANDIDATES;
		this.candidates = candidates;
	}

	public List<String> getCandidates() {
		return candidates;
	}
}
