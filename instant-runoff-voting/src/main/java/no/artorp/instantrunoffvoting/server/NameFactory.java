package no.artorp.instantrunoffvoting.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import no.artorp.instantrunoffvoting.globals.ResourceLocations;

public class NameFactory {

	Set<String> availableNames = new HashSet<>();
	Random r = new Random();
	private int counter = 0;

	public NameFactory() {
		// Get names from resource
		try (InputStream is = getClass().getResourceAsStream(ResourceLocations.CLIENT_NAMES)) {
			BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
			String line;
			while ((line = br.readLine()) != null) {
				if (!line.trim().equals("")) {
					availableNames.add(line);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getNextName() {
		String name = "N/A";
		if (availableNames.size() > 0) {
			String[] choices = availableNames.toArray(new String[0]);
			name = choices[r.nextInt(choices.length)];
			availableNames.remove(name);
		} else {
			name = "Agent " + counter;
			counter++;
		}
		return name;
	}
}
