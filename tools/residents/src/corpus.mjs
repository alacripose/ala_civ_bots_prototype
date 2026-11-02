// The corpus this resident thinks and speaks from.
//
// Nothing in this file chooses a word for a meaning: the compiled lexicon answers with the word
// the corpus actually uses, and a meaning it never attests is unsayable. That is the same rule the
// JVM's Lexicon/Realizer follow, applied to a resident whose mind runs outside the server.
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const HERE = path.dirname(fileURLToPath(import.meta.url));
export const PROJECT_ROOT = path.resolve(HERE, "../../..");
const RESOURCES = path.join(PROJECT_ROOT, "src/main/resources/ensouled");

const TOKEN = /[a-z]+(?:'[a-z]+)?|\d+/g;

function readTsv(file, onRow) {
	if (!fs.existsSync(file)) {
		throw new Error(`corpus file missing: ${file}`);
	}
	for (const raw of fs.readFileSync(file, "utf8").split(/\r?\n/)) {
		const line = raw.trim();
		if (!line || line.startsWith("#")) {
			continue;
		}
		onRow(line.split("\t"));
	}
}

export class Corpus {
	constructor(directory = RESOURCES) {
		this.lexicon = new Map();
		this.baseline = new Map();
		this.cues = new Map();
		this.readLexicon(path.join(directory, "prose_lexicon.tsv"));
		this.readBaseline(path.join(directory, "prose_baseline.tsv"));
		this.readCues(path.join(directory, "prose_cues.tsv"));
		if (!this.baseline.has("EXECUTE")) {
			throw new Error("corpus has no EXECUTE procedure shape; a task cannot be stated");
		}
	}

	readLexicon(file) {
		readTsv(file, (parts) => {
			if (parts.length >= 2) {
				const count = Number.parseInt(parts[1], 10);
				if (Number.isFinite(count)) {
					this.lexicon.set(parts[0], count);
				}
			}
		});
	}

	readBaseline(file) {
		readTsv(file, (parts) => {
			if (parts.length >= 4) {
				this.baseline.set(parts[0], {
					verb: parts[0],
					kind: parts[1],
					modality: parts[2],
					occurrences: Number.parseInt(parts[3], 10) || 0,
				});
			}
		});
	}

	readCues(file) {
		readTsv(file, (parts) => {
			if (parts.length >= 2) {
				const lift = parts.length > 2 ? Number.parseFloat(parts[2]) : 1;
				if (!this.cues.has(parts[0])) {
					this.cues.set(parts[0], new Map());
				}
				this.cues.get(parts[0]).set(parts[1], Number.isFinite(lift) ? lift : 1);
			}
		});
	}

	count(word) {
		return this.lexicon.get(String(word).toLowerCase()) ?? 0;
	}

	attests(word) {
		return this.count(word) > 0;
	}

	/** The corpus's own word for a meaning: the most-used attested candidate, or null. */
	prefer(candidates) {
		let best = null;
		for (const candidate of candidates) {
			const word = String(candidate).toLowerCase();
			const count = this.count(word);
			if (count > 0 && (best === null || count > best.count)) {
				best = { word, count };
			}
		}
		return best;
	}

	pair(verb) {
		return this.baseline.get(String(verb).toUpperCase()) ?? null;
	}

	/** The claim shape a task is stated in: a procedure the body will carry out. */
	procedure() {
		return this.baseline.get("EXECUTE");
	}

	tokens(text) {
		return String(text ?? "").toLowerCase().match(TOKEN) ?? [];
	}

	/** How strongly the corpus itself speaks of this phrase, per word. */
	salience(text) {
		const tokens = this.tokens(text);
		if (!tokens.length) {
			return 0;
		}
		let sum = 0;
		for (const token of tokens) {
			sum += Math.log1p(this.count(token));
		}
		return sum / tokens.length;
	}

	/** Every token the corpus does not attest; empty means the sentence is sayable. */
	unattested(text) {
		return this.tokens(text).filter((token) => !this.attests(token) && !/^\d+$/.test(token));
	}

	/** Classify a composed line in the corpus rule shape `<ref>: VERB [args]`. */
	classify(line) {
		const match = /^\s*(\S[^:]*):\s*([A-Z_]+)\s*\[(.*)\]\s*$/.exec(String(line ?? ""));
		if (!match) {
			return null;
		}
		const pair = this.pair(match[2]);
		if (!pair) {
			return null;
		}
		return { ref: match[1], verb: pair.verb, kind: pair.kind, modality: pair.modality, quote: match[3].trim() };
	}

	/** Compose a claim only when the corpus can read it; otherwise the caller says nothing. */
	compose(ref, verb, sentence) {
		if (!ref || !sentence || /[\[\]]/.test(sentence)) {
			return null;
		}
		const line = `${ref}: ${verb} [${sentence}]`;
		return this.classify(line) ? line : null;
	}
}

export const corpus = new Corpus();
