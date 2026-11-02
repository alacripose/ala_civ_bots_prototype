// A mineflayer body for the Java resident's AXIS decisions.
//
// The server keeps the mind, the wish desk and the AXIS selection. This process only reads the
// motor frames that mind emits, performs them through ordinary player input, and appends what it
// observed. There is no decision loop here and no Fabric control mod: the transport is the world's
// own motor directory.
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import mineflayer from "mineflayer";
import mcDataFactory from "minecraft-data";
import Vec3 from "vec3";
import { ClientObservation } from "./observation.mjs";
import { Movements, goals, pathfinder } from "./pathfinder.mjs";
import toolModule from "mineflayer-tool";
import { DecisionStream } from "./axis.mjs";
import { verbs } from "./verbs.mjs";

const { plugin: toolPlugin } = toolModule;
const args = parseArgs(process.argv.slice(2));
const name = args.name ?? process.env.MC_USER ?? "Ash";
const host = args.host ?? process.env.MC_HOST ?? "127.0.0.1";
const port = Number(args.port ?? process.env.MC_PORT ?? 25565);
const HERE = path.dirname(fileURLToPath(import.meta.url));
const PROJECT_ROOT = path.resolve(HERE, "../../..");
const MOTOR_DIR = path.join(PROJECT_ROOT, "run/world/ensouled/motor");
const commandsPath = path.join(MOTOR_DIR, `${name}.commands.jsonl`);
const resultsPath = path.join(MOTOR_DIR, `${name}.results.jsonl`);
const speechPath = path.join(MOTOR_DIR, `${name}.speech.jsonl`);
const speechResultsPath = path.join(MOTOR_DIR, `${name}.speech.results.jsonl`);
const LEDGER_PATH = path.join(PROJECT_ROOT, "run/stream/ledger.jsonl");

function log(kind, message) {
	const clock = new Date().toISOString().slice(11, 19);
	console.log(`[${clock}] [${name}] ${kind}: ${message}`);
}

const completed = new Set();
if (fs.existsSync(resultsPath)) {
	for (const row of readRows(resultsPath)) {
		if (row.id) {
			completed.add(row.id);
		}
	}
}
// Speech is a stream, not a motor frame: do not replay lines that were already on disk when this
// process started, but accept every new line even while a movement program is active.
const spoken = new Set(readRows(speechPath).map((row) => row.id).filter(Boolean));

const bot = mineflayer.createBot({
	host,
	port,
	username: name,
	auth: "offline",
	version: args.version ?? "1.20.1",
});

bot.loadPlugin(pathfinder);
bot.loadPlugin(toolPlugin);

let mc = null;
let movements = null;
let processing = false;
let polling = false;
let observations = null;
let decisions = null;

bot.once("spawn", () => {
	mc = mcDataFactory(bot.version);
	movements = new Movements(bot, mc);
	movements.canDig = false;
	movements.allow1by1towers = false;
	movements.canOpenDoors = true;
	movements.scafoldingBlocks = [];
	bot.pathfinder.setMovements(movements);
	log("motor", `body online as ${bot.username} at ${fmt(bot.entity.position)}`);
	observations = new ClientObservation({ bot, name, directory: MOTOR_DIR, log });
	observations.session("spawn");
	// The axis suite's decision packages are the only account of *why* a frame runs. The body
	// reads the newest one that stands and records it beside the result; if neither the package
	// nor its vocabulary can be read, the frame is still the server's and is executed, but the
	// record says so instead of inventing a reading.
	decisions = new DecisionStream({ file: LEDGER_PATH, agent: name, log });
	decisions.open();
	startPolling();
});

bot.on("death", () => {
	log("motor", "body died; waiting for the server to bind the respawn");
});

bot.on("respawn", () => {
	if (movements) {
		bot.pathfinder.setMovements(movements);
	}
	observations?.session("respawn");
	log("motor", `respawned at ${fmt(bot.entity.position)}`);
});

