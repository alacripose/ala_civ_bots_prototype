// The axis suite's package API, as the body reads it.
//
// The server owns the decision tree and writes each decision as a `civilian.decision/1` package
// into the world's stream ledger. This module does not decide anything: it reads those packages,
// checks them against the coverage table the axis package itself publishes (the nine ladders and
// their members), and refuses to call an unchecked reading a pass. A package that does not check
// is named by the crate's own refusal codes so the body's record and the server's row agree.
//
// The vocabulary is never written down here. The ladders arrive in the newest `civilian.axis-cells/1`
// package; if the ledger carries none, validation answers `UncoveredAxis` and the reading is
// recorded as unchecked rather than silently accepted.
import fs from "node:fs";
import crypto from "node:crypto";

const DOMAIN = "planet-artifact-v1";
const SCHEMA = 1;
const DECISION_FORMAT = "civilian.decision/1";
const AXIS_FORMAT = "civilian.axis-cells/1";
const TICKET_FORMAT = "civilian.ticket/1";
/** How far back to look for the newest axis package when the client starts. */
const TAIL_SCAN_BYTES = 8 * 1024 * 1024;
/** The establishment ladder is required; a coverage row for it is not optional. */
const ESTABLISHMENT_LADDER = "establishment";

/**
 * The framed digest the axis proof uses, byte-identical to `AxisProof.digest`:
 * domain, little-endian u16 schema, little-endian u64 length, then the bytes.
 */
export function framedDigest(bytes) {
	const domain = Buffer.from(DOMAIN, "utf8");
	const header = Buffer.alloc(domain.length + 2 + 8);
	domain.copy(header, 0);
	header.writeUInt16LE(SCHEMA, domain.length);
	header.writeBigUInt64LE(BigInt(bytes.length), domain.length + 2);
	return `sha256:${crypto.createHash("sha256").update(Buffer.concat([header, bytes])).digest("hex")}`;
}

/** Split a qualified ladder token: `procedural_gate PASS` -> ladder and value, or null. */
export function splitToken(token) {
	const text = String(token ?? "").trim();
	const space = text.indexOf(" ");
	if (space < 0) {
		return null;
	}
	return { ladder: text.slice(0, space), value: text.slice(space + 1) };
}

/**
 * Check one qualified token against the published coverage. Returns a refusal code or null.
 * A bare token is the bug the axis vocabulary names; a token filed under another ladder is
 * refused rather than compared across ladders.
 */
export function checkToken(coverage, ladder, token) {
	const split = splitToken(token);
	if (!split) {
		return "BareTokenRefused";
	}
	if (!coverage.has(split.ladder)) {
		return "UncoveredAxis";
	}
	if (split.ladder !== ladder) {
		return "AxisInterchangeRefused";
	}
	if (!coverage.get(split.ladder).has(split.value)) {
		return "UncoveredAxis";
	}
	return null;
}

/**
 * Read one decision package as a reading. `coverage` is the ladders' own table; `null` means no
 * axis package has been read, and nothing can be certified from nothing.
 */
export function readDecision(envelope, coverage) {
	const payload = envelope?.payload ?? {};
	const proof = payload.proof ?? {};
	const refusals = [];
	if (payload.format !== DECISION_FORMAT) {
		refusals.push("FormatRefused");
	}
	if (!coverage || coverage.size === 0) {
		refusals.push("UncoveredAxis");
	} else {
		const gate = checkToken(coverage, "procedural_gate", payload.token);
		if (gate) {
			refusals.push(gate);
		}
		const establishment = checkToken(coverage, ESTABLISHMENT_LADDER, payload.establishment);
		if (establishment) {
			refusals.push(establishment);
		}
	}
	const witness = String(payload.witness ?? "").trim();
	if (!witness || proof.checked !== true) {
		// No witness is `unchecked`: a state, never a pass.
		refusals.push("UncheckedReading");
	}
	let digestOk = false;
	if (typeof proof.artifact_bytes === "string" && typeof proof.artifact_digest === "string") {
		try {
			digestOk = framedDigest(Buffer.from(proof.artifact_bytes, "utf8")) === proof.artifact_digest;
		} catch {
			digestOk = false;
		}
	}
	if (!digestOk) {
		refusals.push("DigestMismatch");
	}
	const admitted = payload.admitted === true;
	return {
		decision: envelope?.message_id ?? "",
		tick: Number(payload.tick ?? 0),
		skill: String(payload.skill ?? ""),
		subject: String(payload.subject ?? ""),
		verb: String(payload.verb ?? ""),
		verb_basis: String(payload.verb_basis ?? ""),
		token: String(payload.token ?? ""),
		establishment: String(payload.establishment ?? ""),
		admitted,
		checked: witness.length > 0 && proof.checked === true,
		witness,
		digest_ok: digestOk,
		refusals,
		/** A reading that can be relied on: admitted, qualified, witnessed and hashing to itself. */
		stands: admitted && refusals.length === 0,
	};
}

