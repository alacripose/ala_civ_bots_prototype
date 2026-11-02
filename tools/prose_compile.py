#!/usr/bin/env python3
"""Compile the prose-logic corpus into the verb baseline this world classifies speech with.

The corpus is `agent_god_files/prose_logic`: `store/claim.jsonl` is the source of truth and
`output/GOD_*.md` is its rendered view. Every claim's rule has the shape `<ref>: VERB [args]` and
carries exactly one atom kind and one modality. This compiler reads those, counts them, and writes
the baseline table the mods load at runtime, so the vocabulary in the jar is always compiled from
the corpus rather than hand-maintained here.

Usage:
    python tools/prose_compile.py [--corpus PATH] [--out PATH]

Exits non-zero when the corpus is absent or holds no claim in the rule shape: a baseline that
cannot be compiled must not silently replace the committed one.
"""
from __future__ import annotations

import argparse
import collections
import hashlib
import json
import math
import pathlib
import re
import sys

DEFAULT_CORPUS = pathlib.Path(r"C:\Users\ala\Documents\agent_god_files\prose_logic")
DEFAULT_OUT = pathlib.Path(__file__).resolve().parent.parent / "src/main/resources/ensouled/prose_baseline.tsv"

RULE = re.compile(r"^[^:]*:\s*([A-Z_]+)\s*\[")
# A word is a run of letters, optionally with an internal apostrophe: the corpus attests both
# plain words and contractions, and a contraction is one word rather than a word plus a fragment.
WORD = re.compile(r"[a-z]+(?:'[a-z]+)?")

# Grammar, not vocabulary: the closed classes a verb is never drawn from. These are the same sets
# the frame parser uses to find the verb slot, so the two readings of a sentence agree.
# A finite-verb sighting must also be a real share of the word's imperative use: a corpus that
# quotes transliterated and fragmentary sentences opens many quotes with a name or a particle, and
# one modal sighting beside a thousand openers is not evidence that they are verbs.
MINIMUM_FINITE_SHARE = 0.1
MODALS = {"must", "shall", "will", "should", "can", "may", "could", "might", "would", "do",
          "does", "did", "to", "gotta", "ought"}
NOT_A_VERB = MODALS | {
    "the", "a", "an", "my", "your", "his", "her", "their", "our", "some", "any", "this",
    "that", "these", "those", "each", "every", "more", "i", "you", "he", "she", "we", "they",
    "it", "me", "him", "them", "us", "not", "no", "yes", "and", "or", "but", "if", "when",
    "while", "because", "though", "unless", "since", "for", "of", "in", "on", "at", "by",
    "with", "from", "into", "out", "about", "as", "than", "then", "there", "here",
    "so", "all", "just", "only", "very", "also", "still", "even", "too", "well", "much",
    "now", "be", "is", "are", "was", "were", "been", "being", "have", "has", "had",
    "done", "what", "which", "who", "whom", "whose", "why", "how",
    # Pronouns and numerals that open a sentence can look like imperatives, and a pronoun can
    # stand before a modal. Both are closed classes, so they are grammar rather than vocabulary.
    "ye", "thou", "thee", "thy", "thine", "one", "ones", "none", "another", "others",
    "anyone", "someone", "everyone", "nobody", "something", "anything", "nothing", "things",
}


def compile_action_verbs(quotes, lexicon, minimum=3):
    """The action verbs the corpus attests, induced from where it puts them.

    No verb list is written here. A word counts as a verb when the corpus's own quoted sentences
    place it in a verb position:

    * after a modal or auxiliary -- "you must bring the wood", "it will harvest";
    * sentence-initial imperative -- "Gather the logs before dark".

    Both are grammatical positions, so the evidence is the corpus's usage rather than a dictionary
    this project maintains. One sighting in either slot is a coincidence, so a candidate must
    clear ``minimum`` occurrences, and every candidate must already be attested vocabulary.
    """
    modal = collections.Counter()
    imperative = collections.Counter()
    for tokens in quotes:
        for index, token in enumerate(tokens[:-1]):
            if token in MODALS and tokens[index + 1] not in NOT_A_VERB:
                modal[tokens[index + 1]] += 1
        if tokens and tokens[0] not in NOT_A_VERB:
            imperative[tokens[0]] += 1
    rows = []
    for word in set(modal) | set(imperative):
        if lexicon[word] <= 0:
            continue
        evidence = modal[word] + imperative[word]
        if evidence < minimum:
            continue
        if modal[word] < 1 or modal[word] / evidence < MINIMUM_FINITE_SHARE:
            continue
        rows.append((word, evidence, modal[word], imperative[word]))
    rows.sort(key=lambda row: (-row[1], row[0]))
    return rows


