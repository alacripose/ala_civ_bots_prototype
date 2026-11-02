package dev.ensouled;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import dev.ensouled.core.EntityStimulus;
import dev.ensouled.core.InventorySlot;
import dev.ensouled.core.NeedsState;
import dev.ensouled.core.Vec3i;
import dev.ensouled.core.VisibleBlock;
import dev.ensouled.core.WorldView;

/** A deterministic stand-in for the Minecraft world, limited to the same perception contract. */
final class FakeWorld implements WorldView {
	final Map<Vec3i, String> blocks = new LinkedHashMap<>();
	final Map<String, Integer> items = new LinkedHashMap<>();
	final List<EntityStimulus> entities = new ArrayList<>();

	Vec3i body = new Vec3i(0, 0, 0);
	NeedsState needs = NeedsState.healthy();
	long tick;
	int sightRadius = 16;
	/** Simulates a trunk whose lower blocks are hidden behind the block above them. */
	boolean occludeLowerLogs;

	@Override
	public long tick() {
		return tick;
	}

	@Override
	public Vec3i bodyPos() {
		return body;
	}

	@Override
	public String dimensionId() {
		return "minecraft:overworld";
	}

	@Override
	public NeedsState needs() {
		return needs;
	}

	@Override
	public List<VisibleBlock> visibleBlocks(int radius) {
		List<VisibleBlock> out = new ArrayList<>();
		for (Map.Entry<Vec3i, String> entry : blocks.entrySet()) {
			double distance = body.distanceTo(entry.getKey());
			if (distance > Math.min(radius, sightRadius)) {
				continue;
			}
			out.add(new VisibleBlock(entry.getKey(), entry.getValue(), distance, true, canReach(entry.getKey())));
		}
		return out;
	}

	@Override
	public Optional<VisibleBlock> blockAt(Vec3i pos) {
		String id = blocks.get(pos);
		double distance = body.distanceTo(pos);
		if (id == null || distance > sightRadius) {
			return Optional.empty();
		}
		if (occludeLowerLogs) {
			String above = blocks.get(pos.above());
			if (above != null && above.endsWith("_log") && id.endsWith("_log")) {
				return Optional.empty();
			}
		}
		return Optional.of(new VisibleBlock(pos, id, distance, true, canReach(pos)));
	}

	@Override
	public boolean canReach(Vec3i pos) {
		return true;
	}

	@Override
	public boolean isStandable(Vec3i pos) {
		return true;
	}

	@Override
	public Optional<Vec3i> nearestStandingNear(Vec3i target, int radius) {
		return Optional.of(target.offset(1, 0, 0));
	}

	@Override
	public List<InventorySlot> inventory() {
		List<InventorySlot> out = new ArrayList<>();
		int slot = 0;
		for (Map.Entry<String, Integer> entry : items.entrySet()) {
			if (entry.getValue() > 0) {
				out.add(new InventorySlot(slot++, entry.getKey(), entry.getValue()));
			}
		}
		return out;
	}

	@Override
	public int countItem(String itemId) {
		return items.getOrDefault(itemId, 0);
	}

	@Override
	public List<EntityStimulus> entities(int radius) {
		List<EntityStimulus> out = new ArrayList<>();
		for (EntityStimulus stimulus : entities) {
			if (stimulus.distance() <= radius) {
				out.add(stimulus);
			}
		}
		return out;
	}

	@Override
	public Vec3i perceptionAnchor() {
		return body;
	}
}
