#!/bin/bash

#
# Script to send convert HEX string into binary and send it to specified local port
#
# Example usage:
#
#   hex.sh 5039 3e52505631393535352b343533383431382d303733393531383530303032303231323b49443d393939393b2a37423c0d0a
#

if [ $# -lt 2 ]
then
  echo "USAGE: $0 <host> <port> <hex>"
  echo "If only <port> and <hex> are present, <host> defaults to localhost."
  exit 1
fi

host="$1"; port="$2"; hex="$3";

if [ $# -eq 2 ]
then
    host="localhost"; port="$1"; hex="$2";
fi

send_hex_udp () {
  echo "$hex" | xxd -r -p | nc -u -w 0 "$host" "$port"
}

send_hex_tcp () {
  echo "$hex" | xxd -r -p | nc "$host" "$port"
}

send_text_udp () {
  echo -n -e "$hex" | nc -u -w 0 "$host" "$port"
}

send_text_tcp () {
  echo -n -e "$hex" | nc "$host" "$port"
}

send_hex_tcp "$host" "$port" "$hex"

exit $?