def read_claims(corpus: pathlib.Path):
    """Every claim record, from the JSON store when present, else from the rendered markdown."""
    store = corpus / "store" / "claim.jsonl"
    if store.is_file():
        with store.open(encoding="utf-8") as handle:
            for line in handle:
                line = line.strip()
                if line:
                    yield json.loads(line)
        return
    for page in sorted((corpus / "output").glob("GOD_*.md")):
        kind = modality = rule = None
        for line in page.read_text(encoding="utf-8").splitlines():
            if line.startswith("- **Kind:**"):
                parts = line.split("**Kind:**", 1)[1].split("(", 1)
                kind = parts[0].strip()
                modality = parts[1].split(":", 1)[1].rstrip(")").strip() if ":" in parts[1] else ""
            elif line.startswith("- **Rule:**"):
                rule = line.split("**Rule:**", 1)[1].strip()
            if kind and rule:
                yield {"kind": kind, "modality": modality, "rule": rule}
                kind = modality = rule = None


def compile_baseline(corpus: pathlib.Path):
    pairs: collections.Counter[tuple[str, str, str]] = collections.Counter()
    lexicon: collections.Counter[str] = collections.Counter()
    # The words each claim shape is actually stated with. A sentence is classified by the shape
    # whose own quotes it most resembles, so the mapping from English to a claim shape is compiled
    # from the corpus rather than declared in the parser.
    by_verb: dict[str, collections.Counter] = collections.defaultdict(collections.Counter)
    quotes: list[list[str]] = []
    # Every claim the corpus states, in its own words. This is the speech material the mod
    # ingests: a being speaks corpus claims, not sentences assembled from a speech-act menu.
    claim_rows: list[tuple[str, str, str]] = []
    seen: set[tuple[str, str]] = set()
    claims = 0
    digest = hashlib.sha256()
    for claim in read_claims(corpus):
        claims += 1
        rule = claim.get("rule") or ""
        digest.update(rule.encode("utf-8"))
        words = WORD.findall(str(claim.get("evidence_quote") or "").lower())
        if words:
            quotes.append(words)
        # The quoted unit is the corpus's own English. Every word a being may speak has to be
        # attested here: the lexicon is not a dictionary this project writes, it is what the
        # corpus actually says.
        lexicon.update(words)
        match = RULE.match(rule)
        if match:
            pairs[(match.group(1), str(claim.get("kind")), str(claim.get("modality")))] += 1
            by_verb[match.group(1)].update(words)
            # A spoken line needs words: a rule whose quote is a bare number is a parsed artefact,
            # not something a being can say. Length is bounded so one claim is one utterance.
            quote = " ".join(str(claim.get("evidence_quote") or "").split())
            if quote and 3 <= len(words) <= 60:
                key = (match.group(1), quote.lower())
                if key not in seen:
                    seen.add(key)
                    claim_rows.append((match.group(1), rule.split(":", 1)[0].strip(), quote))
    rows = [(verb, kind, modality, count) for (verb, kind, modality), count in sorted(pairs.items())]
    return rows, claims, digest.hexdigest()[:16], lexicon, by_verb, quotes, claim_rows


