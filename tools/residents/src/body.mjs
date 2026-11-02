// The body boundary: what this client can actually do in the world.
//
// These are motor capabilities, not tasks. A capability is named by the corpus (see
// capabilities.mjs); this file only carries the action out through mineflayer's ordinary player
// path and verifies the result from the body's own state afterwards.
import Vec3 from "vec3";
import { goals } from "./pathfinder.mjs";

export class Body {
	constructor(bot, mc, recipes) {
		this.bot = bot;
		this.mc = mc;
		this.recipes = recipes;
	}

	itemId(name) {
		return this.mc.itemsByName[name]?.id ?? null;
	}

	itemName(id) {
		return this.mc.items[id]?.name ?? `item:${id}`;
	}

	countItem(id) {
		let total = 0;
		for (const item of this.bot.inventory.items()) {
			if (item.type === id) {
				total += item.count;
			}
		}
		return total;
	}

	foodItem() {
		for (const item of this.bot.inventory.items()) {
			const def = this.mc.itemsByName[item.name];
			if (def && (def.foodPoints > 0 || this.mc.foodsByName?.[item.name])) {
				return item;
			}
		}
		return null;
	}

	findBlock(name, maxDistance = 32) {
		const def = this.mc.blocksByName[name];
		if (!def) {
			return null;
		}
		// Prefer a dry exposure. A block under water is not a reachable affordance for
		// this body: collectBlock would dive for it and the body would drown.
		for (const position of this.bot.findBlocks({ matching: def.id, maxDistance, count: 64 })) {
			const block = this.bot.blockAt(position);
			if (!block) {
				continue;
			}
			const above = this.bot.blockAt(block.position.offset(0, 1, 0));
			if (above && (above.name === "water" || above.name === "lava")) {
				continue;
			}
			return block;
		}
		return null;
	}

	needsAir() {
		return this.bot.entity.isInWater || (this.bot.oxygenLevel ?? 20) < 12;
	}

	async resurface() {
		this.bot.pathfinder?.stop();
		this.bot.setControlState("jump", true);
		for (let i = 0; i < 100; i++) {
			if (!this.bot.entity.isInWater && (this.bot.oxygenLevel ?? 20) >= 18) {
				break;
			}
			await new Promise((resolve) => setTimeout(resolve, 100));
		}
		this.bot.setControlState("jump", false);
	}

	async collectSafely(block) {
		let surfaced = false;
		const watch = setInterval(() => {
			if (!surfaced && (this.bot.oxygenLevel ?? 20) <= 6) {
				surfaced = true;
				try {
					this.bot.pathfinder.stop();
				} catch {
					// stopping a pathfinder that is not walking is not an error
				}
				this.bot.setControlState("jump", true);
			}
		}, 200);
		try {
			await withTimeout(this.bot.collectBlock.collect(block), 35000, "collect");
			if (surfaced) {
				throw new Error("surfaced for air");
			}
		} finally {
			clearInterval(watch);
			if (surfaced) {
				this.bot.setControlState("jump", false);
			}
		}
	}

	async goto(position, radius = 1, timeoutMs = 45000) {
		const goal = new goals.GoalNear(position.x, position.y, position.z, radius);
		this.bot.pathfinder.setMovements(this.movements);
		try {
			await withTimeout(this.bot.pathfinder.goto(goal), timeoutMs, "walk");
			return { ok: true, detail: `reached ${fmt(position)}` };
		} catch (error) {
			this.bot.pathfinder.stop();
			return { ok: false, code: "movement_failed", detail: error.message };
		}
	}

	async gotoPlayer(name) {
		const player = this.bot.players[name]?.entity;
		if (!player) {
			return { ok: false, code: "player_not_visible", detail: `${name} is not in sight` };
		}
		const result = await this.goto(player.position, 2);
		return result.ok ? { ok: true, detail: `came to ${name}` } : result;
	}

