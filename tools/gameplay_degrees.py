#!/usr/bin/env python3
"""Read the city-builder design lineage and report the gameplay degrees this tree can witness.

The design's own staged vocabulary is Phase 0 through Phase 8. Phase 0 is the evidence/world
boundary; Phases 1-8 are the gameplay degrees. This script does not invent a score: it reads the
phase headings and requirement bullets from the supplied documents, checks that each later version
retains the earlier phases, and then looks for a **source witness** for a requirement at compile
time.

A source witness is a file and a marker in that file. It is deliberately weaker than a live run: a
class existing is not a bot performing the gameplay. The report says so on its face. Precursor
skills (harvesting, crafting, social exchange, placement) are listed separately and are never
counted as credit toward a defense/city phase.

Openlock is a different tree. Its AXIS witness is read from its own store and presented as an
external cell. The report never claims that Openlock's AXIS cells validate this Minecraft mod.
`parity_check.py` is only invoked when the caller supplies an install path, because it writes its
own run to the store and needs a concrete installed shell.

Usage::

    python tools/gameplay_degrees.py [--root PATH] [--openlock PATH]
                                    [--openlock-install PATH] [--json] [--out PATH]

Exit code 0 means the report was produced. A malformed document or an unmatched witness needle is
an internal contract error and exits 2; absent documents, source witnesses, or Openlock state are
named in the report rather than turned into a false pass.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parent.parent
DEFAULT_OPENLOCK = Path(r"C:\Users\ala\Documents\openlock")
DOC_GLOB = "persistent_city_builder_tower_defense_design_analysis*.md"
PHASE_HEADING = re.compile(r"^###\s+Phase\s+(\d+)\s+[—–-]\s+(.+?)\s*$")
ROADMAP_HEADING = re.compile(r"^##\s+12\.\s+Development roadmap", re.IGNORECASE)
NEXT_HEADING = re.compile(r"^##\s+13\.")
SOURCE_SUFFIXES = {".java", ".md", ".mjs", ".js", ".py", ".gradle", ".json"}


class ReportError(RuntimeError):
    """The report's own contract is broken; name it rather than guessing a result."""


@dataclass(frozen=True)
class Phase:
    number: int
    title: str
    requirements: list[str]
    exit_criteria: str
    line: int


@dataclass(frozen=True)
class DesignDoc:
    label: str
    path: Path
    phases: list[Phase]


@dataclass(frozen=True)
class Witness:
    phase: int
    needle: str
    files: tuple[str, ...]
    markers: tuple[str, ...] = ()
    expected: str = "held"
    detail: str = ""


@dataclass(frozen=True)
class Precursor:
    name: str
    files: tuple[str, ...]
    markers: tuple[str, ...]
    detail: str


# The phase requirements are matched by a stable substring, not by list index, so a new bullet in a
# future document cannot silently attach this evidence to the wrong sentence.
WITNESSES: tuple[Witness, ...] = (
    Witness(
        0,
        "decide whether this design",
        ("docs/V9_FINDINGS.md",),
        ("Exploratory / non-final",),
        "partial",
        "the document keeps the proposal separate but still calls it exploratory/non-final",
    ),
    Witness(
        0,
        "stable project vocabulary",
        ("CONTEXT.md",),
        ("**Homebase**", "**Resident**"),
        "held",
        "the project vocabulary names the settlement and the persistent inhabitant",
    ),
    Witness(
        0,
        "save-schema versioning",
        ("src/main/java/dev/ensouled/domain/WorldIdentity.java",
         "src/main/java/dev/ensouled/domain/WorldState.java"),
        ("SCHEMA", "schemaVersion", "migration"),
        "partial",
        "a save schema exists; explicit world-generation migration rules do not",
    ),
    Witness(
        1,
        "one homebase",
        ("src/main/java/dev/ensouled/domain/Homebase.java",
         "src/main/java/dev/ensouled/domain/WorldState.java",
         "src/main/java/dev/ensouled/mind/AgentManager.java"),
        ("claimHomebase",),
        "held",
        "one durable homebase per agent, claimed when the agent is bound",
    ),
    Witness(
        3,
        "resource inputs and outputs",
        ("src/main/java/dev/ensouled/skills/HarvestTreeRoutine.java",
         "src/main/java/dev/ensouled/skills/CraftRoutine.java"),
        ("skillId",),
        "partial",
        "item gathering and crafting exist; typed fluid/power carriers and a production graph do not",
    ),
    Witness(
        3,
        "construction and repair with reserved supply",
        ("src/main/java/dev/ensouled/skills/PlaceBlockRoutine.java",
         "src/main/java/dev/ensouled/work/WorkLedger.java"),
        ("recordPlacement",),
        "partial",
        "one-block placement is witnessed; repair and reserved supply are absent",
    ),
)


