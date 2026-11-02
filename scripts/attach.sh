#!/usr/bin/env bash
# Attach an acceptance phase to the already-running development server (DEV-01).
# Never starts, stops or wipes a server: it submits `/ensouled harness <phase>` over the local RCON
# channel and waits for that run id's result line. The world and every connected session stay up.
#
# Usage: scripts/attach.sh <phase>     e.g. scripts/attach.sh chest
set -uo pipefail

cd "$(dirname "$0")/.." || exit 1

PHASE="${1:-}"
if [ -z "$PHASE" ]; then
	echo "usage: scripts/attach.sh <phase>   (chest, handoff, verbs, phase1, player1, phase2)" >&2
	exit 2
fi

if ! netstat -an 2>/dev/null | grep -q "0.0.0.0:25565 .*LISTENING"; then
	echo "No development server is listening on 25565. Start one with scripts/devserver.sh" >&2
	exit 1
fi

if [ ! -f run/rcon.password ]; then
	echo "No control password in run/rcon.password. Start the server with scripts/devserver.sh" >&2
	exit 1
fi

RCON_PORT="$(grep '^rcon.port=' run/server.properties 2>/dev/null | cut -d= -f2)"
RCON_PORT="${RCON_PORT:-25575}"

RESPONSE="$(python scripts/rcon.py 127.0.0.1 "$RCON_PORT" "$(cat run/rcon.password)" "ensouled harness $PHASE")"
STATUS=$?
echo "$RESPONSE"
if [ "$STATUS" -ne 0 ]; then
	echo "control channel failed (status $STATUS); the dev server is unchanged" >&2
	exit 1
fi

RUN_ID="$(printf '%s' "$RESPONSE" | grep -o 'run=[A-Za-z0-9_-]*' | head -1 | cut -d= -f2)"
if [ -z "$RUN_ID" ]; then
	echo "the server did not start a harness run; the dev server is unchanged" >&2
	exit 1
fi

echo "== attached run=$RUN_ID phase=$PHASE; waiting for its result (server stays up) =="
for _ in $(seq 1 900); do
	LINE="$(grep "run=$RUN_ID" run/ensouled-selftest-result.txt 2>/dev/null | tail -1)"
	if [ -n "$LINE" ]; then
		echo "$LINE"
		if printf '%s' "$LINE" | grep -q "SELFTEST PASS"; then
			exit 0
		fi
		exit 1
	fi
	sleep 1
done

echo "timed out waiting for run=$RUN_ID; cancel it with /ensouled harness cancel in game (the server stays up)" >&2
exit 1
