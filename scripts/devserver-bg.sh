#!/usr/bin/env bash
# Start the persistent dev server in a way that outlives the shell that asked for it.
#
# `scripts/devserver.sh` inherits JAVA_HOME from its caller and runs gradle as a child of that shell;
# when the caller is an agent's tool shell, stopping that shell takes the server down with it. This
# wrapper finds the build JDK itself and execs, so the process tree has no shell above it that
# anybody is going to interrupt.
set -uo pipefail

cd "$(dirname "$0")/.." || exit 1

if [ -z "${JAVA_HOME:-}" ]; then
	for candidate in "/c/Program Files/Eclipse Adoptium"/jdk-21*; do
		[ -x "$candidate/bin/java" ] && JAVA_HOME="$candidate" && break
	done
fi
if [ -z "${JAVA_HOME:-}" ]; then
	echo "devserver-bg: no JDK 21 found; set JAVA_HOME" >&2
	exit 1
fi
export JAVA_HOME

exec bash scripts/devserver.sh
