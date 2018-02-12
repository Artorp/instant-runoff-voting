package no.artorp.instantrunoffvoting.utils;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

public class TimedTask {
	/**
	 * Executes a task once after a preset delay
	 * 
	 * @param delay
	 *            time to wait before executing the task
	 * @param task
	 *            a task to be executed
	 */
	public static void runLater(Duration delay, Action task) {
		KeyFrame kf = new KeyFrame(delay, ae -> task.doAction());
		Timeline timeline = new Timeline(kf);
		timeline.play();
	}

	/**
	 * Executes a task indefinitely with a period of <b>delay</b>. Returns the
	 * {@link Timeline} that executes the task. Stop the task by calling
	 * {@link Timeline#stop()}. Timeline is asynchronous, the task may not be
	 * stopped immediately.
	 * 
	 * @param period time between each execution
	 * @param task the task to be executed
	 * @return the {@link Timeline} that executes the task
	 */
	public static Timeline runCyclic(Duration period, Action task) {
		KeyFrame kf = new KeyFrame(period, ae -> task.doAction());
		Timeline timeline = new Timeline(kf);
		timeline.setCycleCount(Animation.INDEFINITE);
		timeline.play();
		return timeline;
	}
}
