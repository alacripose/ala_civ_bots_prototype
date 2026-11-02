#!/usr/bin/env python3
"""Compare the durable local stores with their MongoDB projection, and refuse drift.

The mod writes two durable append logs - the stream ledger (``run/stream/ledger.jsonl``) and the
learning log (``run/world/ensouled/training.jsonl``) - and projects them into ``ensouled_streams``
(``events`` and ``training``) with a read-back check on every row. The projection is why a feed can
be read from one agent's own rows; a projection that silently lags behind the logs, or holds rows
the logs never committed, is the failure the store was built to prevent.

The server reconciles the projection from the logs when it mounts, and refuses to serve a row it
cannot read back. This script is the same refusal outside the server: it compares both stores row
by row and names what is behind, what diverged, and what the store holds that the chain does not.

States:

``PASS``
    Every local row is in the store, verified, and the store holds nothing else.
``BEHIND``
    Rows the append logs hold and the projection does not, rows whose projection differs, or rows
    written but not read back. A mount reconciles these by re-projecting and verifying every local
    row, and refuses to serve when reconciliation fails.
``DRIFT``
    The store contradicts the logs: extra rows, different digests or kinds, unverified rows, or a
    recorded verification failure that no later pass replaced.
``REFUSED``
    The store is unreachable, unauthorized or unreadable, or a local store is malformed.

Exit codes: 0 for ``PASS`` (and, with ``--launch``, for a projection a mount can reconcile),
1 for ``BEHIND``/``DRIFT``, 2 for ``REFUSED``.

Usage::

    python tools/mongo_parity.py [--json] [--launch] [--uri URI] [--root PATH]
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import sys
from collections import Counter
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parent.parent
PROJECT = "ala-civ-bots"
DATABASE = "ensouled_streams"
# The same candidates in the same order as `MongoStreams.open`: an explicit URI is used alone.
CANDIDATES = ("mongodb://127.0.0.1:27017", "mongodb://127.0.0.1:27018")
LEDGER_PATH = "run/stream/ledger.jsonl"
TRAINING_PATH = "run/world/ensouled/training.jsonl"
COLLECTIONS = ("streams", "events", "verifications", "training")
EVENT_FIELDS = ("kind", "outcome", "subject", "digest", "hash", "at")
# Fields the store adds to a projected training row. They are not part of the row it came from.
STORE_FIELDS = frozenset({"_id", "agent", "row_id", "verified"})
# What a mount reconciles by re-projecting and re-reading every local row. Everything else is a
# contradiction the mount cannot repair, and a launch must refuse it.
REPAIRABLE = frozenset({"missing_events", "mismatched_events", "unverified_events",
                        "missing_training", "unverified_training"})
BLOCKING = ("extra_events", "extra_training", "failed_verifications", "malformed_local")
SAMPLE = 5


class Refused(RuntimeError):
    """The store or a local log cannot be read; name why instead of guessing a result."""

    def __init__(self, code: str, detail: str) -> None:
        super().__init__(f"{code}: {detail}")
        self.code = code
        self.detail = detail


def read_jsonl(path: Path) -> tuple[list[tuple[int, dict[str, Any]]], list[str]]:
    """Parse a JSONL store, keeping the line number so a malformed row can be named."""
    rows: list[tuple[int, dict[str, Any]]] = []
    malformed: list[str] = []
    if not path.is_file():
        return rows, malformed
    with path.open(encoding="utf-8", errors="replace") as handle:
        for number, line in enumerate(handle, 1):
            line = line.strip()
            if not line:
                continue
            try:
                value = json.loads(line)
            except ValueError:
                malformed.append(f"{path.name}:{number} is not JSON")
                continue
            if not isinstance(value, dict):
                malformed.append(f"{path.name}:{number} is not an object")
                continue
            rows.append((number, value))
    return rows, malformed


def canonical(value: dict[str, Any]) -> str:
    """Both sides are canonicalized here, so the comparison never depends on a writer's formatting."""
    return json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=False)


