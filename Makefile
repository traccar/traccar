# ────────────────────────────────────────────────
# Traccar Docker Compose helpers
# ────────────────────────────────────────────────

COMPOSE_FILE := docker/compose/traccar-timescaledb.yaml

up:
	docker compose -f $(COMPOSE_FILE) up -d

down:
	docker compose -f $(COMPOSE_FILE) down

restart:
	docker compose -f $(COMPOSE_FILE) restart

log:
	docker compose -f $(COMPOSE_FILE) exec traccar tail -f /opt/traccar/logs/tracker-server.log

logs:
	docker compose -f $(COMPOSE_FILE) logs -f


# Optional: full clean (stops + removes volumes → loses DB data!)
clean:
	docker compose -f $(COMPOSE_FILE) down -v

bash:
	docker compose -f $(COMPOSE_FILE) exec traccar sh


# Optional: restart only Traccar container
restart-app:
	docker compose -f $(COMPOSE_FILE) restart traccar
	
test-ho2-protocole:
	( \
	  echo "##,imei:358655600007040,A;"; \
	  sleep 1; \
	  echo "*HQ,358655600007040,V1,201844,A,2608.9437,N,08016.2521,W,000.80,000,150317,FFFFF9FF,310,260,0,0,6#"; \
	) | nc localhost 5013 || true


# Send positions to Traccar using persistent H02 connection
# - persistent connection
# - IMEI: 358655600007040
# - 5 seconds between positions
# - 10 seconds between heartbeats
send-h02-trail:
	python3 simulate/h02_sender.py \
	  --persistent \
	  --imei 358655600007040 \
	  --delay 5 \
	  --heartbeat 10


# ────────────────────────────────────────────────────────────────
# ZhongXun Topin (topin protocol) test - binary format
# Port: usually 5199 (check traccar.xml for topin.port)
#       or try 5023 if your device is GT06-compatible variant
#
# This example:
#   - Sends a login packet (0x01)
#   - Sends one GPS position packet (0x10) with Paris coordinates
#     approx lat 48.856614 N, lon 2.352221 E
#   - Uses xxd to convert hex to binary
# ────────────────────────────────────────────────────────────────
test-topin-protocole:
	python3 simulate/topin_sender.py --imei 358655600695588 --lat 48.405517 --lon 3.983991

db:
	docker compose -f $(COMPOSE_FILE) exec database psql -U traccar -d traccar
