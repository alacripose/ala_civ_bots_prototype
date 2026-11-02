#!/usr/bin/env bash
# Operator probe for the served stream. The token is read from run/stream.token and never printed.
#
# Usage:
#   scripts/stream.sh head                 ledger head
#   scripts/stream.sh served Ash           one agent's served payload
#   scripts/stream.sh events Ash [after]   that agent's ledger rows (JSON page)
#   scripts/stream.sh unauth               prove the boundary refuses an unauthenticated read
set -uo pipefail

cd "$(dirname "$0")/.." || exit 1

PORT="${ENSOULED_STREAM_PORT:-7664}"
TOKEN="$(cat run/stream.token 2>/dev/null || true)"
COMMAND="${1:-head}"
shift || true

if [ -z "$TOKEN" ] && [ "$COMMAND" != "unauth" ]; then
	echo "stream: no run/stream.token — the server has not written one yet" >&2
	exit 1
fi

case "$COMMAND" in
	head)
		curl -s -H "Authorization: Bearer $TOKEN" "http://127.0.0.1:$PORT/api/ledger/head"
		;;
	served)
		curl -s -H "Authorization: Bearer $TOKEN" "http://127.0.0.1:$PORT/api/served?agent=${1:?agent name}"
		;;
	events)
		curl -s -H "Authorization: Bearer $TOKEN" \
			"http://127.0.0.1:$PORT/api/ledger/events?after=${2:-0}&agent=${1:?agent name}"
		;;
	unauth)
		curl -s -o /dev/null -w '%{http_code}\n' "http://127.0.0.1:$PORT/api/served?agent=${1:-Ash}"
		;;
	*)
		echo "usage: scripts/stream.sh head|served <agent>|events <agent> [after]|unauth" >&2
		exit 2
		;;
esac
