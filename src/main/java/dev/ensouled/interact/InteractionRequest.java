package dev.ensouled.interact;

/**
 * The operation a being asks of a perceived world object, scoped to one transfer id so the result can
 * be reconciled against the container and inventory deltas instead of trusted.
 */
public record InteractionRequest(String operation, String itemId, int count, String transferId) {
	public static final String INSPECT = "inspect";
	public static final String WITHDRAW = "withdraw";
	public static final String DEPOSIT = "deposit";

	public static InteractionRequest inspect(String itemId, String transferId) {
		return new InteractionRequest(INSPECT, itemId, 0, transferId);
	}

	public static InteractionRequest withdraw(String itemId, int count, String transferId) {
		return new InteractionRequest(WITHDRAW, itemId, count, transferId);
	}

	public static InteractionRequest deposit(String itemId, int count, String transferId) {
		return new InteractionRequest(DEPOSIT, itemId, count, transferId);
	}

	public String describe() {
		return operation + "(" + itemId + (count > 0 ? " x" + count : "") + " transfer=" + transferId + ")";
	}
}
