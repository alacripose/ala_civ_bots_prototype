#!/usr/bin/env bash
# The developer's own client, in the launcher they actually run.
#
# The bot clients belong to scripts/bots.sh. This is the other one: the Prism instance a human joins
# the dev server with. It has to run the same build as the server, so the jar is taken from
# build/libs, replaces any earlier ensouled jar, and is gated by tools/parity_check.py - the reason
# the instance can drift is that nothing in the worktree ever wrote to it. `open` starts that
# instance through the launcher's own command line, so the client does not have to be found by hand.
#
# Usage:
#   scripts/client.sh sync [instance...]    install the built mod and verify the instance
#   scripts/client.sh open [instance...]    launch the instance in Prism
#   scripts/client.sh start [instance...]   sync, then open (the default)
#   scripts/client.sh status                every instance that carries this mod
#
# Environment: CLIENT_INSTANCE (default 1.20.1), PRISM_EXE, PRISM_DATA.
set -uo pipefail

cd "$(dirname "$0")/.." || exit 1

ROOT="$PWD"
INSTANCE="${CLIENT_INSTANCE:-1.20.1}"

prism_data() {
	if [ -n "${PRISM_DATA:-}" ]; then
		printf '%s' "$PRISM_DATA"
		return
	fi
	printf '%s/PrismLauncher' "${APPDATA:?APPDATA is not set; set PRISM_DATA}"
}

prism_exe() {
	if [ -n "${PRISM_EXE:-}" ]; then
		printf '%s' "$PRISM_EXE"
		return
	fi
	printf '%s/Programs/PrismLauncher/prismlauncher.exe' "${LOCALAPPDATA:?LOCALAPPDATA is not set; set PRISM_EXE}"
}

# The instance directory is a launcher setting, so it is read from the launcher's own config: the
# default instances folder is not where this machine keeps them, and checking the wrong one is how
# a second, stale copy of the same instance looks correct.
instance_root() {
	local dir
	dir="$(sed -n 's/^InstanceDir=//p' "$(prism_data)/prismlauncher.cfg" 2>/dev/null | head -1)"
	dir="${dir%$'\r'}"
	case "$dir" in
		"") dir="$(prism_data)/instances" ;;
		[A-Za-z]:[\\/]* | /*) : ;;
		*) dir="$(prism_data)/$dir" ;;
	esac
	printf '%s' "${dir//\\//}"
}

mods_dir() {
	local root
	root="$(instance_root)/$1"
	if [ -d "$root/minecraft/mods" ]; then
		printf '%s' "$root/minecraft/mods"
	elif [ -d "$root/.minecraft/mods" ]; then
		printf '%s' "$root/.minecraft/mods"
	else
		printf '%s' "$root/minecraft/mods"
	fi
}

built_jar() {
	ls -t "$ROOT"/build/libs/ensouled-*.jar 2>/dev/null | grep -v sources | head -1
}

sync_one() {
	local instance="$1" jar mods
	jar="$(built_jar)"
	if [ -z "$jar" ]; then
		echo "no built jar in build/libs; run ./gradlew remapJar first" >&2
		return 1
	fi
	mods="$(mods_dir "$instance")"
	if [ ! -d "$mods" ]; then
		echo "$instance: no mods directory at $mods" >&2
		return 1
	fi
	# One ensouled jar only: Fabric loads every jar in the directory, so an older build must be
	# replaced rather than sit beside the new one.
	# A running client holds its jar open, so the delete below can fail. Removing the old build is
	# not optional: two ensouled jars in one mods directory is the loader picking one of them, which
	# is exactly the drift the parity gate then reports. Say it here rather than deleting silently.
	find "$mods" -maxdepth 1 -name 'ensouled*.jar' ! -name "$(basename "$jar")" -delete 2>/dev/null
	local stale
	stale="$(find "$mods" -maxdepth 1 -name 'ensouled*.jar' ! -name "$(basename "$jar")" 2>/dev/null)"
	if [ -n "$stale" ]; then
		echo "$instance: an old build is still in $mods and cannot be removed (close the game first):" >&2
		echo "  $stale" >&2
		return 1
	fi
	if ! cmp -s "$jar" "$mods/$(basename "$jar")"; then
		if ! cp -f "$jar" "$mods/$(basename "$jar")" 2>/dev/null; then
			echo "$instance: could not install $(basename "$jar") - close the client and retry" >&2
			return 1
		fi
		echo "$instance: installed $(basename "$jar")"
	fi
	python "$ROOT/tools/parity_check.py" "prism:$instance" || {
		echo "$instance: refusing - the instance does not carry this worktree" >&2
		return 1
	}
}

open_one() {
	local instance="$1" exe log
	exe="$(prism_exe)"
	if [ ! -f "$exe" ]; then
		echo "no Prism launcher at $exe; set PRISM_EXE" >&2
		return 1
	fi
	mkdir -p logs
	log="logs/client-${instance}.log"
	# Prism is single-instance: when the launcher is already running, this process hands the command
	# over and exits straight away, writing nothing. That is a launch that worked, not a launch that
	# failed, so it is named here rather than left as an empty log beside a pid that is already gone.
	nohup "$exe" --launch "$instance" > "$log" 2>&1 &
	local pid=$!
	echo "$instance: opening (pid $pid, log $log)"
	sleep 3
	if [ -s "$log" ]; then
		echo "$instance: launcher wrote to $log:" >&2
		tail -5 "$log" >&2
		return 1
	fi
	printf '%s: %s\n' "$instance" \
		"handed to the running launcher (no window of its own); instance log: $(instance_root)/$instance/minecraft/logs/latest.log"
}

ACTION="${1:-start}"
shift || true

case "$ACTION" in
	sync)
		if [ $# -eq 0 ]; then set -- "$INSTANCE"; fi
		for instance in "$@"; do
			sync_one "$instance" || exit 1
		done
		;;
	open)
		if [ $# -eq 0 ]; then set -- "$INSTANCE"; fi
		for instance in "$@"; do
			open_one "$instance" || exit 1
		done
		;;
	start)
		if [ $# -eq 0 ]; then set -- "$INSTANCE"; fi
		for instance in "$@"; do
			sync_one "$instance" || exit 1
		done
		for instance in "$@"; do
			open_one "$instance" || exit 1
		done
		;;
	status)
		python "$ROOT/tools/parity_check.py"
		;;
	*)
		echo "usage: scripts/client.sh [sync|open|start|status] [instance...]" >&2
		exit 2
		;;
esac
