#!/usr/bin/env python3
"""Extract the prose of a corpus's PDFs into plain text for language ingestion.

`tools/prose_ingest_mods.py` reads `.md` and `.txt`; a PDF is prose the compiler cannot see. This
script is the conversion step, not a second reader: every PDF under the given root is converted
with `pdftotext -layout -enc UTF-8` into a hash-addressed `.txt` in the staging directory, and the
manifest records the source path, its SHA-256, the page count when `pdfinfo` can supply one, and
the resulting text size. Identical PDFs are converted once and both sources are named.

A PDF that cannot be converted, or converts to nothing readable, is a refusal: a corpus that is
silently missing a document would make the lexicon claim more provenance than it has.

Usage::

    python tools/pdf_extract.py --root PATH [--out tools/proofs/pdf_text] [--json]
"""

from __future__ import annotations

import argparse
import hashlib
import json
import pathlib
import re
import shutil
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
DEFAULT_OUT = ROOT / "tools" / "proofs" / "pdf_text"
SKIP_DIRS = {".git", "target", "node_modules", ".freebuff", ".scratch", ".cargo"}


def which(name: str) -> str | None:
    return shutil.which(name)


def digest(path: pathlib.Path) -> str:
    value = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1 << 20), b""):
            value.update(chunk)
    return value.hexdigest()


def pages(path: pathlib.Path, pdfinfo: str | None) -> int | None:
    if not pdfinfo:
        return None
    try:
        result = subprocess.run([pdfinfo, str(path)], capture_output=True, text=True, timeout=30)
    except (OSError, subprocess.TimeoutExpired):
        return None
    for line in result.stdout.splitlines():
        if line.startswith("Pages:"):
            try:
                return int(line.split(":", 1)[1].strip())
            except ValueError:
                return None
    return None


def slug(stem: str) -> str:
    value = re.sub(r"[^A-Za-z0-9]+", "-", stem).strip("-").lower()
    return value[:60] or "document"


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=pathlib.Path, required=True)
    parser.add_argument("--out", type=pathlib.Path, default=DEFAULT_OUT)
    parser.add_argument("--json", action="store_true")
    args = parser.parse_args(argv)

    if not args.root.is_dir():
        print(f"pdf_extract: no such corpus root {args.root}", file=sys.stderr)
        return 1
    pdftotext = which("pdftotext")
    if not pdftotext:
        print("pdf_extract: REFUSED pdftotext is not on PATH", file=sys.stderr)
        return 1
    args.out.mkdir(parents=True, exist_ok=True)

    documents: dict[str, dict] = {}
    failures: list[str] = []
    seen: dict[str, str] = {}
    for path in sorted(args.root.rglob("*.pdf")):
        if any(part.casefold() in SKIP_DIRS for part in path.relative_to(args.root).parts):
            continue
        sha = digest(path)
        if sha in seen:
            documents[seen[sha]]["sources"].append(str(path))
            continue
        text_path = args.out / f"{slug(path.stem)}-{sha[:8]}.txt"
        result = subprocess.run([pdftotext, "-layout", "-enc", "UTF-8", str(path), str(text_path)],
                                capture_output=True, text=True, timeout=180)
        if result.returncode != 0:
            failures.append(f"{path}: pdftotext exited {result.returncode}: "
                            f"{(result.stderr or '').strip()[:160]}")
            continue
        text = text_path.read_text(encoding="utf-8", errors="replace")
        if len(text.strip()) < 40:
            failures.append(f"{path}: converted to {len(text.strip())} character(s)")
            continue
        seen[sha] = text_path.name
        documents[text_path.name] = {
            "sources": [str(path)],
            "sha256": sha,
            "bytes": path.stat().st_size,
            "pages": pages(path, which("pdfinfo")),
            "text_chars": len(text),
        }

    manifest = {
        "schema": "ensouled.pdf-text/1",
        "root": str(args.root),
        "out": str(args.out),
        "documents": documents,
        "failures": failures,
        "counts": {
            "pdfs": sum(len(row["sources"]) for row in documents.values()),
            "converted": len(documents),
            "failed": len(failures),
            "text_chars": sum(row["text_chars"] for row in documents.values()),
        },
    }
    (args.out / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n",
                                            encoding="utf-8")
    if args.json:
        print(json.dumps(manifest, indent=2))
    else:
        counts = manifest["counts"]
        print(f"pdf_extract: {counts['converted']}/{counts['pdfs']} PDF(s) converted, "
              f"{counts['text_chars']} character(s) -> {args.out}")
        for failure in failures:
            print(f"  REFUSED {failure}", file=sys.stderr)
    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
