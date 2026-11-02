package dev.ensouled.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import dev.ensouled.axis.AxisProof;

import java.util.List;

/**
 * What one being could see, as a message on its own stream.
 *
 * <p>This is the CUA driver's input, and it is an observation rather than a rendering: the blocks
 * the body had a clear line of sight to, the entities within reach of its senses, the items on the
 * ground, and where it was standing when it looked. A reader can compare two of these and see the
 * world change; a screenshot could not be compared by anything but an eye.
 *
 * <p>The row is a §47.2 envelope ({@link MessageEnvelope}): the utterance is prose the world's
 * baseline can read, and every count, coordinate and digest is typed data in {@code payload}. The
 * digest covers the payload, chained to the previous observation, so an observation cannot be
 * replayed or inserted between two others.
 */
public final class ObservationPackage {
	public static final String FORMAT = "civilian.observation/1";
	/** Blocks named in the package; the rest are counted, not listed, so a package stays small. */
	public static final int NEAREST_BLOCKS = 12;

	private ObservationPackage() {
	}

	public static JsonObject of(String agent, WorldView view, List<VisibleBlock> seen, String previousDigest) {
		Vec3i at = view.bodyPos();
		JsonObject counts = new JsonObject();
		int logs = 0;
		for (VisibleBlock block : seen) {
			if (BlockIds.isNaturalLog(block.blockId())) {
				logs++;
			}
		}
		List<EntityStimulus> entities = view.entities(8);
		List<DroppedItem> items = view.droppedItems(8);
		counts.addProperty("blocks", seen.size());
		counts.addProperty("logs", logs);
		counts.addProperty("entities", entities.size());
		counts.addProperty("items", items.size());

		JsonArray nearest = new JsonArray();
		for (VisibleBlock block : seen) {
			if (nearest.size() >= NEAREST_BLOCKS) {
				break;
			}
			JsonObject row = new JsonObject();
			row.addProperty("pos", block.pos().toString());
			row.addProperty("block", block.blockId());
			row.addProperty("distance", Math.round(block.distance() * 10) / 10.0);
			row.addProperty("sight", block.lineOfSight());
			row.addProperty("reachable", block.reachable());
			nearest.add(row);
		}
		JsonArray entityRows = new JsonArray();
		for (EntityStimulus entity : entities) {
			JsonObject row = new JsonObject();
			row.addProperty("id", entity.entityId());
			row.addProperty("type", entity.entityTypeId());
			row.addProperty("pos", entity.pos().toString());
			row.addProperty("distance", Math.round(entity.distance() * 10) / 10.0);
			row.addProperty("visible", entity.visible());
			row.addProperty("hostile", entity.hostileEngaged());
			entityRows.add(row);
		}
		JsonArray itemRows = new JsonArray();
		for (DroppedItem item : items) {
			JsonObject row = new JsonObject();
			row.addProperty("id", item.entityId());
			row.addProperty("item", item.itemId());
			row.addProperty("count", item.count());
			row.addProperty("pos", item.pos().toString());
			itemRows.add(row);
		}

		JsonObject payload = new JsonObject();
		payload.addProperty("format", FORMAT);
		payload.addProperty("agent", agent);
		payload.addProperty("tick", view.tick());
		payload.addProperty("dimension", view.dimensionId());
		payload.addProperty("at", at.toString());
		payload.addProperty("sight_radius", CandidateGenerator.SIGHT_RADIUS);
		payload.add("counts", counts);
		payload.add("nearest", nearest);
		payload.add("entities", entityRows);
		payload.add("items", itemRows);
		payload.addProperty("prev", previousDigest == null ? "genesis" : previousDigest);
		String digest = "sha256:" + AxisProof.digest(AxisProof.utf8(payload.toString()));
		payload.addProperty("digest", digest);

		return MessageEnvelope.self(agent, "obs:" + agent, "REPORT",
				observationLine(agent, seen.size(), logs, entities.size(), items.size()), view.tick(),
				view.dimensionId(), at, payload);
	}

	/**
	 * The sentence a reader (or the being's own driver) is handed. It is a claim of record with the
	 * counts in it, so the lane carries language and the numbers stay in the typed payload.
	 */
	static String observationLine(String agent, int blocks, int logs, int entities, int items) {
		StringBuilder sentence = new StringBuilder(agent).append(" can see ").append(blocks)
				.append(blocks == 1 ? " block" : " blocks");
		if (logs > 0) {
			sentence.append(", ").append(logs).append(logs == 1 ? " of them a log" : " of them logs");
		}
		sentence.append(", ").append(entities).append(entities == 1 ? " being" : " beings")
				.append(" and ").append(items).append(items == 1 ? " item" : " items")
				.append(" within reach");
		return ProseLines.record(agent, sentence.toString());
	}
}
