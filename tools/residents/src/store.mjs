// A resident's own durable state, held beside the world rather than inside the server process.
//
// The first boot adopts the identity and home the JVM already recorded for this name, so moving the
// mind does not mint a second person. After that this file is the authority for what the resident
// remembers and owes; the world stays the authority for what is physically true.
import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";
import { PROJECT_ROOT } from "./corpus.mjs";

const WORLD = path.join(PROJECT_ROOT, "run/world/ensouled");
const MAX_EPISODES = 240;

export class Memory {
	constructor(name) {
		this.name = name;
		this.directory = path.join(PROJECT_ROOT, "run/residents", name.toLowerCase());
		this.file = path.join(this.directory, "mind.json");
		this.journalFile = path.join(this.directory, "journal.jsonl");
		this.data = this.read();
	}

	read() {
		try {
			if (fs.existsSync(this.file)) {
				return JSON.parse(fs.readFileSync(this.file, "utf8"));
			}
		} catch (error) {
			// An unreadable mind is not replaced with a blank one: the resident starts read-only and
			// says so, rather than silently losing its history.
			throw new Error(`mind ${this.file} is unreadable: ${error.message}`);
		}
		return {
			schema: 1,
			name: this.name,
			agentId: null,
			home: null,
			bornTick: 0,
			adopted: null,
			stats: { decisions: 0, completed: 0, failed: 0 },
			fatigue: 0,
			commitment: null,
			seen: {},
			failures: {},
			episodes: [],
			lastFrame: null,
		};
	}

	/** Take the identity and home the JVM's own world record already gives this name. */
	adoptFromWorld(mc) {
		if (this.data.agentId) {
			return this.data;
		}
		let work = null;
		let domain = null;
		try {
			work = JSON.parse(fs.readFileSync(path.join(WORLD, "work.json"), "utf8"));
		} catch {
			work = null;
		}
		try {
			domain = JSON.parse(fs.readFileSync(path.join(WORLD, "domain.json"), "utf8"));
		} catch {
			domain = null;
		}
		const commitments = (work?.commitments ?? []).filter((row) => String(row.name).toLowerCase() === this.name.toLowerCase());
		const latest = commitments.sort((a, b) => (a.opened_tick ?? 0) - (b.opened_tick ?? 0)).at(-1);
		const agentId = latest?.agent_id ?? null;
		const base = (domain?.homebases ?? []).find((row) => row.owner_agent_id === agentId)
			?? (domain?.homebases ?? []).find((row) => String(row.name).toLowerCase() === this.name.toLowerCase());
		this.data.agentId = agentId ?? `agent:${crypto.randomUUID()}`;
		this.data.home = base ? { x: base.x, y: base.y, z: base.z } : null;
		this.data.adopted = {
			source: latest ? "run/world/ensouled/work.json" : "minted",
			commitment: latest?.commitment_id ?? null,
			from: latest ? "jvm" : "external",
		};
		// An obligation that is still open at the move is work this resident still owes.
		const open = commitments.filter((row) => row.state === "OPEN").at(-1);
		if (open) {
			const item = itemForKind(open.kind, mc);
			if (item) {
				this.data.commitment = {
					id: open.commitment_id,
					itemId: item.id,
					itemName: item.name,
					count: open.count,
					recipient: null,
					dueTick: open.due_tick,
					source: open.source,
				};
			}
		}
		this.save();
		return this.data;
	}

	save() {
		fs.mkdirSync(this.directory, { recursive: true });
		const temporary = `${this.file}.tmp`;
		fs.writeFileSync(temporary, JSON.stringify(this.data, null, 2));
		fs.renameSync(temporary, this.file);
	}

	journal(row) {
		fs.mkdirSync(this.directory, { recursive: true });
		fs.appendFileSync(this.journalFile, `${JSON.stringify({ at: new Date().toISOString(), ...row })}\n`);
	}

	episode(kind, text, extra = {}) {
		this.data.episodes.push({ tick: extra.tick ?? 0, kind, text, ...extra });
		if (this.data.episodes.length > MAX_EPISODES) {
			this.data.episodes.splice(0, this.data.episodes.length - MAX_EPISODES);
		}
		this.journal({ kind: "episode", episode: kind, text, ...extra });
	}

	commitment(next) {
		this.data.commitment = next;
		this.journal({ kind: "commitment", commitment: next });
		this.save();
	}

	closeCommitment(why) {
		if (this.data.commitment) {
			this.journal({ kind: "commitment-closed", commitment: this.data.commitment, why });
			this.data.commitment = null;
			this.save();
		}
	}
}

function itemForKind(kind, mc) {
	const name = {
		LOGS: "oak_log",
		PLANKS: "oak_planks",
		STONE: "cobblestone",
		BREAD: "bread",
		APPLE: "apple",
		DIRT: "dirt",
	}[kind];
	const item = name ? mc.itemsByName[name] : null;
	return item ? { id: item.id, name: item.name } : null;
}
