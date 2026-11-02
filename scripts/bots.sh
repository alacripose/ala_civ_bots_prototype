#!/usr/bin/env bash
# Headless bot clients (V4 §44). One isolated Fabric client process per resident being, each an
# ordinary player session on the shared dev server; nothing here launches a server.
#
# Isolation follows §44.5: immutable jars are shared (one mods directory, linked per bot), while
# everything mutable — options, config, logs, player data — is per agent. The launcher writes an
# options.txt that keeps a headless bot quiet and cheap: narrator off, no accessibility onboarding,
# master audio muted, low render distance, and no pause when the window is not focused.
#
# Usage:
#   scripts/bots.sh start [name...]     default: Ash Rest Bramble
#   scripts/bots.sh stop  [name...]
#   scripts/bots.sh status
#
# Environment: MC_HOST (default 127.0.0.1), MC_PORT (default 25565), BOT_MEMORY (default 1G).
set -uo pipefail

cd "$(dirname "$0")/.." || exit 1

ROOT="$PWD"
HMC_JAR="$ROOT/tools/headlessmc/headlessmc.jar"
SHARED_MODS="$ROOT/tools/botprofile/mods"
BOTS="$ROOT/run/bots"
HOST="${MC_HOST:-127.0.0.1}"
PORT="${MC_PORT:-25565}"
MEMORY="${BOT_MEMORY:-1G}"
# Residents, in the corpus's order. A bot's name is not chosen here: it comes out of the same
# prose-logic corpus as its speech (ResidentNames.java, `ensouled residents` prints the roster), and
# the server only binds a session whose name is on that roster. A name invented in this script would
# never be able to become anybody, so the two lists are kept in the same order on purpose.
DEFAULT_BOTS=(Ash Rest Bramble)

# The launcher itself is a Java 25 application; the game it starts gets its own JVM. They are two
# different runtimes on purpose, so JAVA_HOME (the build JDK) is not used for the launcher.
if ! HMC_JAVA="$(command -v "${HMC_JAVA:-java}")"; then
	echo "a Java 25 runtime is required to run HeadlessMC (set HMC_JAVA)" >&2
	exit 1
fi

mkdir -p "$BOTS" logs/bots

# `kill -0` is not a reliable liveness check here: on Windows it can report success for a pid that
# no longer exists, so a stopped bot reads as "already running" and is never restarted. Ask the OS.
alive() {
	local pid="$1"
	[ -n "$pid" ] || return 1
	if command -v tasklist >/dev/null 2>&1; then
		tasklist //FI "PID eq $pid" 2>/dev/null | grep -q " $pid "
	else
		kill -0 "$pid" 2>/dev/null
	fi
}

# A bot's game directory: mutable state only. The mods directory is a junction/link to the shared
# immutable jars, so a rebuilt mod jar reaches every bot without copying 85 MB per being.
prepare() {
	local name="$1"
	local dir="$BOTS/$name"
	mkdir -p "$dir/logs" "$dir/config"
	if [ ! -e "$dir/mods" ]; then
		if ! cmd //c mklink //J "$(cygpath -w "$dir/mods")" "$(cygpath -w "$SHARED_MODS")" >/dev/null 2>&1; then
			ln -s "$SHARED_MODS" "$dir/mods" 2>/dev/null || cp -r "$SHARED_MODS" "$dir/mods"
		fi
	fi
	# Headless means no display and no audio device (§44.5); these are presentation settings only and
	# change no gameplay state.
	cat > "$dir/options.txt" <<'EOF'
version:3465
narrator:0
onboardAccessibility:false
soundCategory_master:0.0
soundCategory_music:0.0
soundCategory_voice:0.0
renderDistance:2
simulationDistance:4
graphicsMode:0
maxFps:30
enableVsync:false
guiScale:2
pauseOnLostFocus:false
skipMultiplayerWarning:true
tutorialStep:none
joinedFirstServer:true
chatVisibility:0
chatOpacity:0.0
autoJump:true
EOF
	printf '{"servers":[{"name":"dev","ip":"%s:%s"}]}\n' "$HOST" "$PORT" > "$dir/servers.dat"
}

# The clients must run the mod that was just built, not the jar from whenever the profile was set
# up: a stale client jar is a client whose control bridge is older than the server's. The parity
# gate answers that from the jar's own baked version (tools/parity_check.py), so a mods directory
# holding another worktree's build - or two builds at once - stops the launch instead of letting
# the loader pick one.
sync_own_mod() {
	local jar
	jar="$(ls -t "$ROOT"/build/libs/ensouled-*.jar 2>/dev/null | grep -v sources | head -1)"
	if [ -z "$jar" ]; then
		echo "no built jar in build/libs; run ./gradlew build before starting the bots" >&2
		return 1
	fi
	# One ensouled jar only: the shared mods directory is immutable jars plus this one, and a
	# versioned build name must replace the previous build rather than sit next to it. The glob is
	# `ensouled*`, not `ensouled-*`, because a plain `ensouled.jar` from an earlier hand-install
	# only matches the wider one — under the narrow glob it survived and the loader ran it.
	find "$SHARED_MODS" -maxdepth 1 -name 'ensouled*.jar' ! -name "$(basename "$jar")" -delete 2>/dev/null
	# A running bot holds its jar open; the old build then survives the delete and the loader picks
	# one of two. Report it instead of launching on a maybe.
	STALE_MODS="$(find "$SHARED_MODS" -maxdepth 1 -name 'ensouled*.jar' ! -name "$(basename "$jar")" 2>/dev/null)"
	if [ -n "$STALE_MODS" ]; then
		echo "an old build is still in $SHARED_MODS and cannot be removed (stop the bots first):" >&2
		echo "  $STALE_MODS" >&2
		return 1
	fi
	if ! cmp -s "$jar" "$SHARED_MODS/$(basename "$jar")"; then
		cp -f "$jar" "$SHARED_MODS/$(basename "$jar")"
		echo "client mod synced: $(basename "$jar")"
	fi
	python "$ROOT/tools/parity_check.py" client || {
		echo "refusing to launch: the client mods directory does not carry this worktree" >&2
		return 1
	}
}

