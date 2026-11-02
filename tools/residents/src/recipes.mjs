// The game's own recipe graph, read from minecraft-data.
//
// Nothing here lists what a resident may craft: a recipe exists because the world's registry says
// it does. The index only answers the questions a body boundary asks — what does this need, does it
// need a table, and which variants can this inventory actually satisfy.
export class RecipeIndex {
	constructor(mcData) {
		this.mcData = mcData;
		this.byResult = new Map();
		this.usefulItems = new Set();
		for (const [key, variants] of Object.entries(mcData.recipes ?? {})) {
			const resultId = Number.parseInt(key, 10);
			if (!Number.isFinite(resultId)) {
				continue;
			}
			const rows = [];
			for (const variant of variants) {
				const ingredients = ingredientCounts(variant);
				if (!ingredients.size) {
					continue;
				}
				const total = [...ingredients.values()].reduce((sum, n) => sum + n, 0);
				const shape = variant.inShape ?? null;
				const width = shape ? Math.max(...shape.map((row) => row.length)) : 0;
				const height = shape ? shape.length : 0;
				rows.push({
					resultId,
					resultCount: variant.result?.count ?? 1,
					ingredients,
					table: shape ? width > 2 || height > 2 : total > 4,
				});
				for (const id of ingredients.keys()) {
					this.usefulItems.add(id);
				}
				this.usefulItems.add(resultId);
			}
			if (rows.length) {
				this.byResult.set(resultId, rows);
			}
		}
		for (const item of mcData.foodsArray ?? []) {
			this.usefulItems.add(item.id);
		}
	}

	item(name) {
		return this.mcData.itemsByName[name] ?? null;
	}

	name(id) {
		return this.mcData.items[id]?.name ?? `item:${id}`;
	}

	recipesFor(itemId) {
		return this.byResult.get(itemId) ?? [];
	}

	/** A recipe whose ingredients are all present, or null. Table use is checked by the caller. */
	craftableNow(itemId, counts) {
		for (const recipe of this.recipesFor(itemId)) {
			let ok = true;
			for (const [ingredient, needed] of recipe.ingredients) {
				if ((counts.get(ingredient) ?? 0) < needed) {
					ok = false;
					break;
				}
			}
			if (ok) {
				return recipe;
			}
		}
		return null;
	}

	/** Could this item be obtained by crafting, recursively, from what the body already has? */
	craftable(itemId, counts, depth = 0) {
		if (depth > 5) {
			return false;
		}
		if ((counts.get(itemId) ?? 0) > 0) {
			return true;
		}
		for (const recipe of this.recipesFor(itemId)) {
			let ok = true;
			for (const [ingredient, needed] of recipe.ingredients) {
				if ((counts.get(ingredient) ?? 0) < needed && !this.craftable(ingredient, counts, depth + 1)) {
					ok = false;
					break;
				}
			}
			if (ok) {
				return true;
			}
		}
		return false;
	}

	/** The block that yields this item when broken, if the registry has one. */
	blockFor(itemId) {
		const name = this.name(itemId);
		return this.mcData.blocksByName[name] ?? null;
	}
}

function ingredientCounts(variant) {
	const counts = new Map();
	if (Array.isArray(variant.inShape)) {
		for (const row of variant.inShape) {
			for (const id of row) {
				if (id !== null && id !== undefined) {
					counts.set(id, (counts.get(id) ?? 0) + 1);
				}
			}
		}
		return counts;
	}
	if (Array.isArray(variant.ingredients)) {
		for (const id of variant.ingredients) {
			if (id !== null && id !== undefined) {
				counts.set(id, (counts.get(id) ?? 0) + 1);
			}
		}
	}
	return counts;
}
