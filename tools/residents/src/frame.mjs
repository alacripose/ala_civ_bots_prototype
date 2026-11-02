// One utterance read as a frame: who, what verb, on what, how many, to whom, in which shape.
//
// This is a port of the JVM's FrameParser. The grammar below is grammar, not vocabulary: the verb
// is chosen by position and judged by the corpus, and the claim shape is scored from the compiled
// cue table. A word the corpus does not attest stays unknown rather than being mapped to something
// similar.
import { corpus } from "./corpus.mjs";

const DETERMINERS = new Set("the a an my your his her their our some any this that these those each every more".split(" "));
const PREPOSITIONS = new Set("to for with from of in on at by into out about".split(" "));
const PRONOUNS = new Set("i you he she we they it me him them us".split(" "));
const MUST_AUX = new Set(["must", "shall", "will", "should", "ought", "gotta"]);
const MAY_AUX = new Set(["can", "may", "could", "might", "would", "do", "does", "did", "please"]);
const SUBORDINATORS = new Set(["if", "when", "whenever", "while", "whilst", "because", "though", "although", "unless", "since", "whereas", "that"]);
const INDEFINITE = new Set(["someone", "somebody", "something", "anyone", "anybody", "anything", "everyone", "everybody", "everything", "nobody", "nothing"]);

const MIN_SHAPE_EVIDENCE = 12;
const SHAPE_MARGIN_RATIO = 1.5;

export function parseFrame({ tick = 0, source = "chat", actor = "", utterance = "", index = 0 } = {}) {
	const text = String(utterance ?? "");
	const tokens = corpus.tokens(text);
	const residue = [...tokens];
	const frameId = `frame:${tick}:${index}`;
	if (!tokens.length) {
		return frame(frameId, tick, source, actor, "", "unknown", "", null, "", "", [], text);
	}

	let at = 0;
	const subject = PRONOUNS.has(tokens[0]) ? tokens[0] : "";
	if (subject) {
		at = 1;
		residue.shift();
	}

	let modality = "";
	for (const token of tokens) {
		if (MUST_AUX.has(token)) {
			modality = "must";
			break;
		}
		if (MAY_AUX.has(token)) {
			modality = "may";
			break;
		}
	}
	if (tokens.includes("not")) {
		modality = modality || "may";
	}

	let verbAt = -1;
	let verb = "";
	for (let i = 0; i < tokens.length; i++) {
		const token = tokens[i];
		if (
			DETERMINERS.has(token) ||
			PREPOSITIONS.has(token) ||
			PRONOUNS.has(token) ||
			/^\d+$/.test(token) ||
			MUST_AUX.has(token) ||
			MAY_AUX.has(token) ||
			token === "not" ||
			token === "yes" ||
			token === "no" ||
			SUBORDINATORS.has(token) ||
			INDEFINITE.has(token)
		) {
			continue;
		}
		verb = token;
		verbAt = i;
		break;
	}

	let verbState = "unknown";
	if (verb) {
		if (corpus.pair(verb.toUpperCase())) {
			verbState = "attested";
		} else if (corpus.attests(verb)) {
			verbState = "named";
		}
	}

	let count = null;
	const objectWords = [];
	if (verbAt >= 0) {
		let stop = tokens.length;
		for (let i = verbAt + 1; i < tokens.length; i++) {
			if (tokens[i] === "and" || tokens[i] === ",") {
				stop = i;
				break;
			}
		}
		for (let i = verbAt + 1; i < stop; i++) {
			const token = tokens[i];
			if (/^\d+$/.test(token) && count === null) {
				count = Number.parseInt(token, 10);
				continue;
			}
			if (DETERMINERS.has(token) || PREPOSITIONS.has(token) || PRONOUNS.has(token)) {
				continue;
			}
			objectWords.push(token);
		}
		remove(residue, verb);
		if (count !== null) {
			remove(residue, String(count));
		}
		for (const word of objectWords) {
			remove(residue, word);
		}
	}

	const recipient = recipientOf(tokens, subject);
	remove(residue, recipient);
	const shape = claimShape(tokens);

	return frame(frameId, tick, source, actor, verb, verbState, objectWords.join(" "), count,
		recipient, modality, shape, residue, text);
}

function frame(frameId, tick, source, actor, verb, verbState, objectKind, objectCount, recipient,
	modality, claimShape, residue, utterance) {
	return { frameId, tick, source, actor, verb, verbState, objectKind, objectCount, recipient,
		modality, claimShape, residue, utterance };
}

function remove(list, value) {
	const index = list.indexOf(value);
	if (index >= 0) {
		list.splice(index, 1);
	}
}

function recipientOf(tokens, subject) {
	for (let i = 0; i < tokens.length - 1; i++) {
		if ((tokens[i] === "to" || tokens[i] === "for") && PRONOUNS.has(tokens[i + 1])) {
			return tokens[i + 1];
		}
	}
	if (tokens.includes("me")) {
		return "me";
	}
	if (subject === "you") {
		return "you";
	}
	if (tokens.length > 1 && (MAY_AUX.has(tokens[0]) || MUST_AUX.has(tokens[0])) && PRONOUNS.has(tokens[1])) {
		return tokens[1];
	}
	return "";
}

export function claimShape(tokens) {
	if (!corpus.cues.size) {
		return "";
	}
	const evidence = new Map();
	const matched = new Map();
	for (const token of tokens) {
		for (const [shape, cues] of corpus.cues) {
			const lift = cues.get(token);
			if (lift !== undefined) {
				evidence.set(shape, (evidence.get(shape) ?? 0) + lift);
				matched.set(shape, (matched.get(shape) ?? 0) + 1);
			}
		}
	}
	if (!evidence.size) {
		return "";
	}
	const ranked = [...evidence.entries()].sort((a, b) => b[1] - a[1]);
	const [best, score] = ranked[0];
	if ((matched.get(best) ?? 0) < 2 || score < MIN_SHAPE_EVIDENCE) {
		return "";
	}
	const second = ranked.length > 1 ? ranked[1][1] : 0;
	if (second > 0 && score < second * SHAPE_MARGIN_RATIO) {
		return "";
	}
	return best;
}

export function shapeBasis(utterance, shape) {
	if (!shape) {
		return "none";
	}
	const cues = corpus.cues.get(shape);
	if (!cues) {
		return "none";
	}
	const matched = [];
	for (const token of corpus.tokens(utterance)) {
		if (cues.has(token) && !matched.includes(token)) {
			matched.push(token);
		}
	}
	return matched.length ? `cues:${matched.join(",")}` : "none";
}

export function verbBasis(verb) {
	if (!verb) {
		return "none";
	}
	if (corpus.pair(verb.toUpperCase())) {
		return "baseline";
	}
	const count = corpus.count(verb);
	return count > 0 ? `lexicon:${count}` : "unattested";
}

export function describeFrame(f) {
	const parts = [f.actor || "someone", f.verb || "(no verb)"];
	if (f.objectKind) {
		parts.push(f.objectKind);
	}
	if (f.objectCount !== null && f.objectCount !== undefined) {
		parts.push(`x${f.objectCount}`);
	}
	if (f.recipient) {
		parts.push(`-> ${f.recipient}`);
	}
	parts.push(`[${f.verbState}${f.claimShape ? `, ${f.claimShape}` : ""}]`);
	return parts.join(" ");
}