/**
 * Read one ticket package as a claim about work. The claim has to name its state, stay inside its
 * own declared cell budget, hash to itself, and — when it says HELD — cite an observed count. A
 * HELD verdict with nothing observed is `CompletionByDeclaration`, which is the one answer a
 * ticket may never give.
 */
export function readTicket(pkg) {
	const refusals = [];
	if (pkg?.format !== TICKET_FORMAT) {
		refusals.push("FormatRefused");
	}
	const state = String(pkg?.state ?? "");
	if (!["OPEN", "HELD", "ABSENT", "REFUSED"].includes(state)) {
		refusals.push("TicketStateRefused");
	}
	const declared = Number(pkg?.cells?.declared ?? -1);
	const used = Number(pkg?.cells?.used ?? -1);
	if (!Number.isInteger(declared) || declared < 1 || !Number.isInteger(used) || used < 0 || used > declared) {
		refusals.push("CellBudgetRefused");
	}
	const witness = String(pkg?.witness ?? "").trim();
	if (!witness && state !== "OPEN") {
		refusals.push("UncheckedReading");
	}
	const evidence = Array.isArray(pkg?.evidence) ? pkg.evidence.map(String) : [];
	if (state === "HELD" && !/^(held|placed) \d+\/\d+/.test(witness)) {
		refusals.push("CompletionByDeclaration");
	}
	let digestOk = false;
	if (typeof pkg?.artifact === "string" && typeof pkg?.digest === "string") {
		try {
			digestOk = framedDigest(Buffer.from(pkg.artifact, "utf8")) === pkg.digest;
		} catch {
			digestOk = false;
		}
	}
	if (!digestOk) {
		refusals.push("DigestMismatch");
	}
	return {
		ticket: String(pkg?.ticket ?? ""),
		commitment: String(pkg?.commitment ?? ""),
		presenter: String(pkg?.presenter ?? ""),
		state,
		declared,
		used,
		witness,
		evidence,
		digest_ok: digestOk,
		refusals,
		/** A reading that can be relied on: a live or held ticket that passes every rule. */
		stands: refusals.length === 0 && (state === "OPEN" || state === "HELD"),
	};
}

/**
 * One body's view of the ledger: the published coverage, and the newest decision standing over a
 * frame. Reads only what was appended since the last poll, so a 50 MB ledger costs one tail scan
 * at startup and then the new lines alone.
 */
export class DecisionStream {
	constructor({ file, agent, log = () => {} }) {
		this.file = file;
		this.agent = agent;
		this.log = log;
		this.coverage = null;
		this.latest = null;
		this.latestTicket = null;
		this.seen = 0;
		this.ticketsSeen = 0;
		this.ticketsRefused = 0;
		this.refused = 0;
		this.offset = 0;
		this.remainder = "";
		this.fd = null;
	}

	open() {
		this.coverage = this.readVocabulary();
		if (!this.coverage) {
			this.log("axis", `no ${AXIS_FORMAT} package in ${this.file}; the vocabulary is absent`);
		} else {
			this.log("axis", `vocabulary read: ${this.coverage.size} ladder(s)`);
		}
		if (!fs.existsSync(this.file)) {
			this.log("axis", `ledger ${this.file} does not exist; no decision will be read`);
			return;
		}
		this.fd = fs.openSync(this.file, "r");
		// Start at the end: the frame being executed now is explained by the newest decision, and
		// replaying 22k historical decisions would be a record, not a reading.
		this.offset = fs.fstatSync(this.fd).size;
	}