def training_signature(row: dict[str, Any]) -> str:
    content = {key: value for key, value in row.items() if key not in STORE_FIELDS}
    return hashlib.sha256(canonical(content).encode("utf-8")).hexdigest()


def training_agent(row: dict[str, Any]) -> str:
    """The store's own derivation: the client that produced the row, else the actor, else training."""
    for key in ("client", "actor"):
        value = row.get(key)
        if isinstance(value, str) and value.strip():
            return value.strip()
    return "training"


def connect(explicit: str | None) -> tuple[Any, Any, str, list[str]]:
    """The store the mod would open: explicit URI alone, otherwise the candidates in order."""
    try:
        from pymongo import MongoClient
        from pymongo.errors import ConfigurationError, OperationFailure, PyMongoError
    except ImportError as missing:  # pragma: no cover - environment failure, named not guessed
        raise Refused("store-client-missing", f"pymongo is not installed: {missing}")

    uris = [explicit] if explicit else list(CANDIDATES)
    failures: list[str] = []
    for uri in uris:
        client = None
        try:
            client = MongoClient(uri, serverSelectionTimeoutMS=1500, connectTimeoutMS=1500,
                                 socketTimeoutMS=5000)
            client.admin.command("ping")
            database = client[DATABASE]
            # A store this process can open but not read is not a store. The mod probes the same
            # collections before it uses them, so an unauthorized candidate is skipped here too.
            for name in COLLECTIONS:
                database[name].find_one({"_id": "__probe__"}, {"_id": 1})
            return client, database, uri, failures
        except OperationFailure as denied:
            if client is not None:
                client.close()
            if denied.code in (13, 18) or "requires authentication" in str(denied):
                failures.append(f"{uri}: unauthorized")
            else:
                failures.append(f"{uri}: {denied}")
        except (ConfigurationError, PyMongoError) as failure:
            if client is not None:
                client.close()
            failures.append(f"{uri}: {type(failure).__name__}: {failure}")
    raise Refused("store-unavailable", "no readable store (" + "; ".join(failures) + ")")


def local_events(rows: list[tuple[int, dict[str, Any]]], path: str,
                 malformed: list[str]) -> dict[tuple[str, int], dict[str, Any]]:
    events: dict[tuple[str, int], dict[str, Any]] = {}
    for number, row in rows:
        actor, seq = row.get("actor"), row.get("seq")
        if not isinstance(actor, str) or not actor or not isinstance(seq, int):
            malformed.append(f"{path}:{number} has no actor/seq")
            continue
        events[(actor, seq)] = row
    return events


