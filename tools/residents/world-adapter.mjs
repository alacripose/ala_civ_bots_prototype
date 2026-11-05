// The live world, served to the axis as a world adapter.
//
// This is not a simulation of a world: it is a mineflayer body logged into a real Minecraft server
// (1.20.1, flat world, 127.0.0.1:25566). Every value it reports was sent to it by that server:
// the body's position arrives in entity-move packets, the blocks arrive in chunk and block-update
// packets. The digest is a sha256 over exactly that server-sent state, so it is the server's
// answer and not this process's opinion.
//
// Two acts, both ordinary player input, both verified from the world afterwards:
//
// - `move`  {input:{x,y,z}}  walk to a nearby target through the pathfinder; report the position the
//                            server placed the body at. If the body did not move, refuse — the axis
//                            must not be handed an observation of a walk that did not happen.
// - `dig`   {input:{x,y,z}}  dig the block at that point and re-read it; report air only if the
//                            server sent the block update that made it air.
//
// A refusal is `409` with a named code (`world-not-moved`, `world-not-dug`, `body-offline`,
// `world-frame-field`), so a frame the world declined never becomes a verified act.
//
//   node tools/residents/world-adapter.mjs [--port 8789] [--host 127.0.0.1] [--mc-port 25566]
//                                          [--name AxisBody] [--version 1.20.1]

import http from "node:http";
import { createHash } from "node:crypto";
import mineflayer from "mineflayer";
import mcDataFactory from "minecraft-data";
import Vec3 from "vec3";
import { Movements, goals, pathfinder } from "./src/pathfinder.mjs";

const ADAPTER = "minecraft-live";
const args = parseArgs(process.argv.slice(2));
const port = Number(args.port ?? process.env.WORLD_ADAPTER_PORT ?? 8789);
const host = args.host ?? process.env.MC_HOST ?? "127.0.0.1";
const mcPort = Number(args["mc-port"] ?? process.env.MC_PORT ?? 25566);
const name = args.name ?? process.env.MC_USER ?? "AxisBody";
const version = args.version ?? process.env.MC_VERSION ?? "1.20.1";
const reach = 3;

function log(message) {
	console.log(`[adapter ${new Date().toISOString().slice(11, 19)}] ${message}`);
}

const bot = mineflayer.createBot({ host, port: mcPort, username: name, auth: "offline", version });
bot.loadPlugin(pathfinder);

let ready = false;
let movements = null;
let lastSession = null;

bot.once("spawn", async () => {
	try {
		await bot.waitForChunksToLoad();
	} catch (error) {
		log(`chunks did not load: ${error.message}`);
	}
	const mc = mcDataFactory(bot.version);
	movements = new Movements(bot, mc);
	movements.canDig = false;
	movements.allow1by1towers = false;
	movements.scafoldingBlocks = [];
	bot.pathfinder.setMovements(movements);
	ready = true;
	lastSession = {
		spawned: new Date().toISOString(),
		position: point(bot.entity.position),
		server: `${host}:${mcPort}`,
		version: bot.version,
		username: bot.username,
	};
	log(`body online as ${bot.username} at ${fmt(bot.entity.position)} on ${host}:${mcPort}`);
});

bot.on("kicked", (reason) => {
	log(`kicked: ${typeof reason === "string" ? reason : JSON.stringify(reason)}`);
	ready = false;
});
bot.on("error", (error) => log(`error: ${error.message}`));
bot.on("end", (reason) => {
	log(`end: ${reason ?? "connection closed"}`);
	ready = false;
});

/**
 * The world as the server last told us: the body's position, and every block within reach in the
 * loaded chunks. Blocks still unloaded are named `unloaded` rather than guessed, so a digest can
 * never stand for ground nobody sent.
 */
