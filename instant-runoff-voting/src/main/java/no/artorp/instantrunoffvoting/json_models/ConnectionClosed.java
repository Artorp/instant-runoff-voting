package no.artorp.instantrunoffvoting.json_models;

/**
 * Indicate to the other connection that we are closing the connection in an orderly fashion.
 * 
 * Can be sent both from server to client, and client to server.
 */
public class ConnectionClosed extends JsonPackage {
	public ConnectionClosed() {
		messageType = MessageType.CLOSED;
	}
}
