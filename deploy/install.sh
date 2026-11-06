#!/bin/bash
# Installe ou met à jour Traccar sur le serveur (exécuté en root par le workflow .github/workflows/deploy.yml).
#
#   sudo bash install.sh /chemin/traccar-package.tar.gz
#
# - Code installé dans /opt/traccar (jar, lib, jre, schema, templates, web). conf/, data/ et logs/ ne sont jamais écrasés.
# - Base de données : la MÊME base MySQL / MariaDB que le site gpstracker (tables Traccar préfixées « tc_ »).
#   Au premier déploiement, un utilisateur MySQL « traccar » est créé avec un mot de passe généré et conf/traccar.xml est écrit.
# - Premier démarrage : création du compte administrateur Traccar (identifiants dans conf/admin.env, lisible par root seul),
#   fermeture des inscriptions publiques, puis déclaration du serveur dans le Back Office gpstracker
#   (commande « app:traccar:register-local ») pour que l'espace client affiche les positions.
# Idempotent : peut être relancé à chaque déploiement, ne supprime aucune donnée.
set -euo pipefail

PACKAGE="${1:?usage: install.sh traccar-package.tar.gz}"
TRACCAR_DIR="/opt/traccar"
GPSTRACKER_DIR="${GPSTRACKER_DIR:-/var/www/gpstracker.ma}"
PHP_BIN="${PHP_BIN:-php8.3}"
API_URL="http://127.0.0.1:8082/api"

echo "▶ Décompression du paquet..."
STAGE="$(mktemp -d)"
trap 'rm -rf "$STAGE"' EXIT
tar -xzf "$PACKAGE" -C "$STAGE"
for item in tracker-server.jar lib jre schema templates web traccar.service; do
  [ -e "$STAGE/$item" ] || { echo "✗ paquet incomplet : $item manquant"; exit 1; }
done

echo "▶ Outils système (curl, rsync, jq)..."
MISSING=""
for tool in curl rsync jq; do command -v "$tool" > /dev/null || MISSING="$MISSING $tool"; done
if [ -n "$MISSING" ]; then
  apt-get update -qq || echo "  (apt-get update a signalé une erreur, on continue)"
  apt-get install -y -qq $MISSING
fi

# ---------------------------------------------------------------------------------------------------------------------
# Base de données partagée avec gpstracker
# ---------------------------------------------------------------------------------------------------------------------
DB_NAME="gpstracker"
if [ -f "$GPSTRACKER_DIR/.env.local" ]; then
  url="$(grep -E '^DATABASE_URL=' "$GPSTRACKER_DIR/.env.local" | tail -n 1 | cut -d= -f2- | tr -d '"'"'" || true)"
  name="$(printf '%s' "$url" | sed -nE 's#^[a-z0-9]+://[^/]*/([^?]+).*#\1#p')"
  [ -n "$name" ] && DB_NAME="$name"
fi
echo "▶ Base de données partagée : « $DB_NAME »"

