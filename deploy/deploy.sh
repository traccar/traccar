#!/bin/sh
# Copies the staged package to the server and (re)builds/starts the containers there.
# Usage: deploy/deploy.sh [user@host]     (default: luis@robot-pi.local)
set -e

HOST=${1:-luis@robot-pi.local}
REMOTE_DIR=traccar-deploy
DIR=$(cd "$(dirname "$0")" && pwd)

if [ ! -f "$DIR/out/tracker-server.jar" ]; then
    echo "Nothing staged. Run deploy/build.sh first." >&2
    exit 1
fi

echo "==> Syncing to $HOST:~/$REMOTE_DIR"
ssh "$HOST" "mkdir -p $REMOTE_DIR"
rsync -az --delete "$DIR/out" "$DIR/Dockerfile" "$DIR/compose.yaml" "$HOST:$REMOTE_DIR/"

echo "==> Building image and starting containers on $HOST"
ssh "$HOST" "set -e; cd $REMOTE_DIR
    if [ ! -f .env ]; then
        umask 077
        echo \"DB_PASSWORD=\$(openssl rand -hex 16)\" > .env
        echo 'Generated new database password in ~/$REMOTE_DIR/.env'
    fi
    sudo mkdir -p /opt/traccar/postgres /opt/traccar/logs
    if docker compose version >/dev/null 2>&1; then DC='docker compose'; else DC=docker-compose; fi
    \$DC up -d --build
    \$DC ps"
