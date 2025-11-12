# Traccar & Smart Box Listener Deployment Guide

## 🚀 Deployment Steps

### Step 1: Build Traccar

```bash
cd ~/Smart/traccar_LW
./gradlew jar -x test
```

### Step 2: Upload to S3

```bash
# Upload the JAR to S3 root
aws s3 cp target/tracker-server.jar s3://traccar-jar/tracker-server.jar

# Generate a presigned URL (valid for 1 hour)
aws s3 presign s3://traccar-jar/tracker-server.jar --expires-in 3600
```

Copy the presigned URL from the output.

### Step 3: Deploy to Traccar Server

**SSH into Traccar server** and run these commands with the presigned URL from Step 2:

```bash
# Stop Traccar
sudo systemctl stop traccar

# Backup current JAR with timestamp
sudo cp /opt/traccar/tracker-server.jar /opt/traccar/tracker-server.jar.backup.$(date +%Y%m%d_%H%M%S)

# Download new JAR from S3 (replace PRESIGNED_URL_HERE with the actual URL)
sudo wget "PRESIGNED_URL_HERE" -O /opt/traccar/tracker-server.jar

# Start Traccar
sudo systemctl start traccar

# Check status
sudo systemctl status traccar

# Watch logs for network info parsing
sudo journalctl -u traccar -f
```

**Look for these patterns in logs:**
- `[MEITRACK CCE]` - Binary CCE messages being processed
- Position updates for your devices (861076085541819, 865413052597209)
- No `IndexOutOfBoundsException` or `NullPointerException` errors

Press `Ctrl+C` to stop watching logs.

### Step 4: Rebuild and Deploy Listener

```bash
cd ~/Smart/smart_cloud/containers/smart-box-listener

# Rebuild Docker container
docker-compose build

# Restart container
docker-compose down
docker-compose up -d

# Check logs
docker-compose logs -f
```

**Look for these patterns in logs:**
- `[NETWORK DEBUG]` - Network descriptors being parsed
- `[TRACCAR DEBUG]` - Position processing
- Network info being extracted successfully

Press `Ctrl+C` to stop watching logs.

## ✅ Testing After Deployment

### 1. Check Traccar Logs

Monitor for specific devices:

```bash
# Watch logs for P99 and P88 devices
sudo journalctl -u traccar -f | grep -E "(861076085541819|865413052597209)"

# Or check recent messages
sudo journalctl -u traccar -n 50 | grep -E "(861076085541819|865413052597209)"
```

### 2. Check Database

Verify network info is being stored:

```sql
SELECT 
    device_serial,
    signalDateTime,
    json_fields->>'networkType' as network_type,
    json_fields->>'volte' as volte,
    json_fields->>'rssi' as rssi,
    json_fields->>'rsrp' as rsrp,
    json_fields->>'sinr' as sinr,
    json_fields->>'rsrq' as rsrq
FROM device_data
WHERE device_serial IN ('861076085541819', '865413052597209')
ORDER BY signalDateTime DESC
LIMIT 10;
```

**Expected results:**
- **P99** (861076085541819): Shows `networkType`, `volte`, and signal values (when available)
- **P88** (865413052597209): Shows `networkType`, `volte`, and signal values (when available)
- Both show "NOSERVICE" when no cellular signal

### 3. Check Listener Logs

```bash
# Watch listener processing
docker-compose logs -f smart-box-listener | grep "NETWORK"

# Or check recent logs
docker-compose logs --tail=100 smart-box-listener | grep "NETWORK"
```

## 🔄 Quick Redeploy (After Code Changes)

### Traccar Only:

```bash
cd ~/Smart/traccar_LW
./gradlew jar -x test && \
aws s3 cp target/tracker-server.jar s3://traccar-jar/tracker-server.jar && \
aws s3 presign s3://traccar-jar/tracker-server.jar --expires-in 3600
```

Then SSH to server and run:
```bash
sudo systemctl stop traccar && \
sudo cp /opt/traccar/tracker-server.jar /opt/traccar/tracker-server.jar.backup.$(date +%Y%m%d_%H%M%S) && \
sudo wget "PRESIGNED_URL" -O /opt/traccar/tracker-server.jar && \
sudo systemctl start traccar
```

### Listener Only:

```bash
cd ~/Smart/smart_cloud/containers/smart-box-listener
docker-compose build && docker-compose down && docker-compose up -d
```

## 🐛 Troubleshooting

### Traccar Not Starting

```bash
# Check for errors
sudo journalctl -u traccar -n 100

# Verify JAR file
ls -lh /opt/traccar/tracker-server.jar

# Check Java version
java -version

# Restore backup if needed (use the latest timestamped backup)
# First, list available backups
ls -lt /opt/traccar/tracker-server.jar.backup.* | head -5

# Then restore the one you want (replace YYYYMMDD_HHMMSS with actual timestamp)
sudo cp /opt/traccar/tracker-server.jar.backup.YYYYMMDD_HHMMSS /opt/traccar/tracker-server.jar
sudo systemctl start traccar
```

### Network Info Not Appearing

```bash
# Check if devices are sending data
sudo journalctl -u traccar | grep "861076085541819" | tail -20

# Check for parsing errors
sudo journalctl -u traccar | grep -i "error\|exception" | tail -20

# Verify listener is running
docker-compose ps
docker-compose logs smart-box-listener | tail -50
```

### Database Not Updating

```bash
# Check listener database connection
docker-compose logs smart-box-listener | grep -i "error\|connection"

# Verify listener is receiving data from Traccar
docker-compose logs smart-box-listener | grep "Processing Traccar data"

# Check database permissions
psql -U postgres -d smart -c "\du"
```

## 📝 Rollback Procedure

If something goes wrong:

### Rollback Traccar:

```bash
# List available backups (sorted by date, newest first)
ls -lt /opt/traccar/tracker-server.jar.backup.* | head -5

# Stop Traccar
sudo systemctl stop traccar

# Restore the backup you want (replace YYYYMMDD_HHMMSS with actual timestamp)
sudo cp /opt/traccar/tracker-server.jar.backup.YYYYMMDD_HHMMSS /opt/traccar/tracker-server.jar

# Start Traccar
sudo systemctl start traccar
sudo systemctl status traccar
```

### Rollback Listener:

```bash
cd ~/Smart/smart_cloud/containers/smart-box-listener
git checkout HEAD -- src/listeners/traccar.js
docker-compose build && docker-compose restart
```

## 🎯 Success Indicators

After successful deployment, you should see:

- ✅ Traccar service running without errors
- ✅ Position updates in logs for both P99 and P88
- ✅ Network info appearing in database `json_fields`
- ✅ Listener processing messages without errors
- ✅ No `IndexOutOfBoundsException` errors
- ✅ Both devices showing `networkType` field

## 📊 Monitoring Commands

### Real-time monitoring:

```bash
# Watch Traccar processing both devices
sudo journalctl -u traccar -f | grep -E "(861076085541819|865413052597209|NETWORK)"

# Watch listener processing
docker-compose logs -f smart-box-listener

# Monitor database inserts
watch -n 5 "psql -U postgres -d smart -c \"SELECT device_serial, signalDateTime, json_fields->>'networkType' FROM device_data WHERE device_serial IN ('861076085541819', '865413052597209') ORDER BY signalDateTime DESC LIMIT 5;\""
```



