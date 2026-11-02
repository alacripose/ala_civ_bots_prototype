package dev.ensouled.core;

/**
 * A structured communication record (Addendum C §C.6.2). A message is not a world fact and not a
 * grant of authority: it is one agent's speech act, delivered to one recipient, with its own
 * provenance and delivery state.
 */
public final class AgentMessage {
	public static final String REQUEST = "request";
	public static final String ACCEPT = "accept";
	public static final String DECLINE = "decline";

	private final String messageId;
	private final String senderId;
	private final String recipientId;
	private final String speechAct;
	private final String itemId;
	private final int count;
	private final String subject;
	private final String reason;
	private final long createdTick;
	private final long expiresTick;

	private String state = "delivered";

	AgentMessage(String messageId, String senderId, String recipientId, String speechAct, String itemId,
			int count, String subject, String reason, long createdTick, long expiresTick) {
		this.messageId = messageId;
		this.senderId = senderId;
		this.recipientId = recipientId;
		this.speechAct = speechAct;
		this.itemId = itemId;
		this.count = count;
		this.subject = subject;
		this.reason = reason;
		this.createdTick = createdTick;
		this.expiresTick = expiresTick;
	}

	public String messageId() {
		return messageId;
	}

	public String senderId() {
		return senderId;
	}

	public String recipientId() {
		return recipientId;
	}

	public String speechAct() {
		return speechAct;
	}

	public String itemId() {
		return itemId;
	}

	public int count() {
		return count;
	}

	/** The message this one is about (a request id), or the transfer it settles. */
	public String subject() {
		return subject;
	}

	public String reason() {
		return reason;
	}

	public long createdTick() {
		return createdTick;
	}

	public long expiresTick() {
		return expiresTick;
	}

	public String state() {
		return state;
	}

	void answer(String newState) {
		this.state = newState;
	}

	public boolean expired(long now) {
		return now > expiresTick;
	}

	@Override
	public String toString() {
		return speechAct + "(" + messageId + " " + senderId + "->" + recipientId + " " + itemId + "x" + count
				+ (reason.isEmpty() ? "" : " " + reason) + ")";
	}
}
