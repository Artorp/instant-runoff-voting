package no.artorp.instantrunoffvoting.json_models;

/**
 * Updates the "lock" flag for voting
 * 
 * Sent from server to clients
 */
public class VotingLocked extends JsonPackage {
	private boolean locked;
	
	public VotingLocked(boolean locked) {
		messageType = MessageType.LOCKED;
		this.locked = locked;
	}
	
	public boolean isLocked() {
		return locked;
	}
}
