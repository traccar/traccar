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
  echo "USAGE: $0 <port> <hex>"
  exit 1
fi

send_hex_udp () {
  echo $2 | xxd -r -p | nc -u localhost $1
}

send_hex_tcp () {
  echo $2 | xxd -r -p | nc localhost $1
}

send_text_udp () {
  echo -n -e $2 | nc -u localhost $1
}

send_text_tcp () {
  echo -n -e $2 | nc localhost $1
}

send_hex_tcp $1 $2

exit $?