PRECURSORS: tuple[Precursor, ...] = (
    Precursor(
        "survival",
        ("src/main/java/dev/ensouled/skills/EatIfHungryRoutine.java",
         "src/main/java/dev/ensouled/skills/RestRoutine.java"),
        ("eat_if_hungry", "rest"),
        "food and rest routines; not a defense or city-production degree",
    ),
    Precursor(
        "resource",
        ("src/main/java/dev/ensouled/skills/HarvestTreeRoutine.java",
         "src/main/java/dev/ensouled/skills/CraftRoutine.java"),
        ("harvest_tree", "make"),
        "gather and make routines; no factory, power, or transport chain",
    ),
    Precursor(
        "social",
        ("src/main/java/dev/ensouled/skills/RequestFoodRoutine.java",
         "src/main/java/dev/ensouled/skills/GiveFoodRoutine.java",
         "src/main/java/dev/ensouled/skills/RequestSuppliesRoutine.java",
         "src/main/java/dev/ensouled/skills/SupplyFromChestRoutine.java",
         "src/main/java/dev/ensouled/skills/RequestFromHumanRoutine.java"),
        ("pending_request",),
        "request/give and storage-backed supply; no trade, faction, or city service",
    ),
    Precursor(
        "settlement",
        ("src/main/java/dev/ensouled/domain/Homebase.java",
         "src/main/java/dev/ensouled/domain/WorldState.java",
         "src/main/java/dev/ensouled/work/WorkLedger.java",
         "src/main/java/dev/ensouled/skills/PlaceBlockRoutine.java"),
        ("claimHomebase", "recordPlacement"),
        "homebase record, wishes/tickets, and one-block placement; not a built settlement",
    ),
    Precursor(
        "node client",
        ("tools/residents/src/main.mjs",
         "tools/residents/src/axis.mjs"),
        ("DecisionStream", "readDecision"),
        "the mineflayer body reads exact AXIS packages; it is a client, not a gameplay phase",
    ),
)


def version_label(path: Path) -> str:
    match = re.search(r"_v(\d+)$", path.stem)
    return f"v{match.group(1)}" if match else "base"


def parse_doc(path: Path) -> DesignDoc:
    lines = path.read_text(encoding="utf-8").splitlines()
    start = next((i for i, line in enumerate(lines) if ROADMAP_HEADING.match(line)), None)
    if start is None:
        raise ReportError(f"{path}: no '## 12. Development roadmap' section")
    end = next((i for i in range(start + 1, len(lines)) if NEXT_HEADING.match(lines[i])), len(lines))

    phases: list[Phase] = []
    current: dict[str, Any] | None = None
    for index in range(start + 1, end):
        line = lines[index].strip()
        heading = PHASE_HEADING.match(line)
        if heading:
            if current is not None:
                phases.append(Phase(current["number"], current["title"],
                                    current["requirements"], current["exit"], current["line"]))
            current = {
                "number": int(heading.group(1)),
                "title": heading.group(2).strip(),
                "requirements": [],
                "exit": "",
                "line": index + 1,
            }
            continue
        if current is None or not line:
            continue
        if line.startswith("- "):
            current["requirements"].append(line[2:].strip())
        elif line.startswith("**Exit criteria:**"):
            current["exit"] = line[len("**Exit criteria:**"):].strip()
    if current is not None:
        phases.append(Phase(current["number"], current["title"], current["requirements"],
                            current["exit"], current["line"]))
    if not phases:
        raise ReportError(f"{path}: the roadmap contains no phase headings")
    numbers = [phase.number for phase in phases]
    if numbers != list(range(numbers[0], numbers[0] + len(numbers))):
        raise ReportError(f"{path}: phase numbers are not consecutive: {numbers}")
    return DesignDoc(version_label(path), path, phases)


