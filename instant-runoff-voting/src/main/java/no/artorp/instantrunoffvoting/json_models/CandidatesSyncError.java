package no.artorp.instantrunoffvoting.json_models;

import java.util.List;

public class CandidatesSyncError extends JsonPackage {
	private List<String> candidates;
	
	public CandidatesSyncError(List<String> candidates) {
		messageType = MessageType.SYNC_ERROR;
		this.candidates = candidates;
	}
	
	public List<String> getCandidates() {
		return candidates;
	}
}
