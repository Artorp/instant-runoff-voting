package no.artorp.instantrunoffvoting.json_models;

public class WasRenamed extends JsonPackage {
	
	private String newName;

	public WasRenamed(String newName) {
		messageType = MessageType.RENAMED;
		this.newName = newName;
	}

	public String getNewName() {
		return newName;
	}

}