	readVocabulary() {
		if (!fs.existsSync(this.file)) {
			return null;
		}
		// statSync takes the path; fstatSync takes the open fd and is used only after open().
		const size = fs.statSync(this.file).size;
		const start = Math.max(0, size - TAIL_SCAN_BYTES);
		const length = size - start;
		const buffer = Buffer.alloc(length);
		const fd = fs.openSync(this.file, "r");
		try {
			fs.readSync(fd, buffer, 0, length, start);
		} finally {
			fs.closeSync(fd);
		}
		const lines = buffer.toString("utf8").split(/\r?\n/);
		for (let index = lines.length - 1; index >= 0; index--) {
			const line = lines[index];
			if (!line.includes(AXIS_FORMAT)) {
				continue;
			}
			let row;
			try {
				row = JSON.parse(line);
			} catch {
				continue;
			}
			const coverage = coverageOf(row);
			if (coverage) {
				return coverage;
			}
		}
		return null;
	}

	/** Read the lines appended since the last poll. A partial trailing line waits for its rest. */
	poll() {
		if (this.fd === null) {
			return;
		}
		const size = fs.fstatSync(this.fd).size;
		if (size <= this.offset) {
			return;
		}
		const length = size - this.offset;
		const buffer = Buffer.alloc(length);
		fs.readSync(this.fd, buffer, 0, length, this.offset);
		this.offset = size;
		const text = this.remainder + buffer.toString("utf8");
		const lines = text.split(/\r?\n/);
		this.remainder = lines.pop() ?? "";
		for (const line of lines) {
			if (!line.includes("civilian.")) {
				continue;
			}
			let row;
			try {
				row = JSON.parse(line);
			} catch {
				continue;
			}
			this.ingest(row);
		}
	}

	ingest(row) {
		if (row?.actor !== this.agent) {
			return;
		}
		const envelope = row.detail;
		if (envelope?.format === TICKET_FORMAT) {
			const ticket = readTicket(envelope);
			ticket.recorded_at = row.at ?? "";
			ticket.ledger_seq = row.seq ?? 0;
			this.ticketsSeen++;
			if (!ticket.stands) {
				this.ticketsRefused++;
				this.log("ticket", `refused ${ticket.ticket}: ${ticket.refusals.join(",")}`);
				return;
			}
			this.latestTicket = ticket;
			return;
		}
		if (envelope?.payload?.agent !== this.agent) {
			return;
		}
		if (envelope?.payload?.format !== DECISION_FORMAT) {
			// Observations and other civilian rows are not decisions: a lane that scores every
			// package as a decision reports refusals for rows that never claimed to be one.
			return;
		}
		const reading = readDecision(envelope, this.coverage);
		reading.recorded_at = row.at ?? "";
		reading.ledger_seq = row.seq ?? 0;
		this.seen++;
		if (!reading.stands) {
			this.refused++;
			this.log("axis", `refused ${reading.decision}: ${reading.refusals.join(",")}`);
			return;
		}
		this.latest = reading;
	}

	/** The newest decision that stands over a frame, or null when none has been read. */
	reading() {
		return this.latest;
	}

	/** The newest ticket that stands, or null when none has been read. */
	ticket() {
		return this.latestTicket;
	}

	summary() {
		return {
			vocabulary: this.coverage ? this.coverage.size : 0,
			decisions_seen: this.seen,
			decisions_refused: this.refused,
			tickets_seen: this.ticketsSeen,
			tickets_refused: this.ticketsRefused,
		};
	}

	close() {
		if (this.fd !== null) {
			fs.closeSync(this.fd);
			this.fd = null;
		}
	}
}

/** The ladders from an axis package, wherever the row carries the table. */
function coverageOf(row) {
	const candidates = [row?.detail, row?.detail?.payload, row?.detail?.package, row?.detail?.detail];
	for (const candidate of candidates) {
		if (!candidate || !Array.isArray(candidate.coverage)) {
			continue;
		}
		const table = new Map();
		for (const entry of candidate.coverage) {
			if (entry && typeof entry.axis === "string" && Array.isArray(entry.values)) {
				table.set(entry.axis, new Set(entry.values.map(String)));
			}
		}
		if (table.has(ESTABLISHMENT_LADDER)) {
			return table;
		}
	}
	return null;
}
