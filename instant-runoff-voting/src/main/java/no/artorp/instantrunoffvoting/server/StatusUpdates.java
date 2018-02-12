package no.artorp.instantrunoffvoting.server;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class StatusUpdates {
	
	public static final SimpleDateFormat SDF = new SimpleDateFormat("HH:mm");
	private List<Update> updates = new ArrayList<>();
	
	public static class Update {
		private Date timestamp;
		String content;
		private boolean isConnection;
		
		public Update(String content, boolean isConnection) {
			this.content = content;
			this.timestamp = new Date();
			this.isConnection = isConnection;
		}

		public boolean isConnection() {
			return isConnection;
		}
		
		public StringBuilder append(StringBuilder sb) {
			sb.ensureCapacity(sb.length() + content.length() + 9);
			return sb.append('[').append(SDF.format(timestamp)).append("] ").append(content).append('\n');
		}
	}
	
	public void addUpdate(String content, boolean isConnection) {
		Update u = new Update(content, isConnection);
		updates.add(u);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Update u : updates) {
			u.append(sb);
		}
		return sb.toString();
	};
	
	public String connectionsOnly() {
		StringBuilder sb = new StringBuilder();
		for (Update u : updates) {
			if (u.isConnection())
				u.append(sb);
		}
		return sb.toString();
	}

}
