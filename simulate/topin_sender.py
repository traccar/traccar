#!/usr/bin/env python3
# topin_sender.py - ZhongXun Topin binary test (corrected lat/lon + valid flag)

import socket
import time
import binascii
import argparse
from datetime import datetime

def crc16(data):
    crc = 0xFFFF
    for byte in data:
        crc ^= byte
        for _ in range(8):
            if crc & 1:
                crc = (crc >> 1) ^ 0xA001
            else:
                crc >>= 1
    return crc.to_bytes(2, 'little')

def send_packet(sock, payload, desc=""):
    length = len(payload)
    packet = b'\x78\x78' + bytes([length]) + payload + crc16(payload) + b'\x0D\x0A'
    sock.sendall(packet)
    print(f"[SENT {desc}] ({len(packet)} bytes): {binascii.hexlify(packet).decode()}")

def main():
    parser = argparse.ArgumentParser(description="Topin binary packet sender")
    parser.add_argument("--host", default="localhost")
    parser.add_argument("--port", type=int, default=5199)
    parser.add_argument("--imei", type=int, default=358655600695588)
    parser.add_argument("--lat", type=float, default=48.405517, help="Latitude (north positive)")
    parser.add_argument("--lon", type=float, default=3.983991, help="Longitude (east positive)")
    args = parser.parse_args()

    print(f"Using IMEI: {args.imei}, Lat: {args.lat}, Lon: {args.lon}")

    # Validate coords
    if not (-90 <= args.lat <= 90) or not (-180 <= args.lon <= 180):
        print("Invalid coordinates")
        return

    # IMEI BCD (pad to 16 nibbles)
    imei_str = f"0{args.imei:014d}"
    imei_bcd = bytes(int(imei_str[i:i+2], 16) for i in range(0, 16, 2))

    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.connect((args.host, args.port))

        # Login
        login_payload = b'\x01' + imei_bcd + b'\x00\x01'
        send_packet(s, login_payload, "LOGIN")
        time.sleep(2)

        # Position
        now = datetime.utcnow()
        dt_bcd = bytes([now.year % 100, now.month, now.day, now.hour, now.minute, now.second])

        # Correct scaling: 1800000 units per degree
        lat_abs  = abs(args.lat)
        lon_abs  = abs(args.lon)
        lat_int  = int(lat_abs * 60 * 30000)   # 30000 is common for Topin
        lon_int  = int(lon_abs * 60 * 30000)

        # BIG-ENDIAN (this is what most GT06/Topin decoders expect)
        lat_bytes  = lat_int.to_bytes(4,  'big')
        lon_bytes  = lon_int.to_bytes(4,  'big')
        
        speed_bytes = bytes([0])   # most Topin: 1 byte speed
        
        # Course + flags (Topin style – status in high byte)
        course = 0
        # Attempt A: bit 7=1 for North
        if args.lat >= 0:   course |= (1 << 7)
        if args.lon >= 0:   course |= (1 << 8)   # East = 1 ? or try =0 for East
        
        # Keep GPS valid
        course     |= (1 << 9)                   # GPS valid / real-time (bit 9)
        course_deg  = 45                        # example course
        course     |= (course_deg << 10)        # bits 15–10 = course degrees


        course_bytes = course.to_bytes(2, 'big')


        sats = 10

        # Most common 0x10 payload for Topin
        pos_payload = (
            b'\x10' +
            dt_bcd +
            bytes([sats]) +
            lat_bytes +
            lon_bytes +
            speed_bytes +          # now 1 byte
            course_bytes
        )

        send_packet(s, pos_payload, "POSITION")

        print("Connection open. Press Ctrl+C to exit.")
        try:
            while True:
                time.sleep(30)
        except KeyboardInterrupt:
            print("Exiting...")

if __name__ == "__main__":
    main()
