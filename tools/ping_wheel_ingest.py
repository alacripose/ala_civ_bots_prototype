#!/usr/bin/env python3
"""Compile the ping-wheel document into language and non-conclusive wish material.

`docs/ping_wheel_messages.md` is a Deadlock ping-wheel dump: 166 named wheel entries, their
localization tokens, the chat phrases that trigger them, and who hears them. It is not this
world's gameplay, but its English is real, and this project's language rule is that a being may
speak English somebody actually wrote. The document contributes two different things, and they are
kept apart:

* **language** - the entry names and chat triggers, compiled to `tools/ping_wheel_lexicon.tsv` and
  merged into the attested lexicon by `tools/prose_compile.py --extra`. A being may use these words
  in speech and in a wish. The wheel's world need not exist here: an imported document has always
  been allowed to teach the language of a thing this world does not implement.
* **expectations** - the wheel's concepts and phrases, compiled with `basis: expected` and
  `conclusive: false`. They say what somebody might wish for or say; they do not say that this
  world implements it. A wish for a thing with no mod, item or skill behind it is allowed to exist
  as a wish: that is what the being's thinking and dreaming is for. Nothing here may be promoted to
  evidence of a capability, and no report may count an expectation as a passed check.

The artifacts are deterministic. `--check` recompiles in memory and refuses when the committed
files are stale, so a changed document cannot sit un-ingested, and a row cannot be edited to
`conclusive: true` without the check failing.

Usage::

    python tools/ping_wheel_ingest.py [--check] [--json]
"""

from __future__ import annotations

import argparse
import collections
import hashlib
import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
DEFAULT_DOC = ROOT / "docs" / "ping_wheel_messages.md"
DEFAULT_LEXICON = ROOT / "tools" / "ping_wheel_lexicon.tsv"
DEFAULT_JSON = ROOT / "tools" / "proofs" / "ping_wheel" / "expectations.json"
DEFAULT_TSV = ROOT / "tools" / "proofs" / "ping_wheel" / "expectations.tsv"
SCHEMA = "ensouled.ping-wheel-expectations/1"
# The same word shape the corpus compiler attests, so the merged counts mean the same thing.
WORD = re.compile(r"[a-z]+(?:'[a-z]+)?")
ENTRY = re.compile(r'^\t(?:"([^"]+)"|([A-Za-z][^\t=]*?))\s*=\s*$')
CONCEPT = re.compile(r'm_ePingConcept = "(CITADEL_PING_[A-Z0-9_]+)"')
LABEL = re.compile(r'm_strCommsWheelLabelToken = "#([^"]+)"')
MESSAGE = re.compile(r'm_strMessageToken = "#([^"]+)"')
RECIPIENTS = re.compile(r'm_eRecipientsType = "([^"]+)"')
LANE = re.compile(r'm_eLaneColor = "k_ELaneColor_(\w+)"')
OPTION = re.compile(r"m_unPingWheelOptionID = (\d+)")
TRIGGERS = re.compile(r"m_vecChatTextTriggers = \s*\n?\s*\[(.*?)\]", re.S)
RESPONDS = re.compile(r"m_vecRespondsToConcepts = \s*\n?\s*\[(.*?)\]", re.S)


def entries_of(text: str) -> list[dict]:
    """Read the KV3 object entries. Only brace depth is needed: every value is flat."""
    lines = text.splitlines()
    found: list[dict] = []
    index = 0
    while index < len(lines):
        match = ENTRY.match(lines[index])
        if not match or index + 1 >= len(lines) or lines[index + 1].strip() != "{":
            index += 1
            continue
        name = (match.group(1) or match.group(2)).strip()
        depth = 0
        body: list[str] = []
        cursor = index + 1
        while cursor < len(lines):
            depth += lines[cursor].count("{") - lines[cursor].count("}")
            if cursor > index + 1:
                body.append(lines[cursor])
            if depth == 0:
                break
            cursor += 1
        block = "\n".join(body)
        concept = CONCEPT.search(block)
        if concept:
            found.append({
                "name": name,
                "concept": concept.group(1),
                "label": group(LABEL, block),
                "message": group(MESSAGE, block),
                "recipients": group(RECIPIENTS, block),
                "lane": group(LANE, block),
                "option_id": option(block),
                "triggers": [value.strip() for value in re.findall(r'"([^"]+)"', group(TRIGGERS, block))],
                "responds_to": [value.strip() for value in re.findall(r'"([^"]+)"', group(RESPONDS, block))],
            })
        index = cursor + 1
    return found


