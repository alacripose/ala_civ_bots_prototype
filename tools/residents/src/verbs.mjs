// The dynamic verb library, as the body reads it.
//
// `src/main/resources/ensouled/prose_verbs.tsv` is compiled from the prose corpus, which is the
// training set: a word is a verb because the corpus puts it in a verb position, not because this
// project lists it. `prose_lexicon.tsv` is the vocabulary the corpus actually uses, so the body can
// refuse to speak an invented word instead of relaying one.
//
// Nothing in this file chooses vocabulary. `verbFor` reads the skill's own words and lets the
// corpus's counts rank them; `unattested` answers which words came from nowhere, so a line the
// corpus cannot say is refused by name.
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const HERE = path.dirname(fileURLToPath(import.meta.url));
export const PROJECT_ROOT = path.resolve(HERE, "../../..");
const RESOURCES = path.join(PROJECT_ROOT, "src/main/resources/ensouled");

const TOKEN = /[a-z]+(?:'[a-z]+)?|\d+/g;

function readRows(file) {
	const rows = [];
	if (!fs.existsSync(file)) {
		throw new Error(`compiled corpus missing: ${file}`);
	}
	for (const raw of fs.readFileSync(file, "utf8").split(/\r?\n/)) {
		const line = raw.trim();
		if (!line || line.startsWith("#")) {
			continue;
		}
		rows.push(line.split("\t"));
	}
	return rows;
}

export class VerbLibrary {
	constructor(directory = RESOURCES) {
		this.verbs = new Map();
		this.words = new Map();
		this.source = path.join(directory, "prose_verbs.tsv");
		for (const parts of readRows(this.source)) {
			const count = Number.parseInt(parts[1] ?? "", 10);
			if (parts[0] && Number.isFinite(count)) {
				this.verbs.set(parts[0], count);
			}
		}
		for (const parts of readRows(path.join(directory, "prose_lexicon.tsv"))) {
			const count = Number.parseInt(parts[1] ?? "", 10);
			if (parts[0] && Number.isFinite(count)) {
				this.words.set(parts[0], count);
			}
		}
	}

	size() {
		return this.verbs.size;
	}

	/** How often the corpus puts this word in a verb position. */
	count(word) {
		return this.verbs.get(String(word ?? "").toLowerCase()) ?? 0;
	}

	isVerb(word) {
		return this.count(word) > 0;
	}

	/** How often the corpus says this word at all. */
	attests(word) {
		return (this.words.get(String(word ?? "").toLowerCase()) ?? 0) > 0;
	}

	wordsOf(skillId) {
		return String(skillId ?? "").toLowerCase().split(/[^a-z]+/).filter(Boolean);
	}

	/** The verb for a skill id: `harvest_tree` answers `harvest`, ranked by the corpus's counts. */
	verbFor(skillId) {
		let best = "";
		let bestCount = 0;
		for (const word of this.wordsOf(skillId)) {
			const count = this.count(word);
			if (count > bestCount) {
				best = word;
				bestCount = count;
			}
		}
		return best;
	}

	/** The action as words a reader could hear: the attested verb first, then the remaining words. */
	phrase(skillId) {
		const words = this.wordsOf(skillId);
		const verb = this.verbFor(skillId);
		if (!verb) {
			return words.join(" ");
		}
		return [verb, ...words.filter((word) => word !== verb)].join(" ");
	}

	basis(skillId) {
		const verb = this.verbFor(skillId);
		return verb ? `lexicon:${this.count(verb)}` : "unattested";
	}

	/** Every token the corpus does not attest. Numbers are the being's own and need no word. */
	unattested(text) {
		const missing = [];
		for (const token of String(text ?? "").toLowerCase().match(TOKEN) ?? []) {
			if (/^\d+$/.test(token) || this.attests(token)) {
				continue;
			}
			if (!missing.includes(token)) {
				missing.push(token);
			}
		}
		return missing;
	}

	/** Every word of a line the library cannot name, in order: the speech gate's question. */
	unattestedWords(text) {
		const missing = [];
		for (const token of String(text ?? "").toLowerCase().match(TOKEN) ?? []) {
			if (/^\d+$/.test(token) || this.attests(token)) {
				continue;
			}
			missing.push(token);
		}
		return missing;
	}
}

export const verbs = new VerbLibrary();
