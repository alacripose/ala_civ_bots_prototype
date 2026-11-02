package dev.ensouled.core;

/**
 * Owns the bounded motor command currently in flight for one intention. At most one motor program
 * is active; a routine must wait for the command to finish, then consume its result explicitly.
 */
public final class SkillRuntime {
	private final Motor motor;
	private int step;
	private boolean awaiting;
	private boolean issuedThisStep;
	private MotorResult last = MotorResult.success("none", "no command issued", java.util.Map.of());
	private MotorResult justFinished;

	public SkillRuntime(Motor motor) {
		this.motor = motor;
	}

	public int step() {
		return step;
	}

	/** Advance to the next declared step and forget the previous command result. */
	public void advance() {
		step++;
		issuedThisStep = false;
	}

	public void reset() {
		step = 0;
		awaiting = false;
		issuedThisStep = false;
		last = MotorResult.success("none", "no command issued", java.util.Map.of());
		motor.cancel();
	}

	public boolean issue(MotorCommand command) {
		if (motor.busy()) {
			return false;
		}
		motor.start(command);
		awaiting = true;
		issuedThisStep = true;
		return true;
	}

	public void poll() {
		if (!awaiting) {
			return;
		}
		MotorResult result = motor.poll();
		if (result.terminal()) {
			awaiting = false;
			last = result;
			justFinished = result;
		}
	}

	public boolean awaiting() {
		return awaiting;
	}

	/** True once this step has issued a command (whether or not it has finished). */
	public boolean issuedThisStep() {
		return issuedThisStep;
	}

	/** Consume the finished command's result without leaving the current step. */
	public void clearResult() {
		issuedThisStep = false;
	}

	/** Returns the result that finished since the last call, then forgets it. */
	public MotorResult takeJustFinished() {
		MotorResult result = justFinished;
		justFinished = null;
		return result;
	}

	public MotorResult last() {
		return last;
	}

	public MotorStatus lastStatus() {
		return last.status();
	}

	public boolean lastSucceeded() {
		return last.status() == MotorStatus.SUCCESS;
	}

	public boolean lastFailed() {
		return last.status() == MotorStatus.FAILED || last.status() == MotorStatus.CANCELLED;
	}

	public void cancel() {
		motor.cancel();
		awaiting = false;
	}
}
