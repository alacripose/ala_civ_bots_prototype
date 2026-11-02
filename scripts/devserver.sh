#!/usr/bin/env bash
# Persistent development server (DEV-01). Owns run/world for the interactive session and exposes a
# local RCON control channel so scripts/attach.sh can run acceptance phases inside it.
#
# The server stays up across phases; stop it only with Ctrl-C / an explicit kill.
# Usage: scripts/devserver.sh
set -uo pipefail

cd "$(dirname "$0")/.." || exit 1
mkdir -p run logs

if [ -z "${JAVA_HOME:-}" ]; then
	echo "JAVA_HOME must point at a JDK 17+ (this project was built on JDK 21)" >&2
	exit 1
fi

if [ ! -f run/eula.txt ]; then
	printf 'eula=true\n' > run/eula.txt
fi

ensure_property() {
	local key="$1" value="$2"
	if grep -q "^${key}=" run/server.properties; then
		sed -i "s|^${key}=.*|${key}=${value}|" run/server.properties
	else
		printf '%s=%s\n' "$key" "$value" >> run/server.properties
	fi
}

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
level-seed=ensouled-dev
EOF
fi

if netstat -an 2>/dev/null | grep -q "0.0.0.0:25565 .*LISTENING"; then
	echo "A server is already listening on 25565; attach to it with scripts/attach.sh instead." >&2
	exit 1
fi

# Local control channel: a random password per session, stored under run/ (gitignored) and only
# used by scripts/attach.sh. RCON is bound to the loopback interface in server.properties'
# rcon settings; it is not a public control surface.
if [ ! -f run/rcon.password ]; then
	python -c "import secrets; print(secrets.token_hex(16))" > run/rcon.password
fi
ensure_property enable-rcon true
ensure_property rcon.port 25575
ensure_property rcon.password "$(cat run/rcon.password)"
ensure_property broadcast-rcon-to-ops true

# The world folder carries its own copy of this mod for the server runtime. If it is older than the
# build, the server runs a mod that no longer matches its sources — the being then acts through a
# stale control bridge while everything on disk says otherwise.
BUILT_JAR="$(ls -t build/libs/ensouled-*.jar 2>/dev/null | grep -v sources | head -1)"
if [ -n "$BUILT_JAR" ]; then
	mkdir -p run/mods
	# One ensouled jar only, and the glob is `ensouled*` rather than `ensouled-*`: a copy named
	# plain `ensouled.jar` from an earlier hand-install does not match a versioned pattern, so it
	# survived this clean-up and Fabric loaded it instead of the build. The server then reported an
	# old build string and ran old code while every file on disk said otherwise.
	find run/mods -maxdepth 1 -name 'ensouled*.jar' ! -name "$(basename "$BUILT_JAR")" -delete 2>/dev/null
	STALE="$(find run/mods -maxdepth 1 -name 'ensouled*.jar' ! -name "$(basename "$BUILT_JAR")" 2>/dev/null)"
	if [ -n "$STALE" ]; then
		echo "an old ensouled jar is still in run/mods and could not be removed:" >&2
		echo "  $STALE" >&2
		echo "stop the server holding it, delete it, and run this again." >&2
		exit 1
	fi
	if ! cmp -s "$BUILT_JAR" "run/mods/$(basename "$BUILT_JAR")"; then
		cp -f "$BUILT_JAR" "run/mods/$(basename "$BUILT_JAR")"
		echo "server mod synced: $(basename "$BUILT_JAR")"
	fi
fi

# Two refusals before the server takes the port. The installed copies must carry this worktree,
# and the store must be one a mount can reconcile rather than one holding invented history. A
# missing checker is itself a refusal: parity that was not run is the failure, not the default.
if [ ! -f tools/parity_check.py ]; then
	echo "refusing to launch: tools/parity_check.py is missing, so parity cannot be checked." >&2
	exit 1
fi
if ! python tools/parity_check.py --existing build server client; then
	echo "refusing to launch: an installed ensouled jar does not carry this worktree." >&2
	exit 1
fi
if [ ! -f tools/mongo_parity.py ]; then
	echo "refusing to launch: tools/mongo_parity.py is missing, so store drift cannot be checked." >&2
	exit 1
fi
if ! python tools/mongo_parity.py --launch; then
	echo "refusing to launch: the Mongo projection contradicts the durable stores." >&2
	exit 1
fi

echo "== persistent dev server on 25565 (world: run/world, control: 127.0.0.1:25575) =="
./gradlew runServer --console=plain >> logs/devserver.log 2>&1 &
GRADLE_PID=$!
echo "$GRADLE_PID" > run/devserver.pid
for _ in $(seq 1 120); do
	if netstat -an 2>/dev/null | grep -q "0.0.0.0:25565 .*LISTENING"; then
		echo "== dev server is up (pid $GRADLE_PID, log logs/devserver.log) =="
		break
	fi
	if ! kill -0 "$GRADLE_PID" 2>/dev/null; then
		echo "dev server exited during startup; see logs/devserver.log" >&2
		exit 1
	fi
	sleep 2
done
wait "$GRADLE_PID"
