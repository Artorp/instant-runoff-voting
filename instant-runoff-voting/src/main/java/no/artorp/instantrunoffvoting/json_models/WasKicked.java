package no.artorp.instantrunoffvoting.json_models;

/**
 * Notice to client that he was kicked (on purpose).
 * 
 * Sent from server to client, just before closing the socket.
 */
public class WasKicked extends JsonPackage {
	public WasKicked() {
		messageType = MessageType.KICKED;
	}
}
