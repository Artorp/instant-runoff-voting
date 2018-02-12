package no.artorp.instantrunoffvoting.json_models;

/**
 * Sent from server to client once client is connected
 */
public class WelcomeMessage extends JsonPackage {
	private String serverName;
	private String clientName;

	public WelcomeMessage(String serverName, String clientName) {
		messageType = MessageType.WELCOME;
		this.serverName = serverName;
		this.clientName = clientName;
	}

	public String getServerName() {
		return serverName;
	}

	public String getClientName() {
		return clientName;
	}
}