def load_design(root: Path) -> tuple[list[DesignDoc], str | None]:
    directory = root / "docs"
    paths = sorted(directory.glob(DOC_GLOB), key=lambda path: (version_label(path) != "base",
                                                               version_label(path)))
    if not paths:
        return [], f"no {DOC_GLOB} under {directory}"
    docs = [parse_doc(path) for path in paths]
    for previous, current in zip(docs, docs[1:]):
        previous_numbers = {phase.number for phase in previous.phases}
        current_numbers = {phase.number for phase in current.phases}
        missing = sorted(previous_numbers - current_numbers)
        if missing:
            raise ReportError(f"{current.path.name} drops phase(s) {missing} present in "
                              f"{previous.path.name}")
        for before in previous.phases:
            after = next((phase for phase in current.phases if phase.number == before.number), None)
            if after is not None and after.title != before.title:
                raise ReportError(f"{current.path.name} renames Phase {before.number}: "
                                  f"{before.title!r} -> {after.title!r}")
    return docs, None


def source_state(root: Path, files: tuple[str, ...], markers: tuple[str, ...],
                 expected: str = "held") -> tuple[str, list[str]]:
    missing: list[str] = []
    texts: list[str] = []
    for relative in files:
        path = root / relative
        if not path.is_file():
            missing.append(relative)
            continue
        texts.append(path.read_text(encoding="utf-8", errors="replace").casefold())
    if missing:
        return "absent", [f"missing {', '.join(missing)}"]
    absent_markers = [marker for marker in markers
                      if not any(marker.casefold() in text for text in texts)]
    if absent_markers:
        return "partial", [f"marker absent: {marker}" for marker in absent_markers]
    return expected, []


def phase_witnesses(root: Path, docs: list[DesignDoc]) -> tuple[list[dict[str, Any]], list[str]]:
    """Attach each witness to exactly one requirement, then count only source-held evidence."""
    v9 = docs[-1]
    by_number = {phase.number: phase for phase in v9.phases}
    attached: dict[tuple[int, str], dict[str, Any]] = {}
    errors: list[str] = []
    for witness in WITNESSES:
        phase = by_number.get(witness.phase)
        if phase is None:
            errors.append(f"witness names Phase {witness.phase}, absent from {v9.path.name}")
            continue
        matches = [requirement for requirement in phase.requirements
                   if witness.needle.casefold() in requirement.casefold()]
        if len(matches) != 1:
            errors.append(f"Phase {witness.phase}: needle {witness.needle!r} matched "
                          f"{len(matches)} requirements")
            continue
        state, detail = source_state(root, witness.files, witness.markers, witness.expected)
        attached[(witness.phase, matches[0])] = {
            "state": state,
            "files": list(witness.files),
            "markers": list(witness.markers),
            "detail": "; ".join([witness.detail] + detail),
        }

    rows: list[dict[str, Any]] = []
    for phase in v9.phases:
        requirements = []
        for requirement in phase.requirements:
            witness = attached.get((phase.number, requirement))
            requirements.append({
                "text": requirement,
                "state": witness["state"] if witness else "absent",
                "witness": witness,
            })
        rows.append({
            "number": phase.number,
            "title": phase.title,
            "line": phase.line,
            "exit_criteria": phase.exit_criteria,
            "requirements": requirements,
        })
    return rows, errors


def precursor_rows(root: Path) -> list[dict[str, Any]]:
    rows = []
    for precursor in PRECURSORS:
        state, detail = source_state(root, precursor.files, precursor.markers)
        rows.append({
            "name": precursor.name,
            "state": state,
            "files": list(precursor.files),
            "detail": "; ".join([precursor.detail] + detail),
        })
    return rows


