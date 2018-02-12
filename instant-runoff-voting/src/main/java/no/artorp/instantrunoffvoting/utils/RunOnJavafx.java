package no.artorp.instantrunoffvoting.utils;

import javafx.application.Platform;

public class RunOnJavafx {
	public static void run(Runnable r) {
		if (Platform.isFxApplicationThread())
			r.run();
		else
			Platform.runLater(r);
	}
}
