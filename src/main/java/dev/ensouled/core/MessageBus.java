package dev.ensouled.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Per-agent inboxes plus one audit log, shared by every mind in the world (Addendum C §C.6.2).
 * The bus owns message ids, delivery and expiry. It never decides anything: taking a request is an
 * agent's own act, and a delivered message is never a verified world fact.
 */
public final class MessageBus {
	public static final int DEFAULT_TTL_TICKS = 1200;

	private final Map<String, Deque<AgentMessage>> inboxes = new LinkedHashMap<>();
	private final List<AgentMessage> log = new ArrayList<>();
	private long sequence;
	/** Where a recorded speech act is announced; the world installs one, tests leave it absent. */
	private Consumer<AgentMessage> speech = message -> {
	};

	public AgentMessage send(String senderId, String recipientId, String speechAct, String itemId, int count,
			String subject, String reason, long tick, int ttlTicks) {
		AgentMessage message = new AgentMessage("msg:" + (++sequence), senderId, recipientId, speechAct,
				itemId, count, subject, reason, tick, tick + ttlTicks);
		inboxes.computeIfAbsent(recipientId, ignored -> new ArrayDeque<>()).addLast(message);
		log.add(message);
		speech.accept(message);
		return message;
	}

	/** Called for every recorded speech act, exactly once, in the order it was sent. */
	public void onSpeech(Consumer<AgentMessage> listener) {
		this.speech = listener;
	}

	public AgentMessage request(String senderId, String recipientId, String itemId, int count, String subject,
			long tick) {
		return send(senderId, recipientId, AgentMessage.REQUEST, itemId, count, subject, "", tick,
				DEFAULT_TTL_TICKS);
	}

	public AgentMessage accept(String senderId, String recipientId, AgentMessage request, long tick) {
		return send(senderId, recipientId, AgentMessage.ACCEPT, request.itemId(), request.count(),
				request.messageId(), "", tick, DEFAULT_TTL_TICKS);
	}

	public AgentMessage decline(String senderId, String recipientId, AgentMessage request, String reason,
			long tick) {
		return send(senderId, recipientId, AgentMessage.DECLINE, request.itemId(), request.count(),
				request.messageId(), reason, tick, DEFAULT_TTL_TICKS);
	}

	/** Oldest unexpired message waiting for this agent, or empty. */
	public Optional<AgentMessage> peek(String agentId, long now) {
		Deque<AgentMessage> inbox = inboxes.get(agentId);
		if (inbox == null) {
			return Optional.empty();
		}
		while (!inbox.isEmpty() && inbox.peekFirst().expired(now)) {
			inbox.removeFirst();
		}
		return Optional.ofNullable(inbox.peekFirst());
	}

	/** Remove and return the oldest unexpired message: taking a request is the recipient's act. */
	public Optional<AgentMessage> take(String agentId, long now) {
		Optional<AgentMessage> next = peek(agentId, now);
		if (next.isPresent()) {
			inboxes.get(agentId).removeFirst();
			next.get().answer("taken");
		}
		return next;
	}

	/** An unexpired message of this act about this subject, without consuming it. */
	public Optional<AgentMessage> find(String agentId, String speechAct, String subject, long now) {
		Deque<AgentMessage> inbox = inboxes.get(agentId);
		if (inbox == null) {
			return Optional.empty();
		}
		for (AgentMessage message : inbox) {
			if (!message.expired(now) && message.speechAct().equals(speechAct)
					&& message.subject().equals(subject)) {
				return Optional.of(message);
			}
		}
		return Optional.empty();
	}

	/**
	 * A human's own chat answer, recorded on the same audit log as agent speech. It is provenance for
	 * a cooldown and for the evidence trail, not an inbox item: nobody may consume another being's
	 * conversation as if it were a request waiting to be answered.
	 */
	public AgentMessage recordHumanAnswer(String humanId, String agentId, String itemId, String text,
			long tick, int ttlTicks) {
		AgentMessage message = new AgentMessage("msg:" + (++sequence), humanId, agentId,
				AgentMessage.DECLINE, itemId, 0, "", text, tick, tick + ttlTicks);
		log.add(message);
		return message;
	}

	/**
	 * Has this speaker declined this recipient within the window? A person who already said no is not
	 * asked again on the next tick; the being waits out the quiet period on its own.
	 */
	public boolean declinedWithin(String recipientId, String senderId, long now, int windowTicks) {
		for (AgentMessage message : log) {
			if (message.speechAct().equals(AgentMessage.DECLINE)
					&& message.recipientId().equals(recipientId)
					&& message.senderId().equals(senderId)
					&& now - message.createdTick() <= windowTicks) {
				return true;
			}
		}
		return false;
	}

	/** Has this sender already asked this recipient within the window? Keeps asking rate-bounded. */
	public boolean requestedWithin(String senderId, String recipientId, long now, int windowTicks) {
		for (AgentMessage message : log) {
			if (message.speechAct().equals(AgentMessage.REQUEST)
					&& message.senderId().equals(senderId)
					&& message.recipientId().equals(recipientId)
					&& now - message.createdTick() <= windowTicks) {
				return true;
			}
		}
		return false;
	}

	public List<AgentMessage> log() {
		return List.copyOf(log);
	}

	public int pending(String agentId) {
		Deque<AgentMessage> inbox = inboxes.get(agentId);
		return inbox == null ? 0 : inbox.size();
	}
}