def parse_json_loose(text: str) -> dict[str, Any] | None:
    """Read a JSON object from stdout that may also carry a human line or a warning."""
    text = text.strip()
    if not text:
        return None
    try:
        value = json.loads(text)
        return value if isinstance(value, dict) else None
    except json.JSONDecodeError:
        pass
    start = text.find("{")
    end = text.rfind("}")
    if start < 0 or end <= start:
        return None
    try:
        value = json.loads(text[start:end + 1])
        return value if isinstance(value, dict) else None
    except json.JSONDecodeError:
        return None


def run_json(command: list[str], cwd: Path, timeout: int = 30) -> tuple[dict[str, Any] | None, str]:
    try:
        completed = subprocess.run(command, cwd=str(cwd), capture_output=True, text=True,
                                   timeout=timeout)
    except (OSError, subprocess.TimeoutExpired) as failure:
        return None, f"{type(failure).__name__}: {failure}"
    payload = parse_json_loose(completed.stdout)
    if payload is None:
        detail = (completed.stderr or completed.stdout).strip()
        return None, f"exit {completed.returncode}: {detail[:240] or 'no output'}"
    return payload, ""


def openlock_axis(openlock: Path) -> dict[str, Any]:
    script = openlock / "tools" / "axis_witness.py"
    result: dict[str, Any] = {
        "root": str(openlock),
        "script": str(script),
        "command": "verify",
        "state": "ABSENT",
        "detail": "",
    }
    if not script.is_file():
        result["detail"] = "tools/axis_witness.py is not present"
        return result
    payload, failure = run_json([sys.executable, str(script), "verify", "--json"], openlock)
    if payload is None:
        result.update(state="BLOCKED", detail=failure)
        return result
    result["state"] = str(payload.get("state", "BLOCKED"))
    result["detail"] = str(payload.get("detail", ""))
    result["run"] = payload.get("run")
    result["commit"] = payload.get("commit")
    cells = payload.get("cells")
    result["cells"] = len(cells) if isinstance(cells, list) else payload.get("cells", 0)
    verification = payload.get("verification")
    if isinstance(verification, dict):
        result["verification"] = verification
        result["mismatches"] = verification.get("mismatches", [])
        result["refusals"] = verification.get("refusals", 0)
    return result


def ingest_proof(root: Path) -> dict[str, Any]:
    """Read the live client/chat/wish trace and compare it with the recorded fixture.

    The live world is the proof. The fixture is only a drift reference and can never make a cell
    green; when a live store is missing the cell is LIVE_UNAVAILABLE and the build fails.
    """
    script = root / "tools" / "residents" / "prove-ingest.mjs"
    fixture = root / "tools" / "proofs" / "ingest"
    result: dict[str, Any] = {
        "script": str(script),
        "fixture": str(fixture),
        "state": "ABSENT",
        "source": "live",
        "detail": "",
    }
    if not script.is_file():
        result["detail"] = "tools/residents/prove-ingest.mjs is not present"
        return result
    payload, failure = run_json(
        ["node", str(script), "--json", "--compare", str(fixture)], root, timeout=120)
    if payload is None:
        result.update(state="BLOCKED", detail=failure)
        return result
    result["state"] = str(payload.get("status", "BLOCKED"))
    result["source"] = str(payload.get("source", "live"))
    result["live"] = payload.get("live", {})
    result["fixture"] = payload.get("fixture", {})
    result["drift"] = payload.get("drift", {})
    result["missing"] = payload.get("live", {}).get("missing", [])
    result["counts"] = payload.get("counts", {})
    result["trace"] = payload.get("trace", {})
    result["checks"] = payload.get("checks", [])
    result["failures"] = payload.get("failures", [])
    trace = result.get("trace") or {}
    if result["state"] == "PASS" and not all(trace.get(key) for key in ("client", "chat", "wish")):
        result["state"] = "EMPTY"
        result["detail"] = "one or more proof links produced no row"
    if result["state"] == "PASS" and result["missing"]:
        result["state"] = "LIVE_UNAVAILABLE"
        result["detail"] = "missing live path: " + ", ".join(result["missing"])
    if result["state"] == "PASS" and not result["fixture"] and not fixture.is_dir():
        result["detail"] = "live proof passed; fixture comparison is unavailable"
    if result["state"] != "PASS":
        result["detail"] = result.get("detail") or "; ".join(result.get("failures", [])) or "proof did not pass"
    return result


