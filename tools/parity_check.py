#!/usr/bin/env python3
"""Is the jar on disk the jar this worktree compiles?

The identity is read **out of the installed artifact itself** - the ``version`` field in its
``fabric.mod.json``, which the build expands from the commit and the digest of ``src/main`` (see
``build.gradle``). A copy that was hand-placed, cached by a launcher, or left behind by an earlier
build therefore cannot claim to be the current tree: the number inside it disagrees.

This is the Minecraft counterpart of openlock's ``tools/parity_check.py``. Fabric has no mounted
archive to validate and no install grade to select - the loader reads every jar in ``mods/`` - so
the only questions that survive here are the ones that matter: how many builds are present, which
one is it, and does it match the sources. The Valve-specific parts (VPK header parsing, gameinfo.gi
search paths, addon roots, the fleet of seeded installs) are deliberately not ported.

Usage::

    python tools/parity_check.py [--json] [--existing] [target ...]

Targets are ``build``, ``server`` and ``client``; naming none checks all of them. Exit code 0 only
when every checked target holds exactly the build this worktree would produce. ``--existing`` drops
a target whose directory does not exist at all: a copy that was never installed cannot be stale,
while a directory that exists without this mod, or with a different build, still fails.
"""

from __future__ import annotations

import hashlib
import json
import os
import subprocess
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SOURCES = ROOT / "src" / "main"
MODULE_JSON = "fabric.mod.json"

# Where this worktree actually puts the mod jar. The bot clients share one immutable mods
# directory (`scripts/bots.sh`), and the server runs from its own (`scripts/devserver.sh`).
TARGETS = [
    ("build", ROOT / "build" / "libs"),
    ("server", ROOT / "run" / "mods"),
    ("client", ROOT / "tools" / "botprofile" / "mods"),
]


def prism_instance_dir() -> Path | None:
    """Where the launcher the developer actually runs keeps its instances.

    Prism's instance directory is a launcher setting, not a constant: it is read from
    ``prismlauncher.cfg`` so the developer's own client is checked, not a default path that may
    hold a second, stale copy of the same instance.
    """
    data = os.environ.get("PRISM_DATA")
    base = Path(data) if data else Path(os.environ.get("APPDATA", "")) / "PrismLauncher"
    config = base / "prismlauncher.cfg"
    if not config.is_file():
        return None
    for line in config.read_text(encoding="utf-8", errors="replace").splitlines():
        key, _, value = line.partition("=")
        if key.strip() == "InstanceDir" and value.strip():
            directory = Path(value.strip().replace("\\", "/"))
            return directory if directory.is_absolute() else base / directory
    return None


def prism_targets() -> list[tuple[str, Path]]:
    """Every launcher instance that already carries this mod. Untouched instances stay out."""
    directory = prism_instance_dir()
    if directory is None or not directory.is_dir():
        return []
    rows = []
    for instance in sorted(directory.iterdir()):
        for mods in (instance / "minecraft" / "mods", instance / ".minecraft" / "mods"):
            if mods.is_dir() and ensouled_jars(mods):
                rows.append((f"prism:{instance.name}", mods))
                break
    return rows


def gradle_property(name: str, default: str = "") -> str:
    path = ROOT / "gradle.properties"
    for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
        line = line.strip()
        if line.startswith("#") or "=" not in line:
            continue
        key, _, value = line.partition("=")
        if key.strip() == name:
            return value.strip()
    return default


def git(*args: str) -> str:
    """Empty when there is no repository: the build version drops the commit in that case."""
    try:
        result = subprocess.run(["git", *args], cwd=ROOT, capture_output=True, text=True)
    except OSError:
        return ""
    if result.returncode != 0:
        return ""
    return result.stdout.strip()


def source_digest() -> str:
    """The same digest `build.gradle` computes: every file under src/main, by absolute path."""
    digest = hashlib.sha256()
    files = []
    for directory, _dirs, names in os.walk(SOURCES):
        for name in names:
            files.append(os.path.abspath(os.path.join(directory, name)))
    for path in sorted(files):
        digest.update(path.encode("utf-8"))
        with open(path, "rb") as handle:
            digest.update(handle.read())
    return digest.hexdigest()[:8]


def expected_version() -> str:
    base = gradle_property("mod_version", "0.0.0")
    digest = source_digest()
    commit = git("rev-parse", "--short=8", "HEAD")
    if not commit:
        return f"{base}+src.{digest}"
    dirty = "-dirty" if git("status", "--porcelain") else ""
    return f"{base}+g{commit}{dirty}.src.{digest}"


