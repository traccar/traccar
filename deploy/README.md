# Déploiement de Traccar sur le VPS (avec gpstracker)

Chaque push sur `main` ou `master` (ou *Actions > Deploy to Production > Run workflow*) lance
`.github/workflows/deploy.yml` :

1. **Build sur GitHub** : `tracker-server.jar` + `lib/` (Gradle, Java 21), interface web (`traccar/traccar-web`
   au commit du sous-module), JRE minimal (`jlink`). Le serveur n'a besoin ni de Java, ni de Node.
2. **Envoi en SSH** du paquet et de `deploy/install.sh`, exécuté en `sudo` sur le serveur.

## Secrets GitHub à créer (Settings > Secrets and variables > Actions)

Les mêmes que pour le dépôt gpstracker :

| Secret | Contenu |
| --- | --- |
| `SERVER_IP` | IP ou nom du VPS |
| `SERVER_PORT` | port SSH (22 si vide) |
| `SERVER_USER` | utilisateur SSH (doit pouvoir faire `sudo` sans mot de passe) |
| `SSH_SERVER_KEY` | clé privée SSH de cet utilisateur |

## Ce que fait `install.sh` sur le serveur

- Installe le code dans `/opt/traccar` et le service systemd `traccar`. `conf/`, `data/` et `logs/` ne sont jamais écrasés.
- **Base de données** : la même base MySQL / MariaDB que gpstracker (nom lu dans `DATABASE_URL` de
  `/var/www/gpstracker.ma/.env.local`, `gpstracker` par défaut). Les tables Traccar sont préfixées `tc_`
  (+ `DATABASECHANGELOG*` de Liquibase) ; gpstracker les ignore (`schema_filter` Doctrine).
  Au premier déploiement : utilisateur MySQL `traccar` avec mot de passe généré, écrit dans `/opt/traccar/conf/traccar.xml` (root uniquement).
- **Interface web / API** sur `127.0.0.1:8082` uniquement (non exposée sur Internet).
  Accès admin depuis votre poste : `ssh -L 8082:127.0.0.1:8082 utilisateur@serveur`, puis http://localhost:8082.
- **Balises GPS** : ports 5000-5500 TCP/UDP (ouverts dans ufw s'il est actif ; à ouvrir aussi chez l'hébergeur si besoin).
  Chaque protocole a son port (ex. GT06 = 5023, Teltonika = 5027, OsmAnd = 5055).
- **Compte administrateur Traccar** créé au premier démarrage, identifiants dans `/opt/traccar/conf/admin.env`
  (root uniquement). Les inscriptions publiques Traccar sont fermées.
- **Liaison avec gpstracker** : `bin/console app:traccar:register-local` déclare ce serveur dans
  *Back Office > Serveurs Traccar* (URL `http://127.0.0.1:8082/api`) et le teste.

## Ordre conseillé au tout premier déploiement

1. Déployer gpstracker (crée la base et la commande `app:traccar:register-local`).
2. Déployer ce dépôt.
3. Dans le Back Office gpstracker, valider chaque client en lui affectant le serveur « Traccar (ce serveur) » :
   gpstracker crée son compte Traccar, puis la carte et l'historique de `/client` affichent ses positions.

Diagnostic sur le serveur : `systemctl status traccar`, `tail -f /opt/traccar/logs/tracker-server.log`.
