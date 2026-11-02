#!/usr/bin/env bash
# Disposable CI acceptance harness. This is the ONLY mode that boots its own server, and it can
# only ever delete the universe directory it created (run/selftest-universe). The persistent dev
# world (run/world) is never referenced here.
#
# Interactive work uses scripts/devserver.sh + scripts/attach.sh instead.
# Usage: scripts/selftest.sh [mob|player|verbs|handoff|chest]
set -uo pipefail

cd "$(dirname "$0")/.." || exit 1
mkdir -p run logs

CI_UNIVERSE="run/selftest-universe"
CI_MARKER="$CI_UNIVERSE/.harness-owned"

if [ -z "${JAVA_HOME:-}" ]; then
	echo "JAVA_HOME must point at a JDK 17+ (this project was built on JDK 21)" >&2
	exit 1
fi

if [ ! -f run/eula.txt ]; then
	printf 'eula=true\n' > run/eula.txt
fi

if [ ! -f run/server.properties ]; then
	cat > run/server.properties <<'EOF'
level-name=world
level-type=minecraft\:flat
online-mode=false
spawn-protection=0
max-tick-time=-1
view-distance=4
simulation-distance=4
sync-chunk-writes=false
allow-nether=false
enable-command-block=false
difficulty=peaceful
generate-structures=false
level-seed=ensouled-selftest
EOF
fi

# The probe world must be deterministic. Generated structures (plains villages use log blocks as
# corner posts) are real harvestable oak logs: a being that finds one legitimately collects it,
# which would break the no-second-collection assertion through no fault of the code under test.
ensure_property() {
	local key="$1" value="$2"
	if grep -q "^${key}=" run/server.properties; then
		sed -i "s|^${key}=.*|${key}=${value}|" run/server.properties
	else
		printf '%s=%s\n' "$key" "$value" >> run/server.properties
	fi
}
ensure_property level-seed ensouled-selftest
ensure_property generate-structures false
# The CI world lives inside the disposable universe, never in run/world.
ensure_property level-name world

# Deletion is allowed only for the directory this script created and marked. A directory without
# the marker is refused, so a mistyped path or a developer's own save can never be removed here.
delete_ci_universe() {
	if [ ! -e "$CI_UNIVERSE" ]; then
		return 0
	fi
	if [ ! -f "$CI_MARKER" ]; then
		echo "REFUSING to delete $CI_UNIVERSE: it has no $CI_MARKER marker, so this script did not create it" >&2
		exit 1
	fi
	rm -rf "$CI_UNIVERSE"
	echo "== removed disposable CI universe $CI_UNIVERSE =="
}

# A probe world must not carry minds or player data from an earlier run: restored beings walk into
# the new fixture, answer requests from their old inventories, and pick up the probe's drops. That
# contamination is what made a dropped stack sit untouched next to the bots (INT-06).
clean_probe_state() {
	delete_ci_universe
	rm -f run/ensouled-selftest-phase1.txt
	echo "== probe state cleared (disposable CI universe only) =="
}

run_phase() {
	local phase="$1"
	mkdir -p "$CI_UNIVERSE"
	if [ ! -f "$CI_MARKER" ]; then
		printf 'created by scripts/selftest.sh for a disposable probe world\n' > "$CI_MARKER"
	fi
	: > run/ensouled-selftest-result.txt
	printf '%s' "$phase" > run/ensouled-selftest.txt
	echo "== running $phase (disposable CI: $CI_UNIVERSE) =="
	# Own socket: a human client on the normal dev port must not be able to join the probe world and
	# mine its fixtures (which silently invalidates every world-state assertion in the harness).
	# Production embodiment is a connected client (V4 §44.3), so persisted minds are not stood up as
	# in-process fake players by default. The acceptance harness builds its own fixture bodies and
	# needs that legacy path, which is why this disposable CI run asks for it explicitly.
	ENSOULED_SELFTEST=1 \
		ENSOULED_FAKE_BODIES=1 \
		ENSOULED_SELFTEST_UNIVERSE="$PWD/$CI_UNIVERSE" \
		ENSOULED_SELFTEST_PORT="${ENSOULED_SELFTEST_PORT:-25579}" \
		./gradlew runServer --no-daemon > "logs/selftest-$phase.log" 2>&1
	echo "gradle exit=$? (see logs/selftest-$phase.log)"
	if ! grep -q "SELFTEST PASS phase=$phase" run/ensouled-selftest-result.txt 2>/dev/null; then
		echo "FAILED: $phase did not pass" >&2
		grep "SELFTEST" "logs/selftest-$phase.log" >&2 || true
		exit 1
	fi
	echo "PASS: $phase"
}

# Mode: "mob" (default, secondary body), "player" (primary fake-player body), "verbs"
# (jump / equip / use probe on one player body), "handoff" (two citizens settle one food
# transfer through a real request/accept exchange) or "chest" (two citizens settle one
# storage-backed resource transfer through the gated chest interaction).
#
# The human path is not automated here on purpose: a simulated human is not a human. Join the
# dev server with a real client and run `/ensouled humanprobe` instead (see README).
BODY="${1:-mob}"
if [ "$BODY" = "verbs" ] || [ "$BODY" = "handoff" ] || [ "$BODY" = "chest" ]; then
	clean_probe_state
	run_phase "$BODY"
	echo "== results =="
	cat run/ensouled-selftest-result.txt
	delete_ci_universe
	exit 0
fi

HARVEST_PHASE="phase1"
if [ "$BODY" = "player" ]; then
	HARVEST_PHASE="player1"
fi

clean_probe_state
run_phase "$HARVEST_PHASE"
run_phase phase2

echo "== results =="
cat run/ensouled-selftest-result.txt
delete_ci_universe