	async extract(blockName, count, collectedItemId = null) {
		const itemId = collectedItemId ?? this.itemId(blockName);
		const before = this.countItem(itemId);
		const wanted = Math.max(1, count);
		let lastError = "";
		for (let i = 0; i < wanted; i++) {
			if (this.needsAir()) {
				await this.resurface();
			}
			const block = this.findBlock(blockName, 40);
			if (!block) {
				lastError = `no dry ${blockName} in sight`;
				break;
			}
			try {
				await this.collectSafely(block);
			} catch (error) {
				lastError = error.message;
				await this.resurface();
				break;
			}
		}
		const after = this.countItem(itemId);
		const gained = after - before;
		if (gained >= wanted) {
			return { ok: true, detail: `took ${gained} ${blockName}`,
				observed: { before, after, collected: gained } };
		}
		return { ok: false, code: gained > 0 ? "shortfall" : "nothing_collected",
			detail: `took ${gained}/${wanted} ${blockName}${lastError ? ` (${lastError})` : ""}`,
			observed: { before, after, collected: gained } };
	}

	/** Obtain this item however the world allows: craft it, or take it from a block. */
	async ensure(itemId, need, depth = 0) {
		if (this.countItem(itemId) >= need) {
			return { ok: true, detail: `already holds ${need}` };
		}
		if (depth > 5) {
			return { ok: false, code: "plan_too_deep", detail: this.itemName(itemId) };
		}
		const table = this.findBlock("crafting_table", 24);
		const recipes = this.recipes.recipesFor(itemId);
		for (const recipe of recipes) {
			if (recipe.table && !table) {
				const tableMade = await this.ensure(this.itemId("crafting_table"), 1, depth + 1);
				if (!tableMade.ok) {
					continue;
				}
				const placed = await this.place("crafting_table");
				if (!placed.ok) {
					continue;
				}
				await new Promise((resolve) => setTimeout(resolve, 400));
			}
			const batches = Math.max(1, Math.ceil((need - this.countItem(itemId)) / recipe.resultCount));
			let made = true;
			for (const [ingredient, quantity] of recipe.ingredients) {
				const got = await this.ensure(ingredient, quantity * batches, depth + 1);
				if (!got.ok) {
					made = false;
					break;
				}
			}
			if (!made) {
				continue;
			}
			const tableNow = this.findBlock("crafting_table", 24);
			const botRecipes = this.bot.recipesFor(itemId, null, 1, tableNow);
			if (!botRecipes.length) {
				continue;
			}
			try {
				await withTimeout(this.bot.craft(botRecipes[0], batches, tableNow), 30000, "craft");
			} catch (error) {
				return { ok: false, code: "craft_failed", detail: error.message };
			}
			if (this.countItem(itemId) >= need) {
				return { ok: true, detail: `made ${this.itemName(itemId)}` };
			}
		}
		// No recipe worked: take it from the world if a block yields it.
		const block = this.recipes.blockFor(itemId);
		if (block) {
			return this.extract(block.name, need, itemId);
		}
		return { ok: false, code: "not_obtainable", detail: this.itemName(itemId) };
	}

	async place(itemName) {
		const item = this.itemId(itemName);
		if (!item || this.countItem(item) < 1) {
			return { ok: false, code: "item_not_held", detail: itemName };
		}
		const base = this.bot.entity.position;
		let reference = null;
		let face = null;
		for (const [dx, dy, dz] of [[0, -1, 0], [1, -1, 0], [-1, -1, 0], [0, -1, 1], [0, -1, -1]]) {
			const block = this.bot.blockAt(base.offset(dx, dy, dz));
			if (block && block.boundingBox === "block") {
				reference = block;
				face = new Vec3(0, 1, 0);
				break;
			}
		}
		if (!reference) {
			return { ok: false, code: "no_ground", detail: "nothing solid to place on" };
		}
		try {
			await this.bot.equip(item, "hand");
			await this.bot.placeBlock(reference, face);
			return { ok: true, detail: `placed ${itemName}` };
		} catch (error) {
			return { ok: false, code: "place_failed", detail: error.message };
		}
	}

	async deposit(itemId, count) {
		const chest = this.findBlock("chest", 24) ?? this.findBlock("barrel", 24);
		if (!chest) {
			return { ok: false, code: "no_container", detail: "no chest in sight" };
		}
		const walked = await this.goto(chest.position, 2);
		if (!walked.ok) {
			return walked;
		}
		const before = this.countItem(itemId);
		let container = null;
		try {
			container = await withTimeout(this.bot.openContainer(chest), 15000, "open chest");
			await withTimeout(container.deposit(itemId, null, Math.min(count, before)), 15000, "deposit");
			return { ok: this.countItem(itemId) < before, detail: `stored ${before - this.countItem(itemId)}` };
		} catch (error) {
			return { ok: false, code: "container_failed", detail: error.message };
		} finally {
			container?.close();
		}
	}