def group(pattern: re.Pattern, block: str) -> str:
    match = pattern.search(block)
    return match.group(1) if match else ""


def option(block: str) -> int:
    match = OPTION.search(block)
    return int(match.group(1)) if match else 0


def phrase_words(phrase: str) -> list[str]:
    return WORD.findall(phrase.lower())


def compile_document(doc: pathlib.Path) -> tuple[collections.Counter, dict]:
    text = doc.read_text(encoding="utf-8")
    entries = entries_of(text)
    if not entries:
        raise ValueError(f"{doc}: no ping-wheel entries with a concept")
    entries.sort(key=lambda entry: (entry["option_id"], entry["name"].casefold()))
    concepts: dict[str, dict] = {}
    triggers: dict[str, set[str]] = collections.defaultdict(set)
    words: collections.Counter[str] = collections.Counter()
    rows = []
    for entry in entries:
        phrase = entry["name"]
        words.update(phrase_words(phrase))
        for trigger in entry["triggers"]:
            words.update(phrase_words(trigger))
            triggers[trigger].add(phrase)
        concept = entry["concept"]
        intent = concept.removeprefix("CITADEL_PING_").lower()
        concepts.setdefault(concept, {"concept": concept, "intent": intent, "phrases": []})["phrases"].append(phrase)
        rows.append({
            "name": phrase,
            "concept": concept,
            "intent": intent,
            "phrase": phrase,
            "label_token": f"#{entry['label']}" if entry["label"] else "",
            "message_token": f"#{entry['message']}" if entry["message"] else "",
            "triggers": sorted(set(entry["triggers"])),
            "responds_to": sorted(set(entry["responds_to"])),
            "recipients": entry["recipients"],
            "lane": entry["lane"],
            "option_id": entry["option_id"],
            # The wheel is a language source and a set of expected wishes. It is not a statement
            # that this world implements any of them.
            "basis": "expected",
            "conclusive": False,
        })
    payload = {
        "schema": SCHEMA,
        "source": "docs/ping_wheel_messages.md",
        "source_sha256": hashlib.sha256(text.encode("utf-8")).hexdigest(),
        "compiled_by": "tools/ping_wheel_ingest.py",
        "provenance": "deadlock-ping-wheel",
        "basis": "expected",
        "conclusive": False,
        "note": "Language is imported and may be spoken or wished for; a wish naming something "
                "this world does not implement is still a wish. No row here is evidence of a "
                "capability, and a report may not count one as a passed check.",
        "counts": {
            "entries": len(rows),
            "concepts": len(concepts),
            "phrases": len({row["phrase"] for row in rows}),
            "triggers": len(triggers),
            "words": len(words),
            "tokens": sum(words.values()),
        },
        "entries": rows,
        "concepts": sorted(concepts.values(), key=lambda item: item["concept"]),
        "triggers": [{"text": trigger, "entries": sorted(names)}
                     for trigger, names in sorted(triggers.items())],
    }
    return words, payload


def write_artifacts(words: collections.Counter, payload: dict, lexicon: pathlib.Path,
                    json_out: pathlib.Path, tsv_out: pathlib.Path) -> None:
    lexicon.parent.mkdir(parents=True, exist_ok=True)
    with lexicon.open("w", encoding="utf-8", newline="\n") as handle:
        handle.write("# Ping-wheel language, compiled by tools/ping_wheel_ingest.py - do not hand-edit.\n")
        handle.write("# source: docs/ping_wheel_messages.md\n")
        handle.write("# basis: expected language import; these are real English words from the\n")
        handle.write("# document's wheel names and chat triggers, usable in speech or a wish even\n")
        handle.write("# when this world implements no such thing.\n")
        for word, count in sorted(words.items()):
            handle.write(f"{word}\t{count}\n")

    json_out.parent.mkdir(parents=True, exist_ok=True)
    json_out.write_text(json.dumps(payload, indent=2, sort_keys=False) + "\n", encoding="utf-8")

    columns = ["name", "concept", "intent", "phrase", "label_token", "message_token", "triggers",
               "responds_to", "recipients", "lane", "option_id", "basis", "conclusive"]
    with tsv_out.open("w", encoding="utf-8", newline="\n") as handle:
        handle.write("# Ping-wheel expectations, compiled by tools/ping_wheel_ingest.py - do not hand-edit.\n")
        handle.write("# basis=expected, conclusive=false: an idea for a wish or a line, never a\n")
        handle.write("# claim that the world implements it.\n")
        handle.write("\t".join(columns) + "\n")
        for row in payload["entries"]:
            values = []
            for column in columns:
                value = row[column]
                if isinstance(value, list):
                    value = ",".join(value)
                elif isinstance(value, bool):
                    value = "false" if not value else "true"
                values.append(str(value))
            handle.write("\t".join(values) + "\n")