function sample() {
	const position = bot.entity?.position;
	if (!position) {
		return null;
	}
	const base = { x: Math.floor(position.x), y: Math.floor(position.y), z: Math.floor(position.z) };
	const cells = [];
	for (let dx = -reach; dx <= reach; dx++) {
		for (let dy = -1; dy <= reach; dy++) {
			for (let dz = -reach; dz <= reach; dz++) {
				const at = new Vec3(base.x + dx, base.y + dy, base.z + dz);
				let state = "unloaded";
				try {
					const block = bot.blockAt(at);
					if (block) {
						state = block.name;
					}
				} catch {
					// A chunk replaced mid-read keeps the previous word: unloaded.
				}
				cells.push([dx, dy, dz, state]);
			}
		}
	}
	const body = {
		server: `${host}:${mcPort}`,
		version: bot.version ?? version,
		position: [round(position.x), round(position.y), round(position.z)],
		onGround: Boolean(bot.entity?.onGround),
		cells,
	};
	return { body, digest: "sha256:" + createHash("sha256").update(JSON.stringify(body)).digest("hex") };
}

function observation(action, actor) {
	const read = sample();
	if (!read) {
		return null;
	}
	return {
		adapter: ADAPTER,
		version,
		actor,
		action,
		state: {
			server: read.body.server,
			minecraft: read.body.version,
			position: { x: read.body.position[0], y: read.body.position[1], z: read.body.position[2] },
			on_ground: read.body.onGround,
			cells: read.body.cells.length,
			sample: "minecraft-server chunk and block-update packets",
		},
		digest: read.digest,
	};
}

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

async function move(input) {
	const target = new Vec3(Number(input?.x ?? 0), Number(input?.y ?? 0), Number(input?.z ?? 0));
	const from = bot.entity.position.clone();
	let walked = false;
	try {
		await Promise.race([
			bot.pathfinder.goto(new goals.GoalNear(target.x, target.y, target.z, 1.2)),
			sleep(9000).then(() => { throw new Error("walk timed out after 9000ms"); }),
		]);
		walked = true;
	} catch (error) {
		// Pathfinder can refuse a target inside a block; a straight walk is still ordinary input.
		try {
			const dx = target.x - from.x;
			const dz = target.z - from.z;
			await bot.look(Math.atan2(-dx, -dz), 0, true);
			bot.setControlState("forward", true);
			await sleep(Math.min(3000, 600 * Math.hypot(dx, dz)));
		} finally {
			bot.clearControlStates();
		}
		log(`pathfinder refused (${error.message}); walked forward instead`);
	}
	await sleep(400);
	const moved = bot.entity.position.distanceTo(from);
	if (moved < 0.5) {
		return { refused: "world-not-moved", why: `the body stayed within ${moved.toFixed(2)} blocks of ${fmt(from)}` };
	}
	return { walked, moved: round(moved) };
}

async function dig(input) {
	const at = new Vec3(Number(input?.x ?? 0), Number(input?.y ?? 0), Number(input?.z ?? 0));
	const block = bot.blockAt(at);
	if (!block) {
		return { refused: "world-frame-field", why: `no block is loaded at ${fmt(at)}` };
	}
	if (block.name === "air" || block.name === "cave_air") {
		return { refused: "world-not-dug", why: `there is already no block at ${fmt(at)}` };
	}
	const before = block.name;
	const distance = bot.entity.position.distanceTo(at);
	if (distance > 5.5) {
		return { refused: "world-not-dug", why: `the body is ${distance.toFixed(1)} blocks from ${fmt(at)}` };
	}
	try {
		await bot.lookAt(at.offset(0.5, 0.5, 0.5), true);
		await Promise.race([
			bot.dig(block, true),
			sleep(8000).then(() => { throw new Error("dig timed out after 8000ms"); }),
		]);
	} catch (error) {
		return { refused: "world-not-dug", why: error.message };
	}
	await sleep(500);
	const after = bot.blockAt(at);
	if (after && after.name !== "air" && after.name !== "cave_air") {
		return { refused: "world-not-dug", why: `the server still reports ${after.name} at ${fmt(at)}` };
	}
	return { dug: before, at: fmt(at) };
}