bot.on("chat", (username, message) => {
	if (username !== bot.username) {
		log("heard", `<${username}> ${message}`);
	}
	observations?.chat(username, message);
});

bot.on("kicked", (reason) => {
	log("kicked", typeof reason === "string" ? reason : JSON.stringify(reason));
	process.exit(1);
});
bot.on("error", (error) => log("error", error.message));
bot.on("end", (reason) => {
	log("end", reason ?? "connection closed");
	process.exit(0);
});

process.on("SIGINT", () => {
	try {
		bot.pathfinder.stop();
	} catch {
		// already stopped
	}
	bot.quit("motor stopped");
	setTimeout(() => process.exit(0), 600);
});

function startPolling() {
	if (polling) {
		return;
	}
	polling = true;
	setInterval(poll, 150);
	setInterval(pollSpeech, 150);
}

async function poll() {
	if (processing || !bot.entity || !bot.entity.position) {
		return;
	}
	decisions?.poll();
	const rows = readRows(commandsPath);
	const next = rows.find((row) => row && row.id && !completed.has(row.id));
	if (!next) {
		return;
	}
	processing = true;
	observations?.startFrame(next);
	log("frame", `${next.kind ?? "unknown"} ${next.id}`);
	let result;
	try {
		result = await execute(next);
	} catch (error) {
		result = failed("motor_exception", error.message);
	}
	completed.add(next.id);
	const axis = decisions?.reading();
	appendResult({ id: next.id, ...result, ...(axis ? { axis: named(axis) } : {}) });
	observations?.finishFrame(next, result);
	log("result", `${result.status} ${result.code ?? ""} ${result.detail ?? ""}`);
	processing = false;
}

function pollSpeech() {
	if (!bot.entity) {
		return;
	}
	for (const row of readRows(speechPath)) {
		if (!row || !row.id || spoken.has(row.id)) {
			continue;
		}
		spoken.add(row.id);
		if (row.kind !== "Say" || typeof row.text !== "string" || !row.text.trim()) {
			appendSpeechResult({ id: row.id, status: "FAILED", code: "speech_malformed", detail: "row is not a Say frame", facts: {} });
			continue;
		}
		// The corpus is the training set: a word it never uses is not spoken here, and the line is
		// reported by name instead of being relayed. This is what catches invented prose at the
		// body (`plankss`) rather than after it has been heard by a person.
		const missing = verbs.unattestedWords(row.text);
		if (missing.length) {
			appendSpeechResult({
				id: row.id,
				status: "FAILED",
				code: "unattested_word",
				detail: `the corpus does not attest ${missing.join(", ")}; the line was not spoken`,
				facts: { unattested: missing.join(",") },
			});
			log("speech", `refused: unattested ${missing.join(", ")} in "${row.text}"`);
			continue;
		}
		try {
			bot.chat(row.text);
			observations?.said(row.id, row.text);
			appendSpeechResult({ id: row.id, status: "SPOKEN", code: "chat_sent", detail: "sent through the body's ordinary player chat", facts: {} });
			log("speech", `said ${row.text}`);
		} catch (error) {
			appendSpeechResult({ id: row.id, status: "FAILED", code: "chat_failed", detail: error.message, facts: {} });
			log("speech", `failed: ${error.message}`);
		}
	}
}

async function execute(command) {
	switch (command.kind) {
		case "MoveTo":
			return moveTo(command);
		case "LookAt":
			return lookAt(command);
		case "MineBlock":
			return mineBlock(command);
		case "Jump":
			return jump(command);
		case "Use":
			return use(command);
		case "Equip":
			return equip(command);
		case "DropItem":
			return dropItem(command);
		case "CollectDrops":
			return collectDrops(command);
		case "Eat":
			return eat(command);
		case "MoveAwayFrom":
			return moveAwayFrom(command);
		case "Wait":
			await sleep(Math.max(0, Number(command.ticks ?? 1)) * 50);
			return ok("waited", `waited ${command.ticks ?? 1} ticks`, {});
		case "Cancel":
			return cancel(command);
		default:
			return failed("unknown_frame", `no motor for ${command.kind}`);
	}
}

