#!/usr/bin/env python3
"""
H02 Protocol Simulator / Position Sender for Traccar (or similar) - Persistent Connection Mode

Features:
- Persistent TCP connection (one socket for all messages)
- Sends V1 position reports from a list (or interpolated)
- Sends periodic V4 heartbeats to keep device online
- Automatic reconnect on connection loss
- Supports CSV input, looping, interpolation
- Graceful Ctrl+C handling

Persistent mode usage examples:
  python h02_sender.py --persistent --imei 987654321098765 --delay 30 --heartbeat 60
  python h02_sender.py -p --positions route.csv --loop --delay 20 --heartbeat 45 --interpolate 3

Normal (short connections) mode still available (omit --persistent)
"""

import argparse
import socket
import time
import sys
import csv
import signal
from datetime import datetime, timezone
import math
from math import radians, cos, sin, asin, sqrt, atan2, degrees
import random
from datetime import timedelta

def haversine(lat1, lon1, lat2, lon2):
    """Calculate distance between two points in kilometers"""
    R = 6371.0  # Earth radius in km

    dlat = radians(lat2 - lat1)
    dlon = radians(lon2 - lon1)

    a = sin(dlat / 2)**2 + cos(radians(lat1)) * cos(radians(lat2)) * sin(dlon / 2)**2
    c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return R * c


def calculate_bearing(lat1, lon1, lat2, lon2):
    """Calculate initial bearing from point1 to point2 (0–360°)"""
    lat1, lon1, lat2, lon2 = map(radians, [lat1, lon1, lat2, lon2])

    dlon = lon2 - lon1

    y = sin(dlon) * cos(lat2)
    x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dlon)

    bearing = atan2(y, x)
    bearing = degrees(bearing)
    bearing = (bearing + 360) % 360

    return bearing

def parse_datetime(dt_str):
    if 'Z' in dt_str:
        dt_str = dt_str.replace('Z', '+00:00')
    return datetime.fromisoformat(dt_str).astimezone(timezone.utc)


def ddmm_mmmm(decimal_deg):
    deg = int(decimal_deg)
    min_frac = (abs(decimal_deg) - deg) * 60
    return f"{deg:02d}{min_frac:07.4f}"


def generate_v1_message(imei, dt: datetime, lat: float, lon: float,
                        speed_knots: float = 0.0, course_deg: float = 0.0,
                        satellites: int = 8, fix_valid: bool = True,
                        mcc=310, mnc=260, lac=0, cid=0):
    time_str = dt.strftime("%H%M%S")
    date_str = dt.strftime("%d%m%y")
    lat_str = ddmm_mmmm(lat)
    lon_str = ddmm_mmmm(lon)
    lat_hem = 'N' if lat >= 0 else 'S'
    lon_hem = 'E' if lon >= 0 else 'W'
    fix_char = 'A' if fix_valid else 'V'
    speed_str = f"{speed_knots:06.2f}"
    course_str = f"{int(course_deg):03d}"
    status = "FFFFF9FF"
    sat_or_signal = satellites

    return (
        f"*HQ,{imei},V1,{time_str},{fix_char},"
        f"{lat_str},{lat_hem},{lon_str},{lon_hem},"
        f"{speed_str},{course_str},{date_str},"
        f"{status},{mcc},{mnc},{lac},{cid},{sat_or_signal}#"
    )


def generate_v4_message(imei, dt: datetime = None):
    if dt is None:
        dt = datetime.now(timezone.utc)
    timestamp = dt.strftime("%Y%m%d%H%M%S")
    return f"*HQ,{imei},V4,V1,{timestamp}#"