def mongo_parity(root: Path) -> dict[str, Any]:
    """Compare the durable append logs with their MongoDB projection, as a required gate.

    The store is not advisory: a projection that lags the ledger or holds rows the chain never
    committed is the drift this report refuses. The script names PASS/BEHIND/DRIFT/REFUSED; only
    PASS is a pass here.
    """
    script = root / "tools" / "mongo_parity.py"
    result: dict[str, Any] = {
        "script": str(script),
        "state": "NOT_RUN",
        "database": "ensouled_streams",
        "detail": "tools/mongo_parity.py was not run",
        "counts": {},
    }
    if not script.is_file():
        result.update(state="ABSENT",
                      detail="tools/mongo_parity.py is not present, so store drift cannot be checked")
        return result
    payload, failure = run_json([sys.executable, str(script), "--json"], root, timeout=180)
    if payload is None:
        result.update(state="REFUSED", detail=failure)
        return result
    for key in ("state", "detail", "counts", "samples", "uri", "database", "skipped"):
        if key in payload:
            result[key] = payload[key]
    result["state"] = str(payload.get("state", "REFUSED"))
    result["detail"] = str(payload.get("detail", "the store comparison produced no verdict"))
    return result


def openlock_parity(openlock: Path, install: str | None) -> dict[str, Any]:
    script = openlock / "tools" / "parity_check.py"
    result: dict[str, Any] = {
        "root": str(openlock),
        "script": str(script),
        "install": install,
        "state": "NOT_RUN",
        "detail": "pass --openlock-install PATH (or -PopenlockInstall=PATH) to run the install gate",
    }
    if not install:
        return result
    if not script.is_file():
        result.update(state="ABSENT", detail="tools/parity_check.py is not present")
        return result
    if not Path(install).exists():
        result.update(state="ABSENT", detail=f"install path does not exist: {install}")
        return result
    payload, failure = run_json([sys.executable, str(script), "--install", install, "--json"],
                                openlock, timeout=90)
    if payload is None:
        result.update(state="BLOCKED", detail=failure)
        return result
    result["state"] = str(payload.get("state", "BLOCKED"))
    result["detail"] = str(payload.get("detail", ""))
    result["run"] = payload.get("run")
    result["commit"] = payload.get("commit")
    cells = payload.get("cells")
    result["cells"] = len(cells) if isinstance(cells, list) else payload.get("cells", 0)
    audits = payload.get("audits")
    if isinstance(audits, list):
        result["audits"] = len(audits)
    return result


def build_report(root: Path, openlock: Path, install: str | None) -> dict[str, Any]:
    docs, absent = load_design(root)
    report: dict[str, Any] = {
        "root": str(root),
        "docs": [],
        "docs_absent": absent,
        "degrees": [],
        "precursors": precursor_rows(root),
        "openlock": {
            "root": str(openlock),
            "axis": openlock_axis(openlock),
            "parity": openlock_parity(openlock, install),
        },
        "ingest": ingest_proof(root),
        "mongo": mongo_parity(root),
        "errors": [],
    }
    if not docs:
        return report

    report["docs"] = [
        {
            "label": doc.label,
            "path": str(doc.path),
            "phases": [phase.number for phase in doc.phases],
            "requirements": sum(len(phase.requirements) for phase in doc.phases),
        }
        for doc in docs
    ]
    rows, errors = phase_witnesses(root, docs)
    report["degrees"] = rows
    report["errors"] = errors
    if report["ingest"]["state"] != "PASS":
        report["errors"].append(
            f"ingest proof: {report['ingest']['state']} -- {report['ingest'].get('detail', '')}")
    if report["mongo"]["state"] in ("DRIFT", "REFUSED"):
        # A store that contradicts the chain, or cannot be read, is a refusal: the build must not
        # report a verified tree while its projection holds different history. BEHIND is named but
        # not fatal here: the mount is the only reconciler, and refusing the mount would refuse the
        # repair. `verifyStore` refuses BEHIND for every build/check until that mount has run.
        report["errors"].append(
            f"mongo projection: {report['mongo']['state']} -- {report['mongo'].get('detail', '')}")

    gameplay = [row for row in rows if row["number"] >= 1]
    report["summary"] = {
        "gameplay_phases": len(gameplay),
        "complete": sum(1 for row in gameplay
                        if row["requirements"]
                        and all(item["state"] == "held" for item in row["requirements"])),
        "with_source_witness": sum(1 for row in gameplay
                                   if any(item["state"] in ("held", "partial")
                                          for item in row["requirements"])),
        "phase_zero": next(({
            "complete": bool(row["requirements"]) and all(item["state"] == "held"
                                                          for item in row["requirements"]),
            "held": sum(1 for item in row["requirements"] if item["state"] == "held"),
            "partial": sum(1 for item in row["requirements"] if item["state"] == "partial"),
            "required": len(row["requirements"]),
        } for row in rows if row["number"] == 0), None),
    }
    return report


