package no.artorp.instantrunoffvoting;

/**
 * This is the program entry point
 * 
 * This class does not depend on JavaFX. Certain Linux systems does not come
 * with JavaFX by default, by using a "proxy" main class those systems will give
 * a sensible error message
 */
public class Main {
	
	public static void main(String[] args) {
		App.main_proxy(args);
	}

}
