#!/usr/bin/env python3
"""Ingest Minecraft mod source trees as a second attested source for the prose library.

The prose library is compiled from `agent_god_files/prose_logic` (see `tools/prose_compile.py`).
That corpus is a person's writing about claims; it is not the world. A mod source tree is the
world's own vocabulary: the lang files hold the names of things (`Cogwheel`, `Mechanical Press`,
the better part of a smeltery) and the comments and docs hold the prose the authors wrote about what
those things do. Both are English somebody actually wrote, which is the only thing this project lets
a being speak.

What is read, and what is not:

  * `**/lang/en_us.json` — the string *values* only. Keys are identifiers and are excluded.
  * comments in `.java` — `//`, `/* ... */` and `/** ... */` bodies. Code is never read, so
    identifiers, string literals and class names cannot leak into the vocabulary.
  * `**/*.md` and `**/*.txt` — prose from READMEs, changelogs, design docs and directives.

Every file under `src/test` is skipped: test fixtures are not the world's voice.

A root contributes its subdirectories *and* the prose files sitting directly in it, so
`--root docs` reads this project's own documents. `--file` adds a file that lives outside any
root (a design document, an addendum): the world's design record is then attested the same way a
mod's README is, instead of being quoted into the vocabulary by hand. The report lists every prose
file each source contributed, so "did this document make it in?" is answered from the report.

Output is a TSV of `word<TAB>count` plus a JSON report of where the words came from, so a merge
into the compiled lexicon can be checked per mod instead of trusted as a blob. Words are counted
with the same pattern the corpus compiler uses, so the two sources are comparable.

Usage:
    python tools/prose_ingest_mods.py --root F:/shploingus-mod-sources --out tools/mod_lexicon.tsv
"""

from __future__ import annotations

import argparse
import collections
import json
import pathlib
import re
import sys

# The same word shape the corpus compiler attests, so merged counts mean the same thing.
WORD = re.compile(r"[a-z]+(?:'[a-z]+)?")
JAVA_SKIP = ("src/test", "src/testFixtures")
LANG_SKIP = ("lang/en_us.json",)
# Prose suffixes. `.txt` is here because the project's corrective directives arrive as text.
PROSE_SUFFIXES = (".md", ".txt")


def lang_values(path: pathlib.Path) -> list[str]:
    """Only the human-facing strings of a lang file: keys are identifiers, values are prose."""
    try:
        data = json.loads(path.read_text(encoding="utf-8", errors="replace"))
    except (json.JSONDecodeError, OSError):
        return []
    if not isinstance(data, dict):
        return []
    out = []
    for value in data.values():
        if isinstance(value, str):
            out.append(value)
    return out


def java_comments(path: pathlib.Path) -> str:
    """The comments of a Java file, with code and string literals removed."""
    try:
        text = path.read_text(encoding="utf-8", errors="replace")
    except OSError:
        return ""
    kept = []
    i, n = 0, len(text)
    in_string = in_char = False
    while i < n:
        ch = text[i]
        nxt = text[i + 1] if i + 1 < n else ""
        if in_string:
            if ch == "\\":
                i += 2
                continue
            if ch == '"':
                in_string = False
            i += 1
            continue
        if in_char:
            if ch == "\\":
                i += 2
                continue
            if ch == "'":
                in_char = False
            i += 1
            continue
        if ch == '"':
            in_string = True
            i += 1
            continue
        if ch == "'":
            in_char = True
            i += 1
            continue
        if ch == "/" and nxt == "/":
            end = text.find("\n", i)
            kept.append(text[i:end if end >= 0 else n])
            i = n if end < 0 else end
            continue
        if ch == "/" and nxt == "*":
            end = text.find("*/", i + 2)
            kept.append(text[i:end + 2 if end >= 0 else n])
            i = n if end < 0 else end + 2
            continue
        i += 1
    return "\n".join(kept)