async function moveTo(command) {
	const target = vec(command.target);
	const radius = Number(command.radius ?? 1.5);
	try {
		await gotoGoal(new goals.GoalNear(target.x, target.y, target.z, radius),
			timeoutFor(command, 5000), "move");
		const distance = bot.entity.position.distanceTo(target);
		return ok("arrived", `reached ${fmt(bot.entity.position)}`, {
			distance: distance.toFixed(2),
		});
	} catch (error) {
		stopMovement();
		return failed("movement_failed", error.message);
	}
}

async function lookAt(command) {
	const target = vec(command.target);
	try {
		await withTimeout(
			bot.lookAt(new Vec3(target.x + 0.5, target.y + 0.5, target.z + 0.5), true),
			4000,
			"look",
		);
		return ok("oriented", "looked at the target", {});
	} catch (error) {
		return failed("look_failed", error.message);
	}
}

async function mineBlock(command) {
	const target = vec(command.target);
	const block = bot.blockAt(target);
	if (!block) {
		return failed("block_not_loaded", `no block at ${fmt(target)}`);
	}
	const expected = stripNamespace(command.expectedBlockId);
	if (block.name !== expected) {
		return failed("block_changed", `expected ${expected} but found ${block.name}`);
	}
	const before = countAll();
	try {
		if (bot.tool?.equipForBlock) {
			await bot.tool.equipForBlock(block, { requireHarvest: false }).catch(() => {});
		}
		await withTimeout(bot.dig(block, true), timeoutFor(command, 4000), "dig");
	} catch (error) {
		return failed("mining_failed", error.message);
	}
	const afterBlock = bot.blockAt(target);
	const removed = !afterBlock || afterBlock.name === "air" || afterBlock.name !== expected;
	const delta = countAll() - before;
	return ok("block_removed", `mined ${expected} at ${fmt(target)}`, {
		block_removed: String(removed),
		inventory_delta: String(delta),
		block_id: `minecraft:${expected}`,
	});
}

async function jump(command) {
	const before = bot.entity.position.y;
	bot.setControlState("jump", true);
	try {
		await sleep(250);
	} finally {
		bot.setControlState("jump", false);
	}
	const after = bot.entity.position.y;
	if (after > before + 0.1) {
		return ok("jumped", `Y rose ${(after - before).toFixed(2)}`, {
			y_before: before.toFixed(2),
			y_after: after.toFixed(2),
		});
	}
	return failed("jump_failed", `Y stayed at ${after.toFixed(2)}`);
}

async function use(command) {
	const target = vec(command.target);
	const reference = bot.blockAt(target);
	if (!reference) {
		return failed("block_not_loaded", `no block at ${fmt(target)}`);
	}
	const expected = stripNamespace(command.expectedBlockId);
	const faces = [
		[0, 1, 0], [0, -1, 0], [1, 0, 0], [-1, 0, 0], [0, 0, 1], [0, 0, -1],
	];
	for (const [dx, dy, dz] of faces) {
		const adjacent = target.offset(dx, dy, dz);
		const at = bot.blockAt(adjacent);
		if (!at || (at.name !== "air" && at.name !== "cave_air" && at.name !== "water")) {
			continue;
		}
		try {
			await withTimeout(bot.placeBlock(reference, new Vec3(dx, dy, dz)), timeoutFor(command, 4000), "place");
			const placed = bot.blockAt(adjacent);
			if (placed && placed.name === expected) {
				return ok("placed", `placed ${expected} at ${fmt(adjacent)}`, {
					placed_at: fmt(adjacent),
					held: heldName(),
				});
			}
		} catch {
			// Try another face or the ordinary use action below.
		}
	}
	try {
		await withTimeout(bot.activateBlock(reference), 4000, "use");
		return ok("used", `used ${reference.name} at ${fmt(target)}`, {});
	} catch (error) {
		return failed("use_failed", error.message);
	}
}

