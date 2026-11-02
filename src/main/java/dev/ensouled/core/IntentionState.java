package dev.ensouled.core;

public enum IntentionState {
	INACTIVE,
	ACTIVATED,
	COMMITTED,
	EXECUTING,
	VERIFYING,
	SATISFIED,
	FAILED,
	INTERRUPTED,
	BLOCKED;

	public boolean terminal() {
		return this == SATISFIED || this == FAILED || this == INTERRUPTED || this == BLOCKED;
	}

	public boolean active() {
		return this == ACTIVATED || this == COMMITTED || this == EXECUTING || this == VERIFYING;
	}
}