def read_unit(source: pathlib.Path) -> tuple[collections.Counter, dict, list[str]]:
    """The prose of one source: a directory tree, or a single file read as itself."""
    words: collections.Counter[str] = collections.Counter()
    files = {"lang": 0, "java": 0, "markdown": 0}
    documents: list[str] = []
    paths = [source] if source.is_file() else [p for p in source.rglob("*") if p.is_file()]
    for path in paths:
        posix = path.as_posix()
        if any(skip in posix for skip in JAVA_SKIP):
            continue
        suffix = path.suffix.lower()
        if suffix == ".json" and any(posix.endswith(skip) for skip in LANG_SKIP):
            files["lang"] += 1
            for value in lang_values(path):
                words.update(WORD.findall(value.lower()))
        elif suffix == ".java":
            files["java"] += 1
            words.update(WORD.findall(java_comments(path).lower()))
        elif suffix in PROSE_SUFFIXES:
            files["markdown"] += 1
            documents.append(path.name if source.is_file()
                             else path.relative_to(source).as_posix())
            words.update(WORD.findall(path.read_text(encoding="utf-8", errors="replace").lower()))
    return words, files, documents


def ingest(root: pathlib.Path, extra_files: list[pathlib.Path]) -> tuple[collections.Counter, dict]:
    sources: list[pathlib.Path] = [p for p in sorted(root.iterdir())
                                   if p.is_dir() or p.suffix.lower() in PROSE_SUFFIXES]
    sources.extend(extra_files)
    words: collections.Counter[str] = collections.Counter()
    report = {"root": str(root), "mods": {}, "skipped": [], "documents": {}}
    for source in sources:
        mod_words, files, documents = read_unit(source)
        if not mod_words:
            report["skipped"].append(source.name)
            continue
        words.update(mod_words)
        report["mods"][source.name] = {
            "files": files,
            "words": len(mod_words),
            "tokens": sum(mod_words.values()),
            "top": [word for word, _ in mod_words.most_common(12)],
        }
        # Every prose file the source contributed, so coverage is checkable per document rather
        # than inferred from a word count that could have come from anywhere.
        report["documents"][source.name] = documents
    return words, report


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=pathlib.Path, required=True)
    parser.add_argument("--file", type=pathlib.Path, action="append", default=[],
                        help="a prose file outside the root, e.g. a design document")
    parser.add_argument("--out", type=pathlib.Path, required=True)
    parser.add_argument("--report", type=pathlib.Path, default=None)
    args = parser.parse_args(argv)

    if not args.root.is_dir():
        print(f"prose_ingest_mods: no such source root {args.root}", file=sys.stderr)
        return 1
    for extra in args.file:
        if not extra.is_file():
            print(f"prose_ingest_mods: no such file {extra}", file=sys.stderr)
            return 1
    words, report = ingest(args.root, args.file)
    if not words:
        print(f"prose_ingest_mods: no prose found under {args.root}", file=sys.stderr)
        return 1

    args.out.parent.mkdir(parents=True, exist_ok=True)
    with args.out.open("w", encoding="utf-8", newline="\n") as handle:
        handle.write("# Mod-source words, compiled by tools/prose_ingest_mods.py — do not hand-edit.\n")
        handle.write(f"# source: {args.root}\n")
        for extra in args.file:
            handle.write(f"# source file: {extra}\n")
        handle.write("# Read from lang values, Java comments and markdown prose; never from code.\n")
        for word, count in sorted(words.items()):
            handle.write(f"{word}\t{count}\n")

    report_path = args.report or args.out.with_suffix(".report.json")
    report["total_words"] = len(words)
    report["total_tokens"] = sum(words.values())
    report_path.write_text(json.dumps(report, indent=2, sort_keys=True), encoding="utf-8")
    print(f"prose_ingest_mods: {len(words)} word(s) from {len(report['mods'])} mod tree(s) -> {args.out}")
    print(f"prose_ingest_mods: provenance -> {report_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
