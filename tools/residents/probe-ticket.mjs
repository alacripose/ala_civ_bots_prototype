// Probe: do ticket packages hold their own declared bounds?
//
// Reads the world's stream ledger, validates every `civilian.ticket/1` row against the rules the
// ticket itself declares — a named state, a cell budget that is not overspent, a digest over the
// whole claim, and a HELD verdict that cites an observed count rather than a declaration — then
// plants a ticket that claims completion with no witness and shows it is refused.
//
//   node tools/residents/probe-ticket.mjs [--agent Ash] [--bytes 8000000]
import fs from "node:fs";
import path from "node:path";
import { readTicket, framedDigest } from "./src/axis.mjs";
import { PROJECT_ROOT } from "./src/verbs.mjs";

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
const buffer = Buffer.alloc(size - start);
const fd = fs.openSync(ledger, "r");
try {
	fs.readSync(fd, buffer, 0, buffer.length, start);
} finally {
	fs.closeSync(fd);
}

const failures = [];
let real = 0;
let stood = 0;
for (const line of buffer.toString("utf8").split(/\r?\n/)) {
	if (!line.includes("civilian.ticket/1")) {
		continue;
	}
	let row;
	try {
		row = JSON.parse(line);
	} catch {
		continue;
	}
	if (row.actor !== agent) {
		continue;
	}
	const pkg = row.detail?.format === "civilian.ticket/1" ? row.detail : row.detail?.payload;
	if (!pkg) {
		continue;
	}
	real++;
	const reading = readTicket(pkg);
	if (reading.stands) {
		stood++;
	} else if (reading.state === "HELD") {
		failures.push(`held ticket ${reading.ticket} did not stand: ${reading.refusals.join(",")}`);
	}
}

/** A ticket that closes HELD with no observed count is the failure this must refuse. */
function fixture(state, witness, evidence) {
	const artifact = `civilian.ticket/1|ticket:planted|commit:planted|Ash|wish:planted|harvest|${state}|1/2|${witness}|${evidence.join(",")}`;
	return {
		format: "civilian.ticket/1",
		ticket: "ticket:planted",
		commitment: "commit:planted",
		agent: "Ash",
		presenter: "Ash",
		state,
		cells: { declared: 2, used: 1 },
		witness,
		evidence,
		artifact,
		digest: framedDigest(Buffer.from(artifact, "utf8")),
	};
}

const declared = readTicket(fixture("HELD", "", []));
if (declared.stands || !declared.refusals.includes("CompletionByDeclaration")) {
	failures.push("a HELD ticket with no observed count stood");
}
const observed = readTicket(fixture("HELD", "held 1/2 at tick 40", ["intent:1"]));
if (!observed.stands) {
	failures.push(`a HELD ticket with an observed count was refused: ${observed.refusals.join(",")}`);
}
const overspent = readTicket({ ...fixture("OPEN", "", []), cells: { declared: 1, used: 2 } });
if (overspent.stands || !overspent.refusals.includes("CellBudgetRefused")) {
	failures.push("a ticket past its declared cell budget stood");
}

if (failures.length > 0) {
	for (const failure of failures) {
		console.error(`probe: FAIL ${failure}`);
	}
	process.exit(1);
}
console.log(
	`probe: PASS tickets=${real} stood=${stood} planted=3 (declared-completion refused, observed held stood, overspent refused)`,
);