async function equip(command) {
	const itemId = stripNamespace(command.itemId);
	const item = bot.inventory.items().find((stack) => stack.name === itemId);
	if (!item) {
		return failed("not_held", `no ${itemId} in the inventory`);
	}
	try {
		await withTimeout(bot.equip(item, "hand"), 4000, "equip");
		return ok("equipped", `holding ${itemId}`, { item: itemId });
	} catch (error) {
		return failed("equip_failed", error.message);
	}
}

async function dropItem(command) {
	const itemId = stripNamespace(command.itemId);
	const item = bot.inventory.items().find((stack) => stack.name === itemId);
	if (!item) {
		return failed("not_held", `no ${itemId} in the inventory`);
	}
	const before = countItem(itemId);
	const count = Math.max(1, Number(command.count ?? 1));
	try {
		await withTimeout(bot.toss(item.type, null, count), 6000, "drop");
	} catch (error) {
		return failed("drop_failed", error.message);
	}
	const after = countItem(itemId);
	return ok("dropped", `dropped ${count} ${itemId}`, {
		item: itemId,
		count: String(count),
		inventory_before: String(before),
		inventory_after: String(after),
	});
}

async function collectDrops(command) {
	const radius = Number(command.radius ?? 4);
	for (let attempt = 0; attempt < 3; attempt++) {
		const drops = Object.values(bot.entities).filter((entity) => {
			if (!entity || !entity.position || entity === bot.entity) {
				return false;
			}
			const distance = entity.position.distanceTo(bot.entity.position);
			return entity.name === "item" && distance <= radius + 2;
		});
		if (!drops.length) {
			return ok("collected", "no drops remain nearby", { collected: "0" });
		}
		drops.sort((a, b) => a.position.distanceTo(bot.entity.position) - b.position.distanceTo(bot.entity.position));
		const drop = drops[0];
		try {
			await gotoGoal(new goals.GoalNear(drop.position.x, drop.position.y, drop.position.z, 0.9),
				timeoutFor(command, 6000), "collect");
		} catch (error) {
			stopMovement();
			return failed("collect_failed", error.message);
		}
		await sleep(700);
	}
	return ok("collected", "walked over the nearby drops", { collected: "1" });
}

async function eat(command) {
	const itemId = stripNamespace(command.itemId);
	const item = bot.inventory.items().find((stack) => stack.name === itemId);
	if (!item) {
		return failed("no_food_item", `no ${itemId} in the inventory`);
	}
	const before = bot.food;
	try {
		await bot.equip(item, "hand");
		await withTimeout(bot.consume(), 12000, "eat");
		return ok("ate", `food ${before} -> ${bot.food}`, {
			food_before: String(before),
			food_after: String(bot.food),
			item_consumed: itemId,
		});
	} catch (error) {
		return failed("eat_failed", error.message);
	}
}

async function moveAwayFrom(command) {
	const from = vec(command.from);
	const distance = Math.max(4, Number(command.distance ?? 6));
	const here = bot.entity.position;
	let dx = here.x - (from.x + 0.5);
	let dz = here.z - (from.z + 0.5);
	const length = Math.hypot(dx, dz);
	if (length < 0.01) {
		dx = 1;
		dz = 0;
	}
	const scale = distance / Math.max(length, 0.01);
	const target = new Vec3(here.x + dx * scale, here.y, here.z + dz * scale);
	try {
		await withTimeout(
			bot.pathfinder.goto(new goals.GoalNear(target.x, target.y, target.z, 1.5)),
			timeoutFor(command, 5000),
			"escape",
		);
		return ok("escaped", "moved away from the hazard", {});
	} catch (error) {
		stopMovement();
		return failed("escape_failed", error.message);
	}
}

function cancel() {
	stopMovement();
	return { status: "CANCELLED", code: "cancelled", detail: "cancelled by controller", facts: {} };
}

function stopMovement() {
	try {
		// stop() leaves a one-tick flag that makes the next setGoal emit path_stop, rejecting
		// the next goto before it starts. setGoal(null) clears the path now and leaves the
		// next frame free to start normally.
		bot.pathfinder.setGoal(null);
	} catch {
		// pathfinder may not be walking
	}
	bot.clearControlStates();
}