function body(payload) {
	const action = String(payload?.action ?? "").trim();
	const actor = String(payload?.actor ?? "").trim();
	if (!["move", "dig"].includes(action)) {
		return { refused: "world-frame-field", why: `no motor for ${action || "(none)"}` };
	}
	return { action, actor, input: payload?.input ?? {} };
}

async function act(payload) {
	if (!ready || !bot.entity) {
		return { refused: "body-offline", why: "the body has no live session on the server" };
	}
	const parsed = body(payload);
	if (parsed.refused) {
		return parsed;
	}
	const before = sample();
	const outcome = parsed.action === "move" ? await move(parsed.input) : await dig(parsed.input);
	if (outcome.refused) {
		return outcome;
	}
	const after = observation(parsed.action, parsed.actor);
	if (!after) {
		return { refused: "body-offline", why: "the world could not be read after the act" };
	}
	if (before && before.digest === after.digest) {
		return { refused: "world-not-observed", why: "the world's identity is unchanged after the act" };
	}
	return { ...after, facts: outcome, before_digest: before?.digest ?? "" };
}

function respond(response, status, payload) {
	const text = JSON.stringify(payload);
	response.writeHead(status, { "content-type": "application/json", "content-length": Buffer.byteLength(text) });
	response.end(text);
}

const server = http.createServer((request, reply) => {
	const route = new URL(request.url, "http://127.0.0.1");
	if (request.method === "GET" && route.pathname === "/version") {
		respond(reply, 200, {
			adapter: ADAPTER,
			version,
			crate: `minecraft-server ${version} (${host}:${mcPort})`,
			ready,
			session: lastSession,
		});
		return;
	}
	if (request.method === "GET" && route.pathname === "/state") {
		if (!ready || !bot.entity) {
			respond(reply, 409, { refused: "body-offline", why: "the body has no live session on the server" });
			return;
		}
		const read = observation("state", "");
		respond(reply, 200, read ?? { refused: "body-offline", why: "no position from the server" });
		return;
	}
	if (request.method === "POST" && route.pathname === "/act") {
		let text = "";
		request.on("data", (chunk) => { text += chunk; });
		request.on("end", async () => {
			let payload;
			try {
				payload = JSON.parse(text || "{}");
			} catch (error) {
				respond(reply, 400, { refused: "world-frame-field", why: error.message });
				return;
			}
			try {
				const answer = await act(payload);
				respond(reply, answer.refused ? 409 : 200, answer);
			} catch (error) {
				respond(reply, 500, { refused: "world-sample", why: error.message });
			}
		});
		return;
	}
	respond(reply, 404, { refused: "route-unknown", detail: route.pathname });
});

server.listen(port, "127.0.0.1", () => {
	log(`${ADAPTER} ${version} on 127.0.0.1:${port}, body ${name} -> ${host}:${mcPort}`);
});

for (const signal of ["SIGINT", "SIGTERM"]) {
	process.on(signal, () => {
		try {
			bot.quit("adapter stopped");
		} catch {
			// already gone
		}
		server.close(() => process.exit(0));
		setTimeout(() => process.exit(0), 800);
	});
}

function point(position) {
	return { x: round(position.x), y: round(position.y), z: round(position.z) };
}

function round(value) {
	return typeof value === "number" ? Number(value.toFixed(2)) : null;
}

function fmt(position) {
	return `${Math.floor(position.x)},${Math.floor(position.y)},${Math.floor(position.z)}`;
}

function parseArgs(argv) {
	const out = {};
	for (let index = 0; index < argv.length; index++) {
		if (!argv[index].startsWith("--")) {
			continue;
		}
		const key = argv[index].slice(2);
		const value = argv[index + 1] && !argv[index + 1].startsWith("--") ? argv[++index] : "true";
		out[key] = value;
	}
	return out;
}