profile_for() {
	local name="$1"
	local dir="$BOTS/$name"
	"$HMC_JAVA" -jar "$HMC_JAR" profile list 2>/dev/null | awk '{print $1}' | grep -qx "bot-$name" \
		|| "$HMC_JAVA" -jar "$HMC_JAR" profile add -n "bot-$name" -d "$dir" fabric 1.20.1 >/dev/null 2>&1
}

start_one() {
	local name="$1"
	prepare "$name"
	profile_for "$name"
	if [ -f "$BOTS/$name.pid" ] && alive "$(cat "$BOTS/$name.pid")"; then
		echo "$name: already running (pid $(cat "$BOTS/$name.pid"))"
		return 0
	fi
	"$HMC_JAVA" -jar "$HMC_JAR" launch --offline -lwjgl \
		-j "-Xmx$MEMORY -Djava.awt.headless=true" \
		-g "--username $name" --server "$HOST:$PORT" \
		"bot-$name" >> "logs/bots/$name.log" 2>&1 &
	echo $! > "$BOTS/$name.pid"
	echo "$name: launching as a headless client (pid $!, log logs/bots/$name.log)"
}

# The recorded pid is the launcher, not the game, and a launcher exits once its client is running -
# so that number can belong to an unrelated process by the time `stop` is called. Ownership is
# re-established from the process's own command line before anything is signalled: killing by a
# reused pid (or by its tree) takes down whatever inherited the number, up to and including a dev
# server started in the meantime.
launcher_owned() {
	local pid="$1"
	if ! command -v powershell >/dev/null 2>&1; then
		return 0
	fi
	powershell -NoProfile -Command \
		"if ((Get-CimInstance Win32_Process -Filter \"ProcessId=$pid\").CommandLine -like '*headlessmc.jar*') { exit 0 } else { exit 1 }" \
		>/dev/null 2>&1
}

child_pids() {
	local pid="$1"
	command -v powershell >/dev/null 2>&1 || return 0
	powershell -NoProfile -Command \
		"Get-CimInstance Win32_Process -Filter \"ParentProcessId=$pid\" | Select-Object -ExpandProperty ProcessId" \
		2>/dev/null | tr -d '\r' | grep -E '^[0-9]+$'
}

stop_one() {
	local name="$1"
	if [ ! -f "$BOTS/$name.pid" ]; then
		echo "$name: not running"
		return 0
	fi
	local pid child
	pid="$(cat "$BOTS/$name.pid")"
	if alive "$pid"; then
		if ! launcher_owned "$pid"; then
			echo "$name: pid $pid is not a HeadlessMC launcher any more; stale pid cleared, nothing signalled" >&2
			rm -f "$BOTS/$name.pid"
			return 0
		fi
		# The launcher is not the game: HeadlessMC forks the client JVM, so killing only the
		# launcher leaves that client connected and holding every jar in the shared mods directory
		# open. Its children are named from the process table and killed individually.
		for child in $(child_pids "$pid"); do
			taskkill //F //PID "$child" >/dev/null 2>&1 || kill "$child" 2>/dev/null
		done
		taskkill //F //PID "$pid" >/dev/null 2>&1 || kill "$pid" 2>/dev/null
	fi
	rm -f "$BOTS/$name.pid"
	echo "$name: stopped"
}

case "${1:-status}" in
	sync)
		# Put the built mod into the shared client mods directory and gate it, without launching.
		sync_own_mod || exit 1
		;;
	start)
		shift
		sync_own_mod || exit 1
		for name in "${@:-${DEFAULT_BOTS[@]}}"; do
			start_one "$name"
		done
		;;
	stop)
		shift
		for name in "${@:-${DEFAULT_BOTS[@]}}"; do
			stop_one "$name"
		done
		;;
	status)
		for name in "${DEFAULT_BOTS[@]}"; do
			if [ -f "$BOTS/$name.pid" ] && alive "$(cat "$BOTS/$name.pid")"; then
				echo "$name: running (pid $(cat "$BOTS/$name.pid"))"
			else
				echo "$name: stopped"
			fi
		done
		;;
	*)
		echo "usage: scripts/bots.sh start|stop|status [name...]" >&2
		exit 2
		;;
esac
