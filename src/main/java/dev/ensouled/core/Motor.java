package dev.ensouled.core;

/** One body, one active motor program at a time. */
public interface Motor {
	/** Start a bounded motor program. Fails if a program is already active. */
	void start(MotorCommand command);

	/** Poll the active program, or return a cancelled result when idle. */
	MotorResult poll();

	void cancel();

	boolean busy();
}