mkdir -p "$TRACCAR_DIR/conf" "$TRACCAR_DIR/data" "$TRACCAR_DIR/logs"
CONF="$TRACCAR_DIR/conf/traccar.xml"
if ! grep -qs "jdbc:mysql" "$CONF"; then
  echo "  Premier déploiement : création de l'utilisateur MySQL « traccar » et de conf/traccar.xml"
  DB_PASS="$(openssl rand -hex 24)"
  mysql -e "CREATE DATABASE IF NOT EXISTS \`${DB_NAME}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    CREATE USER IF NOT EXISTS 'traccar'@'localhost' IDENTIFIED BY '${DB_PASS}';
    CREATE USER IF NOT EXISTS 'traccar'@'127.0.0.1' IDENTIFIED BY '${DB_PASS}';
    ALTER USER 'traccar'@'localhost' IDENTIFIED BY '${DB_PASS}';
    ALTER USER 'traccar'@'127.0.0.1' IDENTIFIED BY '${DB_PASS}';
    GRANT ALL PRIVILEGES ON \`${DB_NAME}\`.* TO 'traccar'@'localhost';
    GRANT ALL PRIVILEGES ON \`${DB_NAME}\`.* TO 'traccar'@'127.0.0.1';
    FLUSH PRIVILEGES;"
  cat > "$CONF" <<XML
<?xml version='1.0' encoding='UTF-8'?>
<!DOCTYPE properties SYSTEM 'http://java.sun.com/dtd/properties.dtd'>
<properties>

    <!-- Généré par deploy/install.sh au premier déploiement. Ce fichier n'est plus jamais écrasé ensuite. -->
    <!-- Documentation : https://www.traccar.org/configuration-file/ -->

    <!-- Même base que le site gpstracker : les tables Traccar sont préfixées « tc_ ». -->
    <entry key='database.driver'>com.mysql.cj.jdbc.Driver</entry>
    <entry key='database.url'>jdbc:mysql://127.0.0.1:3306/${DB_NAME}?zeroDateTimeBehavior=round&amp;serverTimezone=UTC&amp;allowPublicKeyRetrieval=true&amp;useSSL=false&amp;allowMultiQueries=true&amp;autoReconnect=true&amp;useUnicode=yes&amp;characterEncoding=UTF-8&amp;sessionVariables=sql_mode=''</entry>
    <entry key='database.user'>traccar</entry>
    <entry key='database.password'>${DB_PASS}</entry>

    <!-- Interface web et API : uniquement en local (gpstracker l'appelle sur 127.0.0.1:8082).
         Accès admin depuis votre poste : ssh -L 8082:127.0.0.1:8082 utilisateur@serveur puis http://localhost:8082 -->
    <entry key='web.address'>127.0.0.1</entry>
    <entry key='web.port'>8082</entry>

</properties>
XML
else
  echo "  conf/traccar.xml déjà configuré pour MySQL : inchangé"
fi
chown root:root "$CONF"
chmod 600 "$CONF"

# ---------------------------------------------------------------------------------------------------------------------
# Code
# ---------------------------------------------------------------------------------------------------------------------
echo "▶ Installation du code dans $TRACCAR_DIR..."
WAS_RUNNING=0
systemctl is-active --quiet traccar && WAS_RUNNING=1
[ "$WAS_RUNNING" = 1 ] && systemctl stop traccar
install -m 644 "$STAGE/tracker-server.jar" "$TRACCAR_DIR/tracker-server.jar"
for dir in lib jre schema templates web; do
  rsync -a --delete "$STAGE/$dir/" "$TRACCAR_DIR/$dir/"
done
chmod -R go+rX "$TRACCAR_DIR/tracker-server.jar" "$TRACCAR_DIR/lib" "$TRACCAR_DIR/jre" "$TRACCAR_DIR/schema" "$TRACCAR_DIR/templates" "$TRACCAR_DIR/web"

if ! cmp -s "$STAGE/traccar.service" /etc/systemd/system/traccar.service; then
  install -m 644 "$STAGE/traccar.service" /etc/systemd/system/traccar.service
  systemctl daemon-reload
fi
systemctl enable --quiet traccar

echo "▶ Pare-feu : ports des balises GPS (5000-5500 TCP/UDP)..."
if command -v ufw > /dev/null && ufw status | grep -q "Status: active"; then
  ufw allow 5000:5500/tcp > /dev/null
  ufw allow 5000:5500/udp > /dev/null
  echo "  règles ufw ajoutées"
else
  echo "  ufw inactif : vérifiez que le pare-feu de l'hébergeur laisse passer 5000-5500 TCP/UDP"
fi

echo "▶ Démarrage de Traccar..."
systemctl restart traccar
# Le premier démarrage crée toutes les tables (Liquibase) : cela peut prendre plusieurs minutes.
for i in $(seq 1 90); do
  curl -fsS -m 5 "$API_URL/health" > /dev/null 2>&1 && break
  if ! systemctl is-active --quiet traccar && [ "$i" -gt 3 ]; then
    echo "✗ Traccar s'est arrêté :"; journalctl -u traccar -n 40 --no-pager || true; tail -n 40 "$TRACCAR_DIR"/logs/*.log 2>/dev/null || true
    exit 1
  fi
  sleep 5
done
if ! curl -fsS -m 5 "$API_URL/health" > /dev/null; then
  echo "✗ Traccar ne répond pas sur $API_URL/health après 7 min 30 :"
  tail -n 60 "$TRACCAR_DIR"/logs/*.log 2>/dev/null || journalctl -u traccar -n 60 --no-pager || true
  exit 1
fi
echo "  Traccar $(curl -fsS -m 5 "$API_URL/server" | jq -r '.version // "?"') en ligne"

# ---------------------------------------------------------------------------------------------------------------------
# Compte administrateur Traccar + déclaration dans gpstracker
# ---------------------------------------------------------------------------------------------------------------------
ADMIN_FILE="$TRACCAR_DIR/conf/admin.env"
if [ ! -f "$ADMIN_FILE" ]; then
  echo "▶ Création du compte administrateur Traccar..."
  ADMIN_EMAIL="admin@gpstracker.local"
  ADMIN_PASS="$(openssl rand -hex 20)"
  body="$(jq -nc --arg e "$ADMIN_EMAIL" --arg p "$ADMIN_PASS" '{name: "Administrateur gpstracker", email: $e, password: $p}')"
  # Le premier compte créé sur un Traccar vide devient automatiquement administrateur.
  if curl -fsS -m 15 -H 'Content-Type: application/json' -d "$body" "$API_URL/users" > /dev/null; then
    umask 077
    printf 'TRACCAR_ADMIN_EMAIL=%s\nTRACCAR_ADMIN_PASSWORD=%s\n' "$ADMIN_EMAIL" "$ADMIN_PASS" > "$ADMIN_FILE"
    chmod 600 "$ADMIN_FILE"
    echo "  compte créé, identifiants dans $ADMIN_FILE (root uniquement)"
  else
    echo "⚠ Impossible de créer le compte : Traccar a déjà des utilisateurs."
    echo "  Écrivez TRACCAR_ADMIN_EMAIL=... et TRACCAR_ADMIN_PASSWORD=... (compte administrateur) dans $ADMIN_FILE puis relancez le déploiement."
  fi
fi

if [ -f "$ADMIN_FILE" ]; then
  # shellcheck disable=SC1090
  . "$ADMIN_FILE"
  AUTH="${TRACCAR_ADMIN_EMAIL}:${TRACCAR_ADMIN_PASSWORD}"

  # Inscriptions publiques fermées : les comptes Traccar sont créés uniquement par gpstracker.
  server_json="$(curl -fsS -m 10 -u "$AUTH" "$API_URL/server" || true)"
  if [ -n "$server_json" ] && [ "$(printf '%s' "$server_json" | jq -r '.registration')" = "true" ]; then
    printf '%s' "$server_json" | jq -c '.registration = false' \
      | curl -fsS -m 10 -u "$AUTH" -X PUT -H 'Content-Type: application/json' -d @- "$API_URL/server" > /dev/null \
      && echo "▶ Inscriptions publiques Traccar fermées" || echo "⚠ Impossible de fermer les inscriptions Traccar"
  fi

  echo "▶ Déclaration du serveur dans gpstracker..."
  if [ -f "$GPSTRACKER_DIR/bin/console" ] && (cd "$GPSTRACKER_DIR" && $PHP_BIN bin/console list app --env=prod --no-debug 2>/dev/null | grep -q "app:traccar:register-local"); then
    # Le mot de passe passe par l'environnement (jamais en argument, visible dans « ps »).
    if (cd "$GPSTRACKER_DIR" && sudo -u www-data env TRACCAR_ADMIN_PASSWORD="$TRACCAR_ADMIN_PASSWORD" \
        $PHP_BIN bin/console app:traccar:register-local "$API_URL" "$TRACCAR_ADMIN_EMAIL" --name="Traccar (ce serveur)" --env=prod --no-debug); then
      :
    else
      echo "⚠ La déclaration dans gpstracker a échoué (voir ci-dessus). Traccar fonctionne ; déclarez-le dans Back Office > Serveurs Traccar."
    fi
  else
    echo "⚠ gpstracker introuvable dans $GPSTRACKER_DIR ou trop ancien (commande app:traccar:register-local absente)."
    echo "  Déployez gpstracker puis relancez ce déploiement, ou déclarez le serveur dans Back Office > Serveurs Traccar (URL $API_URL)."
  fi
fi

echo "✅ Traccar déployé"