def check(words: collections.Counter, payload: dict, lexicon: pathlib.Path,
          json_out: pathlib.Path, tsv_out: pathlib.Path) -> list[str]:
    problems = []
    for path in (lexicon, json_out, tsv_out):
        if not path.is_file():
            problems.append(f"{path} is not compiled")
    if problems:
        return problems
    committed = json.loads(json_out.read_text(encoding="utf-8"))
    if committed != payload:
        problems.append(f"{json_out} is stale against docs/ping_wheel_messages.md")
    if any(entry.get("conclusive") is not False for entry in committed.get("entries", [])):
        problems.append(f"{json_out} has a row marked conclusive")
    if any(entry.get("basis") != "expected" for entry in committed.get("entries", [])):
        problems.append(f"{json_out} has a row whose basis is not expected")
    expected_words = "".join(f"{word}\t{count}\n" for word, count in sorted(words.items()))
    if expected_words not in lexicon.read_text(encoding="utf-8"):
        problems.append(f"{lexicon} does not carry the compiled word counts")
    lines = [line for line in tsv_out.read_text(encoding="utf-8").splitlines()
             if line and not line.startswith("#")]
    if len(lines) != len(payload["entries"]) + 1:
        problems.append(f"{tsv_out} has {len(lines) - 1} row(s), not {len(payload['entries'])}")
    return problems


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--doc", type=pathlib.Path, default=DEFAULT_DOC)
    parser.add_argument("--out-lexicon", type=pathlib.Path, default=DEFAULT_LEXICON)
    parser.add_argument("--out-json", type=pathlib.Path, default=DEFAULT_JSON)
    parser.add_argument("--out-tsv", type=pathlib.Path, default=DEFAULT_TSV)
    parser.add_argument("--check", action="store_true",
                        help="refuse when the committed artifacts are stale or conclusive")
    parser.add_argument("--json", action="store_true", help="print the payload")
    args = parser.parse_args(argv)

    if not args.doc.is_file():
        print(f"ping_wheel_ingest: no document at {args.doc}", file=sys.stderr)
        return 1
    try:
        words, payload = compile_document(args.doc)
    except ValueError as malformed:
        print(f"ping_wheel_ingest: REFUSED {malformed}", file=sys.stderr)
        return 1

    if args.check:
        problems = check(words, payload, args.out_lexicon, args.out_json, args.out_tsv)
        if problems:
            print("ping_wheel_ingest: REFUSED stale or conclusive expectations", file=sys.stderr)
            for problem in problems:
                print(f"  {problem}", file=sys.stderr)
            print("  run: python tools/ping_wheel_ingest.py", file=sys.stderr)
            return 1
        print(f"ping_wheel_ingest: {payload['counts']['entries']} entries, "
              f"{payload['counts']['words']} words, all expected and non-conclusive")
        return 0

    write_artifacts(words, payload, args.out_lexicon, args.out_json, args.out_tsv)
    if args.json:
        print(json.dumps(payload, indent=2))
    else:
        counts = payload["counts"]
        print(f"ping_wheel_ingest: {counts['entries']} entries / {counts['concepts']} concepts / "
              f"{counts['triggers']} triggers -> {args.out_json}")
        print(f"ping_wheel_ingest: {counts['words']} word(s) -> {args.out_lexicon} "
              f"(basis=expected, conclusive=false)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
