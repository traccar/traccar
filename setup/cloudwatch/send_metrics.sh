#!/bin/bash

# Namespace for CloudWatch metrics
NAMESPACE="CWAgent"

# Extract and count established TCP connections by port, filter out single connections
ss -tan | grep ESTAB | awk '{print $4}' | awk -F':' '{print $NF}' | sort | uniq -c | awk '$1 > 1 {printf "TCP:%s:%s\n", $2, $1}' > /tmp/tcp_connections.txt

# Extract and count UDP connections by port, filter out single connections
ss -uan | awk '{print $5}' | awk -F':' '{print $NF}' | sort | uniq -c | awk '$1 > 1 {printf "UDP:%s:%s\n", $2, $1}' > /tmp/udp_connections.txt

# Combine TCP and UDP results
cat /tmp/tcp_connections.txt /tmp/udp_connections.txt > /tmp/connections.txt

# Send metrics to CloudWatch
while IFS=: read -r PROTOCOL PORT COUNT; do
  aws cloudwatch put-metric-data \
    --metric-name Connections \
    --namespace "$NAMESPACE" \
    --dimensions Protocol=$PROTOCOL,Port=$PORT \
    --value "$COUNT" \
    --unit Count
done < /tmp/connections.txt

# Clean up temporary files
rm -f /tmp/tcp_connections.txt /tmp/udp_connections.txt /tmp/connections.txt

echo "Metrics sent to CloudWatch."