def compile_cues(by_verb: dict[str, collections.Counter], cues_per_verb: int = 14) -> list[tuple[str, str, float]]:
    """The words that mark each claim shape, relative to every other shape.

    A word's score is how much more often this shape says it than the corpus does overall. Words
    every shape says ("the", "and", "of") sink to a score near 1 and fall away on their own, so no
    stop-word list has to be maintained here.
    """
    overall: collections.Counter = collections.Counter()
    for counts in by_verb.values():
        overall.update(counts)
    total = sum(overall.values())
    if not total:
        return []
    rows: list[tuple[str, str, float]] = []
    for verb, counts in by_verb.items():
        own = sum(counts.values())
        if not own:
            continue
        scored = []
        for word, count in counts.items():
            # A word said twice has a spectacular lift and no evidence behind it: short fragments
            # and rare names outrank the words that actually mark a shape. Frequency and lift are
            # required together, and the ranking weighs how much text stands behind the lift.
            if count < 25:
                continue
            lift = (count / own) / (overall[word] / total)
            if lift < 2.5:
                continue
            scored.append((lift * math.log2(count), lift, word))
        scored.sort(reverse=True)
        for _, lift, word in scored[:cues_per_verb]:
            rows.append((verb, word, round(lift, 3)))
    return rows


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--corpus", type=pathlib.Path, default=DEFAULT_CORPUS)
    parser.add_argument("--out", type=pathlib.Path, default=DEFAULT_OUT)
    # A second attested source: words compiled by tools/prose_ingest_mods.py from mod trees. It
    # contributes vocabulary only — the verb baseline stays the corpus's, because a verb is a
    # claim shape and a language file does not make claims. Provenance is written into the header
    # so a merged lexicon can still be traced to each source.
    parser.add_argument("--extra", type=pathlib.Path, action="append", default=[],
                        help="word counts compiled from another source, merged into the lexicon")
    args = parser.parse_args(argv)

    if not args.corpus.exists():
        print(f"prose_compile: corpus not found at {args.corpus}", file=sys.stderr)
        return 1
    rows, claims, digest, lexicon, by_verb, quotes, claim_rows = compile_baseline(args.corpus)
    if not rows:
        print(f"prose_compile: no rule-shaped claims in {args.corpus}", file=sys.stderr)
        return 1

    corpus_words = len(lexicon)
    for extra in args.extra:
        if not extra.is_file():
            print(f"prose_compile: extra source not found at {extra}", file=sys.stderr)
            return 1
        merged = 0
        with extra.open(encoding="utf-8") as handle:
            for line in handle:
                line = line.rstrip("\n")
                if not line or line.startswith("#"):
                    continue
                word, _, count = line.partition("\t")
                try:
                    lexicon[word] += int(count)
                except ValueError:
                    continue
                merged += 1
        print(f"prose_compile: merged {merged} word(s) from {extra} ({len(lexicon) - corpus_words} new)")

    args.out.parent.mkdir(parents=True, exist_ok=True)
    lexicon_out = args.out.parent / "prose_lexicon.tsv"
    with lexicon_out.open("w", encoding="utf-8", newline="\n") as handle:
        handle.write("# Prose-logic lexicon, compiled by tools/prose_compile.py — do not hand-edit.\n")
        handle.write(f"# source: {args.corpus}\n")
        for extra in args.extra:
            handle.write(f"# merged source: {extra}\n")
        handle.write("# Every word the corpus quotes attest, with how often it says it. Speech here may\n")
        handle.write("# only use these words; the world supplies its own names and the being its numbers.\n")
        for word, count in sorted(lexicon.items()):
            handle.write(f"{word}\t{count}\n")
    print(f"prose_compile: {len(lexicon)} attested word(s) -> {lexicon_out}")
    with args.out.open("w", encoding="utf-8", newline="\n") as handle:
        handle.write("# Prose-logic baseline, compiled by tools/prose_compile.py — do not hand-edit.\n")
        handle.write(f"# source: {args.corpus}\n")
        handle.write(f"# claims read: {claims} · corpus digest: {digest}\n")
        handle.write("# Each row is one verb the corpus uses, with the atom kind and modality the corpus\n")
        handle.write("# assigns it. This table is the vocabulary sentences here may be composed of.\n")
        for verb, kind, modality, count in rows:
            handle.write(f"{verb}\t{kind}\t{modality}\t{count}\n")
    print(f"prose_compile: {len(rows)} verb(s) from {claims} claim(s) -> {args.out}")

    # The ingested claim corpus: the being's speech material. Each row is one claim the corpus
    # states, with its own verb and its own quoted words, so speech is selected from claims rather
    # than composed from a hand-written set of speech acts.
    claims_out = args.out.parent / "prose_claims.tsv"
    with claims_out.open("w", encoding="utf-8", newline="\n") as handle:
        handle.write("# Ingested prose claims, compiled by tools/prose_compile.py — do not hand-edit.\n")
        handle.write(f"# source: {args.corpus}\n")
        handle.write("# verb<TAB>ref<TAB>quote — what the corpus states, in its own words.\n")
        for verb, ref, quote in claim_rows:
            handle.write(f"{verb}\t{ref}\t{quote}\n")
    print(f"prose_compile: {len(claim_rows)} spoken claim(s) ingested -> {claims_out}")

    # The dynamic verb library: the action verbs this corpus uses, read from its own sentences.
    # A being names what it is doing with a word from here, so no capability carries a hand-written
    # English synonym list and no action is spoken by its machine id.
    verbs = compile_action_verbs(quotes, lexicon)
    verbs_out = args.out.parent / "prose_verbs.tsv"
    with verbs_out.open("w", encoding="utf-8", newline="\n") as handle:
        handle.write("# Action verbs, compiled by tools/prose_compile.py — do not hand-edit.\n")
        handle.write(f"# source: {args.corpus}\n")
        handle.write("# Induced from the corpus's own quoted sentences, never from a verb list:\n")
        handle.write("# a word counts when the corpus puts it after a modal/auxiliary or at the\n")
        handle.write("# start of an imperative. word<TAB>verbs<TAB>modal<TAB>imperative\n")
        for word, evidence, modal, imperative in verbs:
            handle.write(f"{word}\t{evidence}\t{modal}\t{imperative}\n")
    print(f"prose_compile: {len(verbs)} action verb(s) induced from {claims} claim quote(s)"
          f" -> {verbs_out}")

    cues = compile_cues(by_verb)
    if cues:
        cues_out = args.out.parent / "prose_cues.tsv"
        with cues_out.open("w", encoding="utf-8", newline="\n") as handle:
            handle.write("# Claim-shape cues, compiled by tools/prose_compile.py — do not hand-edit.\n")
            handle.write(f"# source: {args.corpus}\n")
            handle.write("# verb<TAB>word<TAB>lift — how much more often this shape says the word than\n")
            handle.write("# the corpus overall. The frame parser scores a sentence against these, so no\n")
            handle.write("# word-to-shape table is written by hand anywhere in the code.\n")
            for verb, word, lift in cues:
                handle.write(f"{verb}\t{word}\t{lift}\n")
        print(f"prose_compile: {len(cues)} cue(s) over {len(by_verb)} shape(s) -> {cues_out}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