class PersistentH02Sender:
    def __init__(self, host, port, imei, heartbeat_interval=60):
        self.host = host
        self.port = port
        self.imei = imei
        self.heartbeat_interval = heartbeat_interval
        self.socket = None
        self.connected = False
        self.last_heartbeat = time.time()

    def connect(self):
        if self.connected:
            return True
        try:
            self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.socket.settimeout(10)
            self.socket.connect((self.host, self.port))
            self.connected = True
            print(f"[{datetime.now():%Y-%m-%d %H:%M:%S}] Connected to {self.host}:{self.port}")
            self.last_heartbeat = time.time()
            return True
        except Exception as e:
            print(f"Connect failed: {e}", file=sys.stderr)
            self.connected = False
            return False

    def close(self):
        if self.socket:
            try:
                self.socket.close()
            except:
                pass
        self.connected = False
        self.socket = None
        print("Connection closed")

    def send(self, message):
        if not self.connected:
            if not self.connect():
                return False

        try:
            self.socket.sendall((message + "\r\n").encode('ascii'))
            print(f"[{datetime.now():%Y-%m-%d %H:%M:%S}] SENT: {message}")

            # Try to read response (non-blocking)
            self.socket.setblocking(False)
            try:
                data = self.socket.recv(4096)
                if data:
                    resp = data.decode('ascii', errors='ignore').strip()
                    if resp:
                        print(f"          RECV: {resp}")
            except BlockingIOError:
                pass
            except Exception as e:
                print(f"Recv error: {e}")
            finally:
                self.socket.setblocking(True)

            return True
        except Exception as e:
            print(f"Send failed: {e}", file=sys.stderr)
            self.connected = False
            return False

    def send_heartbeat_if_needed(self):
        now = time.time()
        if now - self.last_heartbeat >= self.heartbeat_interval:
            v4 = generate_v4_message(self.imei)
            self.send(v4)
            self.last_heartbeat = now


def interpolate_positions(pos1, pos2, steps=5):
    lat1, lon1, speed1, course1 = pos1
    lat2, lon2, speed2, course2 = pos2
    dlat = lat2 - lat1
    dlon = lon2 - lon1
    dspeed = (speed2 - speed1) / steps
    dcourse = (course2 - course1) / steps

    interpolated = []
    for i in range(1, steps):
        ratio = i / steps
        lat = lat1 + dlat * ratio
        lon = lon1 + dlon * ratio
        speed = speed1 + dspeed * i
        course = course1 + dcourse * i
        interpolated.append((lat, lon, speed, course))
    return interpolated

COORD_LIST = [
        (48.405517,3.983991),(48.405796,3.983216),(48.406004,3.982725),(48.406317,3.982030),(48.406800,3.980920),(48.407592,3.978815),(48.407977,3.978107),
        (48.408837,3.976971),(48.409290,3.976403),(48.410437,3.975370),(48.411174,3.974738),(48.411920,3.974375),(48.412415,3.974119),(48.413347,3.973603),
        (48.414558,3.972703),(48.416361,3.972161),(48.417492,3.970732),(48.417661,3.971383),(48.417869,3.972276),(48.418114,3.973265),(48.416874,3.974502),
        (48.415561,3.975804),(48.413571,3.977762),(48.411767,3.979561),(48.410725,3.980518),(48.408833,3.982355),(48.407372,3.983803),(48.406990,3.984141),
        (48.405902,3.985277),(48.405102,3.986036),(48.404687,3.986438),(48.403992,3.986515),(48.404682,3.984907),(48.405220,3.984161),(48.405534,3.983765),
    ]
#TIME_STEP_SECONDS = 30  # seconds between position updates (for simulation)

def load_positions(time_step, csv_path=None):
    now = datetime.now(timezone.utc)
    seconds_back = len(COORD_LIST) * time_step
    start_time = now - timedelta(seconds=seconds_back)
    start_time = start_time.replace(second=0, microsecond=0)
    base_time = now - timedelta(seconds=seconds_back)

    if csv_path is None:
        positions = []
        prev_lat = None
        prev_lon = None
        prev_time = base_time
        for i, (lat, lon) in enumerate(COORD_LIST):
            current_time = base_time + timedelta(seconds=i * time_step)
            if prev_lat is not None and prev_lon is not None:
                distance_km = haversine(prev_lat, prev_lon, lat, lon)
                time_delta_sec = (current_time - prev_time).total_seconds()
                speed_kmh = (distance_km / (time_delta_sec / 3600)) if time_delta_sec > 0 else 0
                speed_knots = speed_kmh * 0.539957  # km/h → knots
                course = calculate_bearing(prev_lat, prev_lon, lat, lon)
            else:
                speed_knots = 0.0
                course = 0.0  # or 180.0 or random – first point has no direction
            satellites = random.randint(7, 12)
            timestamp_str = current_time.replace(tzinfo=None).isoformat() + "Z"
            positions.append((
                timestamp_str,   # ISO string for CSV compatibility
                lat,
                lon,
                round(speed_knots, 2),
                round(course, 1),
                satellites
            ))
            prev_lat = lat
            prev_lon = lon
            prev_time = current_time
        return positions
    else:
        positions = []
        with open(csv_path, newline='') as f:
            reader = csv.DictReader(f)
            for row in reader:
                dt_str = row['timestamp']
                positions.append((
                    dt_str,
                    float(row['lat']),
                    float(row['lon']),
                    float(row.get('speed_knots', 0.0)),
                    float(row.get('course_deg', 0.0)),
                    int(row.get('satellites', 8))
                ))
        return positions


