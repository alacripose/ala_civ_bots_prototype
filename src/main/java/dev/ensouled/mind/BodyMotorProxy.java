package dev.ensouled.mind;

import dev.ensouled.core.Motor;
import dev.ensouled.core.MotorCommand;
import dev.ensouled.core.MotorResult;

/**
 * The mind outlives any particular entity instance; this proxy lets the manager re-bind a reloaded
 * body without rebuilding the mind, and reports an explicit failure when no body is available.
 */
public final class BodyMotorProxy implements Motor {
	private Motor delegate;

	public void attach(Motor motor) {
		if (delegate != null && delegate != motor) {
			delegate.cancel();
		}
		delegate = motor;
	}

	public void detach() {
		if (delegate != null) {
			delegate.cancel();
			delegate = null;
		}
	}

	public boolean attached() {
		return delegate != null;
	}

	@Override
	public void start(MotorCommand command) {
		if (delegate == null) {
			throw new IllegalStateException("no body attached");
		}
		delegate.start(command);
	}

	@Override
	public MotorResult poll() {
		if (delegate == null) {
			return MotorResult.failed("no_body", "no body is attached to this mind");
		}
		return delegate.poll();
	}

	@Override
	public void cancel() {
		if (delegate != null) {
			delegate.cancel();
		}
	}

	@Override
	public boolean busy() {
		return delegate != null && delegate.busy();
	}
}
