echo 78780d0103534190360660610001e0cd0d0a | perl -ne 's/([0-9a-f]{2})/print chr hex $1/gie' | nc -v -w 10 localhost 5023
