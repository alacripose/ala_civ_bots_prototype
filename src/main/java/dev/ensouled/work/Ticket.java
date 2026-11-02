package dev.ensouled.work;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import dev.ensouled.axis.AxisProof;

import java.util.ArrayList;
import java.util.List;

/**
 * The smallest auditable unit of work (v4 §12.2), carrying the properties v9 §19.5 requires to be
 * traceable together: one identity, declared cells, an opened window, a witness, a digest and a
 * named state.
 *
 * <p>A ticket is not a copy of the commitment it serves. The commitment says what is owed and until
 * when; the ticket says what was authorised, how many execution cells that authorisation covers, and
 * which decisions actually spent them. It closes on observed evidence ({@code HELD}), on a window
 * that closed first ({@code ABSENT}), or because the being put the work down ({@code REFUSED}) — and
 * it never closes because somebody said it was done.
 *
 * <p>{@code artifact} is the whole record in one line, so the digest a reader checks is a digest of
 * the ticket and not of a summary of it. The Node reader recomputes the same framed digest.
 */
public record Ticket(String ticketId, String commitmentId, String worldId, String agentId,
		String presenter, String purpose, String verb, String verbBasis, int declaredCells,
		int usedCells, long openedTick, long closesTick, State state, long closedTick,
		String closedWhy, String witness, List<String> evidence) {

	public static final String FORMAT = "civilian.ticket/1";

	/** The named states v9 §19.5 uses for a claim. {@code OPEN} is work in flight, not a verdict. */
	public enum State {
		OPEN,
		HELD,
		ABSENT,
		REFUSED
	}

	public static Ticket opened(String ticketId, String commitmentId, String worldId, String agentId,
			String presenter, String purpose, String verb, String verbBasis, int declaredCells,
			long openedTick, long closesTick) {
		return new Ticket(ticketId, commitmentId, worldId, agentId, presenter,
				purpose == null ? "" : purpose, verb == null ? "" : verb,
				verbBasis == null ? "" : verbBasis, Math.max(1, declaredCells), 0, openedTick,
				closesTick, State.OPEN, 0L, "", "", List.of());
	}

	public boolean open() {
		return state == State.OPEN;
	}

	/** A declared cell budget is a bound, not a suggestion: spent is spent. */
	public boolean cellAvailable() {
		return open() && usedCells < declaredCells;
	}

	/** One decision spends one cell. The decision id is kept, so the spend is checkable later. */
	public Ticket withCell(String decisionId) {
		List<String> rows = new ArrayList<>(evidence);
		if (decisionId != null && !decisionId.isBlank()) {
			rows.add(decisionId);
		}
		return new Ticket(ticketId, commitmentId, worldId, agentId, presenter, purpose, verb,
				verbBasis, declaredCells, usedCells + 1, openedTick, closesTick, state, closedTick,
				closedWhy, witness, List.copyOf(rows));
	}

	public Ticket closed(State next, long tick, String why, String witness) {
		return new Ticket(ticketId, commitmentId, worldId, agentId, presenter, purpose, verb,
				verbBasis, declaredCells, usedCells, openedTick, closesTick, next, tick,
				why == null ? "" : why, witness == null ? "" : witness, evidence);
	}

	/**
	 * The bytes the digest is taken over. Every field that changes the claim is in it, so a reader
	 * cannot be shown a digest of an older, cheaper ticket.
	 */
	public String artifact() {
		return FORMAT + "|" + ticketId + "|" + commitmentId + "|" + agentId + "|" + purpose + "|"
				+ verb + "|" + state + "|" + usedCells + "/" + declaredCells + "|" + witness + "|"
				+ String.join(",", evidence);
	}

	public String digest() {
		return "sha256:" + AxisProof.digest(AxisProof.utf8(artifact()));
	}

	/** The package a reader receives: shape in one place, state named in words, digest over all of it. */
	public JsonObject json() {
		JsonObject cells = new JsonObject();
		cells.addProperty("declared", declaredCells);
		cells.addProperty("used", usedCells);
		JsonObject window = new JsonObject();
		window.addProperty("opened", openedTick);
		window.addProperty("closes", closesTick);
		JsonArray spent = new JsonArray();
		evidence.forEach(spent::add);

		JsonObject payload = new JsonObject();
		payload.addProperty("format", FORMAT);
		payload.addProperty("ticket", ticketId);
		payload.addProperty("commitment", commitmentId);
		payload.addProperty("world", worldId);
		payload.addProperty("agent", agentId);
		payload.addProperty("presenter", presenter);
		payload.addProperty("purpose", purpose);
		payload.addProperty("verb", verb);
		payload.addProperty("verb_basis", verbBasis);
		payload.add("cells", cells);
		payload.add("window", window);
		payload.addProperty("state", state.name());
		payload.addProperty("closed_tick", closedTick);
		payload.addProperty("why", closedWhy);
		payload.addProperty("witness", witness);
		payload.add("evidence", spent);
		payload.addProperty("artifact", artifact());
		payload.addProperty("digest", digest());
		return payload;
	}
}
