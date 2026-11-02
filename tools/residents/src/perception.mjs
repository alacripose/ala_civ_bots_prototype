// What this body can see, in its own connection's terms.
//
// The snapshot is locality-limited: only blocks the client has loaded, only entities within reach
// of its own eyes, only its own inventory and HUD. Nothing here asks the server for a global view.
export function perceive(bot, mc, recipes) {
	const pos = bot.entity.position;
	const inventory = new Map();
	const held = [];
	for (const item of bot.inventory.items()) {
		inventory.set(item.type, (inventory.get(item.type) ?? 0) + item.count);
		held.push({ id: item.type, name: item.name, count: item.count, slot: item.slot });
	}

	const resources = new Map();
	const containers = [];
	const tables = [];
	const ids = mc.blocksArray
		.filter((block) => block.diggable && block.boundingBox === "block" && block.id !== 0)
		.map((block) => block.id);
	for (const p of bot.findBlocks({ matching: ids, maxDistance: 24, count: 512 })) {
		const block = bot.blockAt(p);
		if (!block || block.name === "air" || block.name === "cave_air") {
			continue;
		}
		// A block covered by water or lava cannot be worked from the surface without
		// breathing it. It is not an affordance this body can take on.
		const above = bot.blockAt(block.position.offset(0, 1, 0));
		if (block.name === "water" || block.name === "lava"
				|| (above && (above.name === "water" || above.name === "lava"))) {
			continue;
		}
		const distance = pos.distanceTo(block.position);
		if (block.name.endsWith("chest") || block.name === "barrel" || block.name.endsWith("shulker_box")) {
			containers.push({ name: block.name, block, distance });
		}
		if (block.name.endsWith("crafting_table")) {
			tables.push({ name: block.name, block, distance });
		}
		const itemId = dropOf(block, mc);
		if (itemId === null || !recipes.usefulItems.has(itemId)) {
			continue;
		}
		const row = resources.get(itemId);
		if (row) {
			row.count++;
			if (distance < row.distance) {
				row.distance = distance;
				row.block = block;
			}
		} else {
			resources.set(itemId, {
				itemId,
				name: mc.items[itemId]?.name ?? block.name,
				blockName: block.name,
				block,
				count: 1,
				distance,
			});
		}
	}

	const players = [];
	const hostiles = [];
	for (const entity of Object.values(bot.entities)) {
		if (!entity || entity === bot.entity || !entity.position) {
			continue;
		}
		const distance = pos.distanceTo(entity.position);
		if (distance > 24) {
			continue;
		}
		if (entity.type === "player" || entity.username) {
			players.push({
				name: entity.username ?? entity.name ?? "someone",
				entity,
				distance,
				position: entity.position,
			});
		} else if (isHostile(entity, mc)) {
			hostiles.push({ name: entity.name, entity, distance, position: entity.position });
		}
	}
	players.sort((a, b) => a.distance - b.distance);
	hostiles.sort((a, b) => a.distance - b.distance);

	const timeOfDay = bot.time?.timeOfDay ?? 0;
	return {
		tick: bot.time?.age ?? 0,
		position: { x: pos.x, y: pos.y, z: pos.z },
		dimension: bot.game?.dimension ?? "minecraft:overworld",
		health: bot.health,
		food: bot.food,
		air: bot.air ?? 300,
		onFire: bot.entity.isInLava || false,
		inWater: bot.entity.isInWater || false,
		onGround: bot.entity.onGround,
		timeOfDay,
		isNight: timeOfDay >= 13000 && timeOfDay <= 23000,
		inventory,
		held,
		resources: [...resources.values()].sort((a, b) => a.distance - b.distance),
		containers: containers.sort((a, b) => a.distance - b.distance),
		tables: tables.sort((a, b) => a.distance - b.distance),
		players,
		hostiles,
		positionVec: pos,
	};
}

function dropOf(block, mc) {
	if (Array.isArray(block.drops) && block.drops.length) {
		const first = block.drops[0];
		if (typeof first === "number") {
			return first;
		}
		if (typeof first === "string") {
			return mc.itemsByName[first]?.id ?? null;
		}
		if (first && typeof first === "object" && typeof first.id === "number") {
			return first.id;
		}
	}
	if (block.name === "stone") {
		return mc.itemsByName.cobblestone?.id ?? null;
	}
	return mc.itemsByName[block.name]?.id ?? null;
}

function isHostile(entity, mc) {
	const type = mc.entitiesByName?.[entity.name]?.type;
	if (type) {
		return type === "hostile" || type === "Hostile mobs";
	}
	return entity.type === "hostile" || entity.kind === "Hostile mobs";
}

export function inventoryCount(perception, itemId) {
	return perception.inventory.get(itemId) ?? 0;
}