def compare(root: Path, database: Any) -> dict[str, Any]:
    ledger_rows, malformed = read_jsonl(root / LEDGER_PATH)
    training_rows, training_bad = read_jsonl(root / TRAINING_PATH)
    malformed.extend(training_bad)
    events = local_events(ledger_rows, LEDGER_PATH, malformed)

    local_training: Counter[tuple[str, str]] = Counter()
    for number, row in training_rows:
        local_training[(training_agent(row), training_signature(row))] += 1

    store_events: dict[tuple[str, int], dict[str, Any]] = {}
    for document in database["events"].find({"project": PROJECT}, {"_id": 0, "agent": 1, "seq": 1,
                                                                  "kind": 1, "outcome": 1,
                                                                  "subject": 1, "digest": 1,
                                                                  "hash": 1, "at": 1, "verified": 1}):
        store_events[(document.get("agent"), document.get("seq"))] = document

    missing: list[str] = []
    mismatched: list[str] = []
    unverified: list[str] = []
    for (actor, seq), row in events.items():
        document = store_events.get((actor, seq))
        if document is None:
            missing.append(f"{actor}:{seq}")
            continue
        differing = [field for field in EVENT_FIELDS
                     if canonical({"v": row.get(field)}) != canonical({"v": document.get(field)})]
        if differing:
            mismatched.append(f"{actor}:{seq} ({', '.join(differing)})")
        if document.get("verified") is not True:
            unverified.append(f"{actor}:{seq}")
    candidates = [key for key in store_events if key not in events]
    extra_events: list[tuple[str, int]] = []
    if candidates:
        # The server appends the ledger row and then projects it, so a live server can put the
        # store one row ahead of this comparison's ledger read. Re-read the append log before
        # calling a store-only row invented: a row that appears now was in flight, not history the
        # chain never committed.
        confirmed, _ignored = read_jsonl(root / LEDGER_PATH)
        current = local_events(confirmed, LEDGER_PATH, [])
        extra_events = [key for key in candidates if key not in current]

    store_training: Counter[tuple[str, str]] = Counter()
    unverified_training = 0
    for document in database["training"].find({}):
        if document.get("verified") is not True:
            unverified_training += 1
        content = {key: value for key, value in document.items() if key not in STORE_FIELDS}
        store_training[(document.get("agent", "training"),
                        hashlib.sha256(canonical(content).encode("utf-8")).hexdigest())] += 1
    missing_training = sum((local_training - store_training).values())
    surplus = store_training - local_training
    extra_training = sum(surplus.values())
    if surplus:
        # The learning row is appended before it is projected; see the event re-read above.
        confirmed, _ignored = read_jsonl(root / TRAINING_PATH)
        current: Counter[tuple[str, str]] = Counter()
        for _number, row in confirmed:
            current[(training_agent(row), training_signature(row))] += 1
        extra_training = sum(max(0, count - current[key]) for key, count in surplus.items())

    failed = database["verifications"].count_documents({"result": "FAIL"})
    unfixed: list[str] = []
    if failed:
        for document in database["verifications"].find({"result": "FAIL"}, {"id": 1}).limit(20):
            identity = document.get("id")
            if identity and database["verifications"].find_one(
                    {"id": identity, "result": "PASS"}, {"_id": 1}) is None:
                unfixed.append(str(identity))

    counts = {
        "local_ledger_rows": len(events),
        "local_training_rows": len(training_rows),
        "store_events": len(store_events),
        "store_training": sum(store_training.values()),
        "missing_events": len(missing),
        "mismatched_events": len(mismatched),
        "unverified_events": len(unverified),
        "extra_events": len(extra_events),
        "missing_training": missing_training,
        "extra_training": extra_training,
        "unverified_training": unverified_training,
        "failed_verifications": len(unfixed),
        "malformed_local": len(malformed),
    }
    samples = {
        "missing_events": missing[:SAMPLE],
        "mismatched_events": mismatched[:SAMPLE],
        "unverified_events": unverified[:SAMPLE],
        "extra_events": [f"{actor}:{seq}" for actor, seq in extra_events[:SAMPLE]],
        "failed_verifications": unfixed[:SAMPLE],
        "malformed_local": malformed[:SAMPLE],
    }
    return {"counts": counts, "samples": samples}


def verdict(counts: dict[str, int]) -> str:
    if counts["malformed_local"]:
        return "REFUSED"
    if any(counts[name] for name in BLOCKING):
        return "DRIFT"
    if any(counts[name] for name in REPAIRABLE):
        # Missing, mismatched or not-yet-read-back rows are the mount's reconcile set. They are
        # still a refusal for a build: only a mount that re-projects and verifies every row may
        # turn them into a pass, so the build verdict is BEHIND rather than PASS.
        return "BEHIND"
    return "PASS"


def blocking(counts: dict[str, int]) -> list[str]:
    return [name for name in BLOCKING if counts[name]]