def jar_version(path: Path) -> str | None:
    """The version the jar states about itself, or None when the jar cannot be read."""
    try:
        with zipfile.ZipFile(path) as archive:
            raw = archive.read(MODULE_JSON)
        return json.loads(raw.decode("utf-8")).get("version")
    except (OSError, KeyError, zipfile.BadZipFile, ValueError):
        return None


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with open(path, "rb") as handle:
        for chunk in iter(lambda: handle.read(1 << 20), b""):
            digest.update(chunk)
    return digest.hexdigest()


def ensouled_jars(directory: Path) -> list[Path]:
    if not directory.is_dir():
        return []
    jars = [
        path
        for path in directory.glob("ensouled-*.jar")
        if not path.name.endswith("-sources.jar")
    ]
    return sorted(jars, key=lambda path: path.stat().st_mtime, reverse=True)


def inspect(name: str, directory: Path, expected: str, built: Path | None) -> dict:
    """One target's reading. `build` may hold several builds; a mods directory may hold one."""
    jars = ensouled_jars(directory)
    row = {
        "target": name,
        "directory": str(directory),
        "jars": [path.name for path in jars],
        "expected": expected,
        "state": "ok",
        "detail": "",
    }
    if not directory.is_dir():
        row.update(state="absent", detail="the directory does not exist yet")
        return row
    if not jars:
        row.update(state="missing", detail="no ensouled jar is installed")
        return row
    if name == "build":
        newest = jars[0]
        if len(jars) > 1:
            row["detail"] = f"{len(jars)} builds in output; comparing the newest"
        jar = newest
    else:
        jar = jars[0]
        if len(jars) > 1:
            # Fabric loads whatever it finds; two builds in one mods directory is undefined order.
            row.update(state="multiple", detail=f"{len(jars)} builds installed; the loader picks one")
            row["installed"] = jar_version(jar)
            return row
    version = jar_version(jar)
    row["installed"] = version
    row["jar"] = jar.name
    row["sha256"] = sha256(jar)
    if version is None:
        row.update(state="unreadable", detail=f"{jar.name} has no readable {MODULE_JSON}")
        return row
    if version != expected:
        row.update(state="stale", detail=f"installed {version}, this worktree builds {expected}")
        return row
    if name != "build" and built is not None and row["sha256"] != built:
        # Same sources, different bytes: the copy predates the current build (a dependency or the
        # manifest changed, or it was packed at another time). The sources agree, the artifact does
        # not, and a launch should pick up the built one rather than guess.
        row.update(state="differs", detail=f"same sources, but not the jar in {ROOT / 'build' / 'libs'}")
        return row
    return row


def main(argv: list[str]) -> int:
    as_json = "--json" in argv
    existing_only = "--existing" in argv
    wanted = [arg for arg in argv[1:] if arg not in ("--json", "--existing")]
    targets = TARGETS + prism_targets()
    known = [name for name, _directory in targets]
    groups = {name.split(":")[0] for name in known}
    unknown = [arg for arg in wanted if arg not in known and arg not in groups]
    if unknown:
        print(f"parity_check: unknown target {unknown[0]} (known: {', '.join(known)})", file=sys.stderr)
        return 2
    selected = [(name, directory) for name, directory in targets
                if not wanted or name in wanted or name.split(":")[0] in wanted]

    expected = expected_version()
    build_jars = ensouled_jars(ROOT / "build" / "libs")
    built_sha = sha256(build_jars[0]) if build_jars else None
    rows = [inspect(name, directory, expected, built_sha) for name, directory in selected]
    if existing_only:
        rows = [row for row in rows if row["state"] != "absent"]
    failed = [row for row in rows if row["state"] != "ok"]

    if as_json:
        print(json.dumps({"expected": expected, "targets": rows, "ok": not failed}, indent=2))
        return 1 if failed else 0

    print(f"worktree build: {expected}")
    cover = {
        "ok": "ok",
        "stale": "STALE",
        "missing": "MISSING",
        "absent": "absent",
        "multiple": "MULTIPLE",
        "differs": "DIFFERS",
        "unreadable": "UNREADABLE",
    }
    for row in rows:
        state = cover[row["state"]]
        installed = row.get("installed") or "-"
        print(f"  {row['target']:<7} {state:<10} {installed}")
        if row["detail"] and row["state"] != "ok":
            print(f"          {row['detail']}")
    if failed:
        print(f"parity: {len(failed)} target(s) do not carry this worktree; rebuild before launching.")
        return 1
    print("parity: every target carries this worktree.")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
