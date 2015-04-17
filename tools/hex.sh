#
# This test data assumes device "352964051908664" exists.
#
#     INSERT INTO device ( name, uniqueid )
#         VALUES ( 'Test Device for hex.sh', 352964051908664 );
#
# Note: Not all shells support "echo -e" (e.g., dash - default on Ubuntu).
#       This will cause an unknown device error for "-e".
#

echo 3e52505631393535352b343533383431382d303733393531383530303032303231323b49443d393939393b2a37423c0d0a | perl -ne 's/([0-9a-f]{2})/print chr hex $1/gie' | nc -v -w 10 localhost 5039
#echo 3e52505631393535352b343533383431382d303733393531383530303032303231323b49443d393939393b2a37423c0d0a | perl -ne 's/([0-9a-f]{2})/print chr hex $1/gie' > /dev/udp/localhost/5057
