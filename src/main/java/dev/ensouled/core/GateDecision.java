package dev.ensouled.core;

/** A gate verdict is a predicate result, never a score that utility could outbid (§5.5). */
public record GateDecision(Verdict verdict, String reasonCode, String detail, String actionHash) {

	public enum Verdict {
		PERMIT,
		REFUSE,
		HOLD
	}

	public static GateDecision permit(String actionHash) {
		return new GateDecision(Verdict.PERMIT, "permit", "within declared scope", actionHash);
	}

	public static GateDecision refuse(String reasonCode, String detail, String actionHash) {
		return new GateDecision(Verdict.REFUSE, reasonCode, detail, actionHash);
	}

	public static GateDecision hold(String reasonCode, String detail, String actionHash) {
		return new GateDecision(Verdict.HOLD, reasonCode, detail, actionHash);
	}

	public boolean permitted() {
		return verdict == Verdict.PERMIT;
	}
}