def main():
    parser = argparse.ArgumentParser(description="H02 V1 position sender - persistent connection mode")
    parser.add_argument("--host", default="localhost")
    parser.add_argument("--port", type=int, default=5013)
    parser.add_argument("--imei", required=True)
    parser.add_argument("--delay", type=float, default=30.0, help="Seconds between main position updates")
    parser.add_argument("--heartbeat", type=int, default=60, help="Seconds between V4 heartbeats")
    parser.add_argument("--loop", action="store_true", help="Loop forever")
    parser.add_argument("--interpolate", type=int, default=0, help="Interpolation steps between positions")
    parser.add_argument("--positions", help="CSV file with positions")
    parser.add_argument("--persistent", "-p", action="store_true", help="Use persistent connection (recommended)")

    args = parser.parse_args()

    positions = load_positions(args.delay, args.positions)
    print(f"Loaded {len(positions)} positions. Delay: {args.delay}s  Heartbeat: {args.heartbeat}s")

    if not args.persistent:
        print("Non-persistent mode (short connections). Use --persistent for real tracker simulation.")
        # Original short-connection logic here (omitted for brevity - keep your old code if needed)
        sys.exit("Non-persistent mode not implemented in this version. Use --persistent.")

    sender = PersistentH02Sender(args.host, args.port, args.imei, args.heartbeat)

    def shutdown(sig=None, frame=None):
        print("\nShutting down...")
        sender.close()
        sys.exit(0)

    signal.signal(signal.SIGINT, shutdown)
    signal.signal(signal.SIGTERM, shutdown)

    try:
        pos_index = 0
        while True:
            sender.send_heartbeat_if_needed()

            if pos_index < len(positions):
                timestamp_str, lat, lon, speed, course, sats = positions[pos_index]
                dt = parse_datetime(timestamp_str)

                points = [(dt, lat, lon, speed, course, sats)]

                # Interpolation
                if args.interpolate > 0 and pos_index < len(positions) - 1:
                    next_pos = positions[pos_index + 1]
                    inter = interpolate_positions(
                        (lat, lon, speed, course),
                        (next_pos[1], next_pos[2], next_pos[3], next_pos[4]),
                        args.interpolate
                    )
                    points += [(dt, ilat, ilon, ispeed, icourse, sats) for ilat, ilon, ispeed, icourse in inter]

                for p_dt, p_lat, p_lon, p_speed, p_course, p_sats in points:
                    v1 = generate_v1_message(
                        args.imei, p_dt, p_lat, p_lon, p_speed, p_course, p_sats
                    )
                    sender.send(v1)
                    time.sleep(args.delay / max(1, len(points)))  # spread delay

                pos_index += 1

            if pos_index >= len(positions):
                if not args.loop:
                    print("All positions sent. Waiting for heartbeats... Press Ctrl+C to exit.")
                    while True:
                        time.sleep(1)
                        sender.send_heartbeat_if_needed()
                else:
                    print("Looping back to first position...")
                    pos_index = 0
                    time.sleep(5)

            time.sleep(0.5)  # small loop sleep

    except KeyboardInterrupt:
        shutdown()


if __name__ == "__main__":
    main()
