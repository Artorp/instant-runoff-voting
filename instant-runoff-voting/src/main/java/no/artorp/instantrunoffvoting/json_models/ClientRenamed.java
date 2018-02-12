package no.artorp.instantrunoffvoting.json_models;

/**
 * A message sent to the client, client has been renamed.
 * 
 * Might let client rename themselves in a later version?
 */
public class ClientRenamed extends JsonPackage {
	private String newName;
	
	public ClientRenamed(String newName) {
		messageType = MessageType.RENAMED;
		this.newName = newName;
	}
	
	public String getNewName() {
		return newName;
	}
}
