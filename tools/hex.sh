#
# This test data assumes device "352964051908664" exists.
#
#     INSERT INTO device ( name, uniqueid )
#         VALUES ( 'Test Device for hex.sh', 352964051908664 );
#
# Note: Not all shells support "echo -e" (e.g., dash - default on Ubuntu).
#       This will cause an unknown device error for "-e".
#

echo 234c233335323936343035313930383636343b4e410d0a2342233131303931343b3130323133323b353032372e35303732383b4e3b30333032362e32303336393b453b312e3937393b3238382e3137303b4e413b4e413b4e413b4e413b4e413b3b4e413bd091d0b0d182d0b0d180d0b5d18f3a333a31303020250d0a | perl -ne 's/([0-9a-f]{2})/print chr hex $1/gie' | nc -v -w 10 localhost 5039
#echo -n -e "\x0f\x00\x00\x00\x4e\x52\x30\x39\x46\x30\x34\x31\x35\x35\x00" >/dev/udp/localhost/5053

