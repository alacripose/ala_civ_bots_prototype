// Probe: does the body's reader hold the axis package API to its own rules?
//
// Reads the world's stream ledger (not a fixture), validates every decision package the named
// being has on record against the coverage table the axis package publishes, and then plants four
// damaged packages to show that each refusal the crate names is actually raised. Exits non-zero
// when a package that was admitted does not validate, so a passing run is evidence rather than a
// report.
//
//   node tools/residents/probe-axis.mjs [--agent Ash] [--bytes 8000000]
import fs from "node:fs";
import path from "node:path";
import { readDecision, framedDigest } from "./src/axis.mjs";
import { verbs, PROJECT_ROOT } from "./src/verbs.mjs";

const args = Object.fromEntries(
	process.argv.slice(2).reduce((pairs, arg, index, argv) => {
		if (arg.startsWith("--")) {
			const value = argv[index + 1] && !argv[index + 1].startsWith("--") ? argv[index + 1] : "true";
			pairs.push([arg.slice(2), value]);
		}
		return pairs;
	}, []),
);
const agent = args.agent ?? "Ash";
const bytes = Number(args.bytes ?? 8_000_000);
const ledger = path.join(PROJECT_ROOT, "run/stream/ledger.jsonl");

if (!fs.existsSync(ledger)) {
	console.error(`probe: no ledger at ${ledger}`);
	process.exit(1);
}
const size = fs.statSync(ledger).size;
const start = Math.max(0, size - bytes);
const length = size - start;
const buffer = Buffer.alloc(length);
const fd = fs.openSync(ledger, "r");
try {
	fs.readSync(fd, buffer, 0, length, start);
} finally {
	fs.closeSync(fd);
}

let coverage = null;
const packages = [];
for (const line of buffer.toString("utf8").split(/\r?\n/)) {
	if (!line.includes("civilian.")) {
		continue;
	}
	let row;
	try {
		row = JSON.parse(line);
	} catch {
		continue;
	}
	if (!coverage && line.includes("civilian.axis-cells/1")) {
		const candidates = [row.detail, row.detail?.payload, row.detail?.package];
		for (const candidate of candidates) {
			if (candidate && Array.isArray(candidate.coverage)) {
				const table = new Map();
				for (const entry of candidate.coverage) {
					table.set(entry.axis, new Set(entry.values.map(String)));
				}
				if (table.has("establishment")) {
					coverage = table;
					break;
				}
			}
		}
	}
	if (row.actor === agent && line.includes("civilian.decision/1")) {
		packages.push(row);
	}
}

if (!coverage) {
	console.error("probe: no axis package (civilian.axis-cells/1) in the ledger tail");
	process.exit(1);
}
if (!packages.length) {
	console.error(`probe: no decision package for ${agent} in the ledger tail`);
	process.exit(1);
}

const refusals = new Map();
const verbCounts = new Map();
const basis = new Map();
let stood = 0;
let notAdmitted = 0;
let violations = 0;
for (const row of packages) {
	const reading = readDecision(row.detail, coverage);
	// Packages written before the verb library existed carry no verb; the body names the act from
	// the skill either way, so the probe reads it the way `main.mjs`'s `named()` does.
	reading.verb = reading.verb || verbs.verbFor(reading.skill);
	reading.verb_basis = reading.verb_basis || verbs.basis(reading.skill);
	const label = `${reading.admitted ? "admitted" : "refused"}:${reading.refusals.join("+") || "clean"}`;
	verbCounts.set(reading.verb || "(none)", (verbCounts.get(reading.verb || "(none)") ?? 0) + 1);
	basis.set(reading.verb_basis || "(none)", (basis.get(reading.verb_basis || "(none)") ?? 0) + 1);
	if (reading.admitted && reading.refusals.length) {
		violations++;
		console.error(`probe: admitted package failed validation: ${reading.decision} ${reading.refusals}`);
	}
	for (const code of reading.refusals) {
		refusals.set(code, (refusals.get(code) ?? 0) + 1);
	}
	if (reading.stands) {
		stood++;
	} else if (!reading.admitted) {
		notAdmitted++;
	}
}

const sample = readDecision(packages[packages.length - 1].detail, coverage);
sample.verb = sample.verb || verbs.verbFor(sample.skill);
sample.verb_basis = sample.verb_basis || verbs.basis(sample.skill);

// Four damaged packages, one per rule. Each must be refused by the code the crate names.
const base = JSON.parse(JSON.stringify(packages[packages.length - 1].detail));
function damaged(mutate) {
	const copy = JSON.parse(JSON.stringify(base));
	mutate(copy.payload, copy.payload.proof);
	return readDecision(copy, coverage).refusals;
}
const plants = [
	["bare token", damaged((payload) => { payload.token = "PASS"; })],
	["misfiled token", damaged((payload) => { payload.token = "establishment active"; })],
	["no witness", damaged((payload) => { payload.witness = ""; payload.proof.checked = false; })],
	// The bytes change and the digest does not: this is the plant that must be caught, because the
	// proof would otherwise be shown against an artifact it was not made from.
	["tampered bytes", damaged((payload, proof) => {
		proof.artifact_bytes = `${proof.artifact_bytes}|tampered`;
	})],
];
let plantsHeld = 0;
for (const [name, codes] of plants) {
	const held = codes.length > 0;
	if (held) {
		plantsHeld++;
	}
	console.log(`plant ${held ? "REFUSED" : "ACCEPTED"} ${name}: ${codes.join(",") || "(none)"}`);
}

const histogram = (map) => [...map.entries()].sort((a, b) => b[1] - a[1]).slice(0, 8)
	.map(([key, count]) => `${key}=${count}`).join(" ");

// The speech gate, checked on the two lines the live body actually met: the line the server was
// speaking before the plural fix, and the same line after it. The body must refuse the first by
// name and pass the second.
const invented = "I have 9 spruce plankss and 20 food.";
const repaired = "I have 9 spruce planks and 20 food.";
const inventedMissing = verbs.unattestedWords(invented);
const repairedMissing = verbs.unattestedWords(repaired);
const gateHeld = inventedMissing.length === 1 && inventedMissing[0] === "plankss"
	&& repairedMissing.length === 0;
console.log(`speech gate: invented -> [${inventedMissing.join(",")}] ` +
	`repaired -> [${repairedMissing.join(",")}] ${gateHeld ? "HELD" : "BROKEN"}`);

console.log(`ledger ${ledger} (${(size / 1e6).toFixed(1)} MB, read ${(length / 1e6).toFixed(1)} MB)`);
console.log(`vocabulary: ${coverage.size} ladders ` +
	[...coverage.entries()].map(([ladder, values]) => `${ladder}:${values.size}`).join(" "));
console.log(`packages for ${agent}: ${packages.length} stood=${stood} not-admitted=${notAdmitted} violations=${violations}`);
console.log(`refusals: ${histogram(refusals) || "(none)"}`);
console.log(`verbs spoken: ${histogram(verbCounts)}`);
console.log(`verb basis: ${histogram(basis)}`);
console.log(`newest reading: ${sample.decision} tick=${sample.tick} skill=${sample.skill} ` +
	`verb=${sample.verb || "(none)"} token="${sample.token}" establishment="${sample.establishment}" ` +
	`witness=${sample.witness} digest_ok=${sample.digest_ok} stands=${sample.stands}`);
console.log(`verb library: ${verbs.size()} induced verbs from ${verbs.source}`);

const ok = violations === 0 && plantsHeld === 4 && stood > 0 && gateHeld;
console.log(ok ? "PROBE PASS" : "PROBE FAIL");
process.exit(ok ? 0 : 1);