def describe(state: str, counts: dict[str, int]) -> str:
    if state == "PASS":
        return (f"{counts['local_ledger_rows']} ledger row(s) and {counts['local_training_rows']} "
                f"training row(s) are in the store, verified")
    if state == "BEHIND":
        parts = []
        if counts["missing_events"]:
            parts.append(f"{counts['missing_events']} event row(s) missing")
        if counts["missing_training"]:
            parts.append(f"{counts['missing_training']} training row(s) missing")
        if counts["mismatched_events"]:
            parts.append(f"{counts['mismatched_events']} event row(s) differ")
        if counts["unverified_events"]:
            parts.append(f"{counts['unverified_events']} event row(s) not read back")
        if counts["unverified_training"]:
            parts.append(f"{counts['unverified_training']} training row(s) not read back")
        return "; ".join(parts) + "; a mount reconciles them"
    if state == "DRIFT":
        parts = []
        if counts["extra_events"]:
            parts.append(f"{counts['extra_events']} event row(s) the chain does not hold")
        if counts["extra_training"]:
            parts.append(f"{counts['extra_training']} training row(s) the log does not hold")
        if counts["mismatched_events"]:
            parts.append(f"{counts['mismatched_events']} event row(s) differ from the chain")
        if counts["unverified_events"]:
            parts.append(f"{counts['unverified_events']} event row(s) were never verified")
        if counts["unverified_training"]:
            parts.append(f"{counts['unverified_training']} training row(s) were never verified")
        if counts["failed_verifications"]:
            parts.append(f"{counts['failed_verifications']} verification failure(s) no pass replaced")
        return "; ".join(parts)
    return "a local store is malformed"


def render(report: dict[str, Any], launch: bool, repairs: list[str]) -> str:
    counts = report["counts"]
    lines = [f"mongo projection: {report['state']} -- {report['detail']}",
             f"  store    {report.get('uri') or '-'} / {DATABASE}"]
    if counts:
        lines.append("  local    ledger={local_ledger_rows} training={local_training_rows}"
                     .format(**counts))
        lines.append("  store    events={store_events} training={store_training}".format(**counts))
    if report["state"] == "PASS":
        return "\n".join(lines)
    for name in ("missing_events", "mismatched_events", "unverified_events", "extra_events",
                 "missing_training", "extra_training", "unverified_training",
                 "failed_verifications", "malformed_local"):
        if counts.get(name):
            lines.append(f"  {name.replace('_', ' '):<22}{counts[name]}")
            for sample in (report.get("samples") or {}).get(name, []):
                lines.append(f"          {sample}")
    if launch and repairs:
        lines.append("  reconcile " + ", ".join(repairs) + " -- the mount re-projects these and "
                     "refuses to serve a row it cannot read back")
    return "\n".join(lines)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        description="Compare the durable stores with their MongoDB projection.")
    parser.add_argument("--root", default=str(ROOT), help="project root")
    parser.add_argument("--uri", default=os.environ.get("ENSOULED_MONGO_URI"),
                        help="explicit store URI (default: ENSOULED_MONGO_URI, else the candidates)")
    parser.add_argument("--launch", action="store_true",
                        help="allow a projection a mount reconciles; refuse contradictions")
    parser.add_argument("--json", action="store_true", help="emit the comparison as JSON")
    args = parser.parse_args(argv)
    root = Path(args.root).resolve()

    report: dict[str, Any] = {"state": "REFUSED", "detail": "", "counts": {}, "samples": {},
                             "uri": None, "database": DATABASE}
    try:
        client, database, uri, failures = connect(args.uri)
        report["uri"] = uri
        if failures:
            report["skipped"] = failures
        try:
            report.update(compare(root, database))
        finally:
            client.close()
        report["state"] = verdict(report["counts"])
        report["detail"] = describe(report["state"], report["counts"])
    except Refused as refusal:
        report["state"] = "REFUSED"
        report["detail"] = f"{refusal.code}: {refusal.detail}"

    repairs = [name for name in REPAIRABLE if report["counts"].get(name)]
    if args.launch and report["state"] == "BEHIND":
        # A mount re-projects every local row and verifies it by reading it back; it cannot serve
        # a row that fails, so being behind is reconciled rather than tolerated.
        report["state"] = "PASS"
        report["detail"] = describe("BEHIND", report["counts"]) + " (reconciled at mount)"

    if args.json:
        print(json.dumps(report, indent=2))
    else:
        print(render(report, args.launch, repairs))
    if report["state"] == "PASS":
        return 0
    return 2 if report["state"] == "REFUSED" else 1


if __name__ == "__main__":
    sys.exit(main())