def render_human(report: dict[str, Any]) -> str:
    lines: list[str] = []
    lines.append("gameplay degrees -- design phases and compile-time source witnesses")
    lines.append(f"root: {report['root']}")
    if report["docs_absent"]:
        lines.append(f"design documents: ABSENT -- {report['docs_absent']}")
        lines.append("  no phase coverage is claimed from an absent document")
    else:
        lines.append("design lineage:")
        for doc in report["docs"]:
            numbers = doc["phases"]
            span = f"{numbers[0]}-{numbers[-1]}" if numbers else "-"
            lines.append(f"  {doc['label']:<6} phases {span:<5} "
                         f"{len(numbers)} phase(s), {doc['requirements']} requirement bullet(s)")
        lines.append("  v9 adds Phase 0; Phases 1-8 are the gameplay degrees and are retained.")
        lines.append("  source: docs/persistent_city_builder_tower_defense_design_analysis*.md")
        lines.append("")
        lines.append("degree  phase                                      required  held  partial")
        for row in report["degrees"]:
            requirements = row["requirements"]
            held = sum(1 for item in requirements if item["state"] == "held")
            partial = sum(1 for item in requirements if item["state"] == "partial")
            title = row["title"]
            if len(title) > 40:
                title = title[:37] + "..."
            lines.append(f"  {row['number']:>2}    {title:<40} {len(requirements):>8} "
                         f"{held:>5} {partial:>8}")
            for item in requirements:
                if item["state"] == "held":
                    lines.append(f"        held: {item['text']}")
                elif item["state"] == "partial":
                    lines.append(f"        partial: {item['text']}")
            gaps = sum(1 for item in requirements if item["state"] == "absent")
            if gaps:
                lines.append(f"        gap: {gaps} requirement(s) without a source witness")
        summary = report.get("summary") or {}
        if summary:
            lines.append("")
            lines.append(f"summary: {summary.get('complete', 0)}/{summary.get('gameplay_phases', 0)} "
                         f"gameplay phases complete; {summary.get('with_source_witness', 0)}/"
                         f"{summary.get('gameplay_phases', 0)} have any source witness.")
            phase_zero = summary.get("phase_zero")
            if phase_zero:
                lines.append(f"         Phase 0 boundary: {phase_zero['held']} held, "
                             f"{phase_zero['partial']} partial of {phase_zero['required']}.")
            lines.append("         A source witness is a file+marker at build time, not a live bot run.")
    lines.append("")
    ingest = report.get("ingest") or {}
    fixture = ingest.get("fixture") or {}
    fixture_path = fixture.get("path", "") if isinstance(fixture, dict) else ""
    fixture_state = fixture.get("status", "ABSENT") if isinstance(fixture, dict) else "ABSENT"
    lines.append("ingest -> training -> dynamic verbs (live stores; fixture is drift reference):")
    lines.append(f"  state   {ingest.get('state', 'UNKNOWN')}")
    lines.append(f"  source  {ingest.get('source', 'live')} {fixture_path}")
    counts = ingest.get("counts") or {}
    if counts:
        lines.append("  counts  " + "; ".join([
            f"training={counts.get('training_rows', 0)}",
            f"client={counts.get('client_rows', 0)}",
            f"frames={counts.get('frames', 0)}",
            f"verbs={counts.get('verbs', 0)}",
            f"sources={','.join(counts.get('sources', []))}",
            f"clients={','.join(counts.get('clients', []))}",
            f"taught={','.join(counts.get('skills_taught', []))}",
        ]))
    live = ingest.get("live") or {}
    if live.get("missing"):
        lines.append("  missing " + ", ".join(live.get("missing", [])))
    lines.append(f"  fixture {fixture_path} state={fixture_state}")
    drift = ingest.get("drift") or {}
    if drift:
        lines.append(f"  drift   {drift.get('status', 'UNKNOWN')}")
        if drift.get("counts"):
            lines.append("          " + "; ".join(drift.get("counts", [])))
        trace = drift.get("trace") or {}
        for key in ("client", "chat", "wish"):
            entry = trace.get(key) or {}
            if entry:
                ids = " ".join(f"{name}={value.get('live') if isinstance(value, dict) else value}"
                               for name, value in entry.items()
                               if name != "row_still_exists")
                lines.append(f"          {key} {ids} row_still_exists={entry.get('row_still_exists')}")
    trace = ingest.get("trace") or {}
    client = trace.get("client") or {}
    if client:
        training = client.get("training") or {}
        package = client.get("package") or {}
        mapping = client.get("mapping") or {}
        result = client.get("result") or {}
        lines.append(f"  client  {training.get('client')} {training.get('command')} -> "
                     f"{package.get('decision')} {package.get('skill')}:{package.get('verb')} "
                     f"({package.get('verb_basis')}, stands={package.get('stands')}); "
                     f"result={result.get('status')}; library={mapping.get('expected')}")
        lines.append(f"          training {training.get('file')}:{training.get('line')} "
                     f"verb_basis={training.get('verb_basis')} accepted={training.get('accepted')}")
    chat = trace.get("chat") or {}
    if chat:
        lines.append(f"  chat    {chat.get('frame_id')} \"{chat.get('utterance')}\" -> "
                     f"{chat.get('verb')} (frame={chat.get('frame_verb')}, "
                     f"basis={chat.get('frame_basis')}, library_count={chat.get('library_count')})")
    wish = trace.get("wish") or {}
    if wish:
        lines.append(f"  wish    {wish.get('ticket_id')} {wish.get('ticket_state')} "
                     f"wish={wish.get('ticket_wish_id')} -> "
                     f"{wish.get('resolved_skill')}:{wish.get('ticket_verb')} "
                     f"(basis={wish.get('ticket_basis')}, kind={wish.get('basis_kind')}, "
                     f"expected={wish.get('expected_client_basis')})")
        lines.append(f"          frame {wish.get('frame_id')} wish={wish.get('frame_wish_id')} "
                     f"\"{wish.get('frame_utterance')}\" verb={wish.get('frame_verb')} "
                     f"(operator's request, keyed by wish id); identity={wish.get('identity_match')}; "
                     f"plan_now={wish.get('plan_now') or 'none'}")
        lines.append(f"          decision {wish.get('decision_id') or 'none'} "
                     f"method={wish.get('decision_skill') or 'none'}:{wish.get('decision_verb') or 'none'} "
                     f"({wish.get('decision_basis') or 'unrecorded'}) "
                     f"cell={wish.get('decision_cell')} authorised={wish.get('decision_authorised')}; "
                     f"action=\"{wish.get('action')}\" library={wish.get('action_verb')}; "
                     f"identity={wish.get('identity_match')}; "
                     f"completion={wish.get('completion')} ({wish.get('completion_reason')})")
    for check in ingest.get("checks", []):
        lines.append(f"  check   {check.get('status')} {check.get('name')}: {check.get('file')}")
    if ingest.get("detail"):
        lines.append(f"  detail  {ingest.get('detail')}")
    for failure in ingest.get("failures", []):
        lines.append(f"  FAIL    {failure}")
    lines.append("")
    mongo = report.get("mongo") or {}
    lines.append("mongo projection (durable ledger/training -> ensouled_streams, read-back verified):")
    lines.append(f"  state   {mongo.get('state', 'UNKNOWN')}")
    if mongo.get("uri"):
        lines.append(f"  store   {mongo.get('uri')} / {mongo.get('database', 'ensouled_streams')}")
    counts = mongo.get("counts") or {}
    if counts:
        lines.append("  rows    local ledger={local_ledger_rows} training={local_training_rows}; "
                     "store events={store_events} training={store_training}".format(**counts))
    if mongo.get("detail"):
        lines.append(f"  detail  {mongo.get('detail')}")
    if mongo.get("state") != "PASS":
        for name in ("missing_events", "mismatched_events", "unverified_events", "extra_events",
                     "missing_training", "extra_training", "unverified_training",
                     "failed_verifications", "malformed_local"):
            if counts.get(name):
                lines.append(f"  drift   {name.replace('_', ' ')}={counts[name]}")
    lines.append("")
    lines.append("precursors (source witnesses; never phase credit):")
    for row in report["precursors"]:
        lines.append(f"  {row['name']:<12} {row['state']:<8} {row['detail']}")
    lines.append("")
    axis = report["openlock"]["axis"]
    lines.append("openlock axis (external; Openlock worktree/store, not this repository):")
    lines.append(f"  state   {axis.get('state', 'UNKNOWN')}")
    if axis.get("run") is not None:
        lines.append(f"  run     {axis.get('run')}")
    if axis.get("commit"):
        lines.append(f"  commit  {axis.get('commit')}")
    if axis.get("cells") is not None:
        lines.append(f"  cells   {axis.get('cells')}")
    verification = axis.get("verification")
    if isinstance(verification, dict):
        lines.append(f"  verify  result={verification.get('result')} "
                     f"cells={verification.get('cells')} refusals={verification.get('refusals')} "
                     f"mismatches={len(verification.get('mismatches', []))}")
    if axis.get("detail"):
        lines.append(f"  detail  {axis.get('detail')}")
    lines.append(f"  witness {axis.get('script')} verify --json")
    parity = report["openlock"]["parity"]
    lines.append("openlock parity (external; optional, writes its own run):")
    lines.append(f"  state   {parity.get('state', 'UNKNOWN')}")
    if parity.get("cells") is not None:
        lines.append(f"  cells   {parity.get('cells')}")
    if parity.get("detail"):
        lines.append(f"  detail  {parity.get('detail')}")
    lines.append(f"  witness {parity.get('script')} --install PATH --json")
    if report["errors"]:
        lines.append("")
        lines.append("report errors (the build should not treat this as a pass):")
        for error in report["errors"]:
            lines.append(f"  {error}")
    return "\n".join(lines)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        description="Report the city-builder gameplay phases and the external Openlock AXIS cell.")
    parser.add_argument("--root", default=str(ROOT), help="project root")
    parser.add_argument("--openlock", default=os.environ.get("OPENLOCK_ROOT", str(DEFAULT_OPENLOCK)),
                        help="Openlock checkout (default: OPENLOCK_ROOT or the local checkout)")
    parser.add_argument("--openlock-install", default=None,
                        help="installed shell path; enables the optional Openlock parity run")
    parser.add_argument("--json", action="store_true", help="emit the report as JSON")
    parser.add_argument("--out", default=None, help="also write the human report to this path")
    args = parser.parse_args(argv)

    root = Path(args.root).resolve()
    openlock = Path(args.openlock).resolve()
    try:
        report = build_report(root, openlock, args.openlock_install)
    except (OSError, ReportError, ValueError) as failure:
        print(f"gameplay degrees: REFUSED -- {type(failure).__name__}: {failure}", file=sys.stderr)
        return 2

    human = render_human(report)
    if args.out:
        out = Path(args.out)
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(human + "\n", encoding="utf-8")
    if args.json:
        print(json.dumps(report, indent=2))
    else:
        print(human)
    if report["errors"]:
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