	async eat() {
		const food = this.foodItem();
		if (!food) {
			return { ok: false, code: "no_food", detail: "nothing edible is held" };
		}
		const before = this.bot.food;
		try {
			await this.bot.equip(food.type, "hand");
			await withTimeout(this.bot.consume(), 20000, "eat");
			return { ok: this.bot.food > before || this.countItem(food.type) < food.count,
				detail: `food ${before} -> ${this.bot.food}` };
		} catch (error) {
			return { ok: false, code: "eat_failed", detail: error.message };
		}
	}

	async rest(home, milliseconds) {
		if (home && this.bot.entity.position.distanceTo(new Vec3(home.x, home.y, home.z)) > 3) {
			const walked = await this.goto(home, 2);
			if (!walked.ok) {
				return walked;
			}
		}
		await new Promise((resolve) => setTimeout(resolve, milliseconds));
		return { ok: true, detail: "rested" };
	}

	async deliver(playerName, itemId, count) {
		const player = this.bot.players[playerName]?.entity;
		if (!player) {
			return { ok: false, code: "player_not_visible", detail: `${playerName} is not in sight` };
		}
		const walked = await this.goto(player.position, 2);
		if (!walked.ok) {
			return walked;
		}
		const before = this.countItem(itemId);
		if (before < count) {
			return { ok: false, code: "not_enough_held", detail: `${before}/${count}` };
		}
		try {
			await this.bot.toss(itemId, null, count);
			return { ok: true, detail: `gave ${count} ${this.itemName(itemId)} to ${playerName}` };
		} catch (error) {
			return { ok: false, code: "toss_failed", detail: error.message };
		}
	}

	async explore() {
		const start = this.bot.entity.position.clone();
		for (let attempt = 0; attempt < 4; attempt++) {
			const angle = Math.random() * Math.PI * 2;
			const distance = 18 + Math.random() * 18;
			const x = Math.floor(start.x + Math.cos(angle) * distance);
			const z = Math.floor(start.z + Math.sin(angle) * distance);
			this.bot.pathfinder.setMovements(this.movements);
			try {
				await withTimeout(this.bot.pathfinder.goto(new goals.GoalXZ(x, z)), 30000, "explore");
				if (this.bot.entity.position.distanceTo(start) > 6) {
					return { ok: true, detail: `walked to ${Math.floor(x)},${Math.floor(z)}` };
				}
			} catch (error) {
				this.bot.pathfinder.stop();
			}
		}
		return { ok: false, code: "explore_blocked", detail: "no open route" };
	}

	async attackNearest(hostiles) {
		const target = hostiles?.[0];
		if (!target) {
			return { ok: false, code: "no_target", detail: "nothing hostile in sight" };
		}
		try {
			for (let i = 0; i < 3; i++) {
				await this.bot.lookAt(target.entity.position.offset(0, 1.5, 0));
				this.bot.attack(target.entity);
				await new Promise((resolve) => setTimeout(resolve, 650));
			}
			return { ok: true, detail: `struck ${target.name}` };
		} catch (error) {
			return { ok: false, code: "attack_failed", detail: error.message };
		}
	}

	chase(name) {
		const player = this.bot.players[name]?.entity;
		if (!player) {
			return { ok: false, code: "player_not_visible", detail: name };
		}
		this.bot.pathfinder.setMovements(this.movements);
		this.bot.pathfinder.setGoal(new goals.GoalFollow(player, 2), true);
		return { ok: true, detail: `following ${name}` };
	}
}

function withTimeout(promise, milliseconds, label) {
	let timer;
	const timeout = new Promise((_, reject) => {
		timer = setTimeout(() => reject(new Error(`${label} timed out after ${milliseconds}ms`)), milliseconds);
	});
	return Promise.race([promise, timeout]).finally(() => clearTimeout(timer));
}

function fmt(position) {
	return `${Math.floor(position.x)},${Math.floor(position.y)},${Math.floor(position.z)}`;
}
