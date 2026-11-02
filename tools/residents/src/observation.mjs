// Per-body client evidence for the motor bridge.
//
// The server owns authoritative state and the Java mind owns decisions. This process owns the only
// view from inside the actual client session. Every frame records what this body could see and what
// sounds this client received while executing it. That is deliberately separate from the server's
// world view: it proves the rendering/audio side of the body, not just that a command was issued.
import fs from "node:fs";
import path from "node:path";
import Vec3 from "vec3";

export class ClientObservation {
	constructor({ bot, name, directory, log = () => {} }) {
		this.bot = bot;
		this.name = name;
		this.directory = directory;
		this.file = path.join(directory, `${name}.client.jsonl`);
		this.log = log;
		this.active = null;
		this.audio = [];
		this.bind();
	}

	bind() {
		this.bot.on("soundEffectHeard", (soundName, position, volume, pitch) => {
			this.sound({
				kind: "named",
				sound: String(soundName),
				position: point(position),
				volume,
				pitch,
			});
		});
		this.bot.on("hardcodedSoundEffectHeard", (soundId, category, position, volume, pitch) => {
			this.sound({
				kind: "hardcoded",
				id: Number(soundId),
				category: String(category),
				position: point(position),
				volume,
				pitch,
			});
		});
		this.bot.on("chat", (username, message) => this.chat(username, message));
		this.bot.on("message", (jsonMsg, position) => {
			try {
				this.append("ui", {
					channel: "system",
					position: String(position ?? ""),
					text: jsonMsg.toString(),
				});
			} catch {
				// a malformed system message must not stop the motor
			}
		});
	}

	session(reason = "spawn") {
		this.append("session", { reason, rendering: this.render() });
	}

	startFrame(command) {
		this.active = { id: command.id, kind: command.kind };
		this.audio = [];
		this.append("frame-start", {
			command: command.id,
			kind: command.kind,
			rendering: this.render(),
		});
	}

	finishFrame(command, result) {
		this.append("frame-end", {
			command: command.id,
			kind: command.kind,
			rendering: this.render(),
			audio: this.audio,
			result: {
				status: result.status,
				code: result.code,
				detail: result.detail,
			},
		});
		this.active = null;
		this.audio = [];
	}

	chat(username, message) {
		this.append("ui", { channel: "chat", username, text: message });
	}

	said(id, text) {
		this.append("speech", { command: id, text, transport: "PLAYER_CHAT_PACKET" });
	}

	sound(event) {
		const row = { ...event, command: this.active?.id ?? "" };
		if (this.active) {
			this.audio.push(row);
		}
		this.append("audio", row);
	}

	render() {
		const bot = this.bot;
		const entity = bot.entity;
		const position = entity?.position ?? null;
		const nearbyBlocks = [];
		if (position) {
			const base = {
				x: Math.floor(position.x),
				y: Math.floor(position.y),
				z: Math.floor(position.z),
			};
			for (let dx = -2; dx <= 2; dx++) {
				for (let dy = -1; dy <= 2; dy++) {
					for (let dz = -2; dz <= 2; dz++) {
						if (nearbyBlocks.length >= 36) {
							break;
						}
						const at = new Vec3(base.x + dx, base.y + dy, base.z + dz);
						const block = bot.blockAt(at);
						if (block && block.name !== "air" && block.name !== "cave_air") {
							nearbyBlocks.push({
								x: at.x,
								y: at.y,
								z: at.z,
								block: block.name,
							});
						}
					}
				}
			}
		}
		const entities = Object.values(bot.entities ?? {})
			.filter((other) => other && other !== entity && other.position)
			.map((other) => ({
				name: other.name ?? other.username ?? other.type ?? "unknown",
				distance: position ? round(other.position.distanceTo(position)) : null,
				position: point(other.position),
			}))
			.sort((a, b) => (a.distance ?? 0) - (b.distance ?? 0))
			.slice(0, 12);
		return {
			mode: "logical-headless",
			position: point(position),
			yaw: round(entity?.yaw),
			pitch: round(entity?.pitch),
			dimension: bot.game?.dimension ?? "",
			health: bot.health ?? null,
			food: bot.food ?? null,
			air: bot.air ?? null,
			held: bot.heldItem?.name ?? "",
			hotbar: bot.quickBarSlot ?? null,
			crosshair: block(bot.blockAtCursor?.(4.5)),
			screen: bot.currentWindow
				? {
					title: String(bot.currentWindow.title ?? ""),
					slots: bot.currentWindow.slots?.length ?? 0,
				}
				: null,
			nearbyBlocks,
			nearbyEntities: entities,
		};
	}

	append(type, data) {
		fs.mkdirSync(this.directory, { recursive: true });
		const row = {
			schema: "ensouled.client/1",
			body: this.name,
			type,
			at: new Date().toISOString(),
			...data,
		};
		fs.appendFileSync(this.file, `${JSON.stringify(row)}\n`);
	}
}

function point(position) {
	if (!position) {
		return null;
	}
	return {
		x: round(position.x),
		y: round(position.y),
		z: round(position.z),
	};
}

function block(blockAt) {
	if (!blockAt) {
		return null;
	}
	return {
		name: blockAt.name,
		position: point(blockAt.position),
	};
}

function round(value) {
	return typeof value === "number" ? Number(value.toFixed(3)) : null;
}
