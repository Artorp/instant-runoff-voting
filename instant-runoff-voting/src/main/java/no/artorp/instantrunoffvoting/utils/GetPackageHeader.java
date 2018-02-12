package no.artorp.instantrunoffvoting.utils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;

import no.artorp.instantrunoffvoting.json_models.HeaderOnly;
import no.artorp.instantrunoffvoting.json_models.MessageType;

public class GetPackageHeader {
	/**
	 * Retrieves the package type as {@link MessageType} from a JSON string
	 * 
	 * @param json
	 *            the json string to be parsed
	 * @return the message type, <code>null</code> if none applicable
	 */
	public static MessageType getPackageHeader(JsonElement jsonElement) {
		Gson gson = new Gson();
		HeaderOnly headerOnly;
		try {
			headerOnly = gson.fromJson(jsonElement, HeaderOnly.class);
		} catch (JsonSyntaxException e) {
			e.printStackTrace();
			return null;
		}
		MessageType mt = headerOnly.getMessageType();
		return mt;
	}
}