function gotoGoal(goal, milliseconds, label) {
	const movement = bot.pathfinder.goto(goal);
	// withTimeout can win the race; keep the abandoned goto from becoming an unhandled rejection.
	movement.catch(() => {});
	return withTimeout(movement, milliseconds, label);
}

function countItem(itemId) {
	let total = 0;
	for (const stack of bot.inventory.items()) {
		if (stack.name === itemId) {
			total += stack.count;
		}
	}
	return total;
}

function countAll() {
	let total = 0;
	for (const stack of bot.inventory.items()) {
		total += stack.count;
	}
	return total;
}

function heldName() {
	return bot.heldItem?.name ?? "";
}

function ok(code, detail, facts) {
	return { status: "SUCCESS", code, detail, facts };
}

function failed(code, detail) {
	return { status: "FAILED", code, detail, facts: {} };
}

function vec(raw) {
	// Mineflayer's blockAt/distanceTo/offset APIs require a real Vec3. A plain {x,y,z}
	// reaches blockAt as a position without floored(), which was failing every MineBlock.
	return new Vec3(
		Number(raw?.x ?? 0),
		Number(raw?.y ?? 0),
		Number(raw?.z ?? 0),
	);
}

function stripNamespace(value) {
	return String(value ?? "").replace(/^minecraft:/, "");
}

function fmt(position) {
	return `${Math.floor(position.x)},${Math.floor(position.y)},${Math.floor(position.z)}`;
}

function timeoutFor(command, fallback) {
	const ticks = Number(command.timeoutTicks ?? 0);
	return Math.max(fallback, ticks > 0 ? ticks * 50 + 3000 : 0);
}

function sleep(milliseconds) {
	return new Promise((resolve) => setTimeout(resolve, milliseconds));
}

function withTimeout(promise, milliseconds, label) {
	let timer;
	const timeout = new Promise((_, reject) => {
		timer = setTimeout(() => reject(new Error(`${label} timed out after ${milliseconds}ms`)), milliseconds);
	});
	return Promise.race([promise, timeout]).finally(() => clearTimeout(timer));
}

function readRows(file) {
	if (!fs.existsSync(file)) {
		return [];
	}
	const rows = [];
	for (const line of fs.readFileSync(file, "utf8").split(/\r?\n/)) {
		if (!line.trim()) {
			continue;
		}
		try {
			rows.push(JSON.parse(line));
		} catch {
			// A line that is still being appended is read again on the next poll.
		}
	}
	return rows;
}

function appendResult(result) {
	fs.mkdirSync(MOTOR_DIR, { recursive: true });
	fs.appendFileSync(resultsPath, `${JSON.stringify(result)}\n`);
}

function appendSpeechResult(result) {
	fs.mkdirSync(MOTOR_DIR, { recursive: true });
	fs.appendFileSync(speechResultsPath, `${JSON.stringify(result)}\n`);
}

/** The axis reading as the body records it: the ladder tokens, and the corpus's word for the act. */
function named(axis) {
	return {
		decision: axis.decision,
		tick: axis.tick,
		skill: axis.skill,
		verb: axis.verb || verbs.verbFor(axis.skill),
		verb_basis: axis.verb_basis || verbs.basis(axis.skill),
		token: axis.token,
		establishment: axis.establishment,
		admitted: axis.admitted,
		checked: axis.checked,
		digest_ok: axis.digest_ok,
		refusals: axis.refusals,
		recording: "newest standing decision at frame dispatch",
	};
}

function parseArgs(argv) {
	const out = {};
	for (let i = 0; i < argv.length; i++) {
		const arg = argv[i];
		if (arg.startsWith("--")) {
			const key = arg.slice(2);
			const value = argv[i + 1] && !argv[i + 1].startsWith("--") ? argv[++i] : "true";
			out[key] = value;
		}
	}
	return out;
}
