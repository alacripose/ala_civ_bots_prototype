#!/usr/bin/env bash
# Project-local MongoDB for the per-civilian stream store.
#
# The store is required: the served stream is read from one agent's own stream, and with no store
# the streamer refuses to serve rather than merging every citizen's notices into one feed. A local
# instance under run/mongo keeps that dependency inside the project; no service install, no admin.
#
# Usage: scripts/mongo.sh start|stop|status
set -uo pipefail

cd "$(dirname "$0")/.." || exit 1

BIN="${MONGO_BIN:-/c/Program Files/MongoDB/Server/8.3/bin/mongod.exe}"
PORT="${MONGO_PORT:-27018}"
DBPATH="$PWD/run/mongo"
LOG="$PWD/logs/mongod.log"

listening() {
	netstat -an 2>/dev/null | grep -q "127.0.0.1:$PORT .*LISTENING"
}

case "${1:-status}" in
	start)
		if listening; then
			echo "mongo: already listening on 127.0.0.1:$PORT"
			exit 0
		fi
		if [ ! -x "$BIN" ]; then
			echo "mongo: no mongod at $BIN (set MONGO_BIN)" >&2
			exit 1
		fi
		mkdir -p "$DBPATH" logs
		"$BIN" --dbpath "$(cygpath -w "$DBPATH")" --port "$PORT" --bind_ip 127.0.0.1 \
			--logpath "$(cygpath -w "$LOG")" --logappend > /dev/null 2>&1 &
		echo $! > run/mongo.pid
		for _ in $(seq 1 20); do
			sleep 1
			if listening; then
				echo "mongo: up on 127.0.0.1:$PORT (dbpath run/mongo, log logs/mongod.log)"
				exit 0
			fi
		done
		echo "mongo: did not come up; see logs/mongod.log" >&2
		exit 1
		;;
	stop)
		if [ -f run/mongo.pid ] && kill -0 "$(cat run/mongo.pid)" 2>/dev/null; then
			kill "$(cat run/mongo.pid)"
			rm -f run/mongo.pid
			echo "mongo: stopped"
		else
			echo "mongo: not started by this script"
		fi
		;;
	status)
		if listening; then
			echo "mongo: listening on 127.0.0.1:$PORT"
		else
			echo "mongo: not listening on 127.0.0.1:$PORT"
		fi
		;;
	*)
		echo "usage: scripts/mongo.sh start|stop|status" >&2
		exit 2
		;;
esac
