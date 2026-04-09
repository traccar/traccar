-- Traccar database schema (SQLite compatible)

CREATE TABLE IF NOT EXISTS tc_servers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    registration INTEGER DEFAULT 1,
    readonly INTEGER DEFAULT 0,
    deviceReadonly INTEGER DEFAULT 0,
    limitCommands INTEGER DEFAULT 0,
    disableReports INTEGER DEFAULT 0,
    fixedEmail INTEGER DEFAULT 0,
    map TEXT,
    bingKey TEXT,
    mapUrl TEXT,
    overlayUrl TEXT,
    latitude REAL DEFAULT 0,
    longitude REAL DEFAULT 0,
    zoom INTEGER DEFAULT 0,
    forceSettings INTEGER DEFAULT 0,
    coordinateFormat TEXT,
    attributes TEXT DEFAULT '{}'
);

CREATE TABLE IF NOT EXISTS tc_users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    login TEXT,
    email TEXT NOT NULL UNIQUE,
    phone TEXT,
    readonly INTEGER DEFAULT 0,
    administrator INTEGER DEFAULT 0,
    map TEXT,
    latitude REAL DEFAULT 0,
    longitude REAL DEFAULT 0,
    zoom INTEGER DEFAULT 0,
    coordinateFormat TEXT,
    disabled INTEGER DEFAULT 0,
    expirationTime TEXT,
    deviceLimit INTEGER DEFAULT -1,
    userLimit INTEGER DEFAULT 0,
    deviceReadonly INTEGER DEFAULT 0,
    limitCommands INTEGER DEFAULT 0,
    disableReports INTEGER DEFAULT 0,
    fixedEmail INTEGER DEFAULT 0,
    poiLayer TEXT,
    totpKey TEXT,
    temporary INTEGER DEFAULT 0,
    hashedPassword TEXT,
    salt TEXT,
    attributes TEXT DEFAULT '{}'
);

CREATE TABLE IF NOT EXISTS tc_groups (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    groupId INTEGER REFERENCES tc_groups(id),
    attributes TEXT DEFAULT '{}'
);

CREATE TABLE IF NOT EXISTS tc_devices (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    uniqueId TEXT NOT NULL UNIQUE,
    status TEXT DEFAULT 'offline',
    lastUpdate TEXT,
    positionId INTEGER DEFAULT 0,
    groupId INTEGER REFERENCES tc_groups(id),
    phone TEXT,
    model TEXT,
    contact TEXT,
    category TEXT,
    disabled INTEGER DEFAULT 0,
    expirationTime TEXT,
    calendarId INTEGER,
    motionStreak INTEGER DEFAULT 0,
    motionState INTEGER DEFAULT 0,
    motionPositionId INTEGER DEFAULT 0,
    motionTime TEXT,
    motionDistance REAL DEFAULT 0,
    motionLatitude REAL DEFAULT 0,
    motionLongitude REAL DEFAULT 0,
    overspeedState INTEGER DEFAULT 0,
    overspeedTime TEXT,
    overspeedGeofenceId INTEGER DEFAULT 0,
    attributes TEXT DEFAULT '{}'
);

CREATE TABLE IF NOT EXISTS tc_positions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    protocol TEXT,
    deviceId INTEGER NOT NULL REFERENCES tc_devices(id),
    serverTime TEXT NOT NULL,
    deviceTime TEXT NOT NULL,
    fixTime TEXT NOT NULL,
    valid INTEGER DEFAULT 0,
    latitude REAL DEFAULT 0,
    longitude REAL DEFAULT 0,
    altitude REAL DEFAULT 0,
    speed REAL DEFAULT 0,
    course REAL DEFAULT 0,
    address TEXT,
    accuracy REAL DEFAULT 0,
    network TEXT,
    geofenceIds TEXT,
    attributes TEXT DEFAULT '{}'
);

CREATE TABLE IF NOT EXISTS tc_events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    type TEXT NOT NULL,
    deviceId INTEGER NOT NULL REFERENCES tc_devices(id),
    eventTime TEXT NOT NULL,
    positionId INTEGER DEFAULT 0,
    geofenceId INTEGER DEFAULT 0,
    maintenanceId INTEGER DEFAULT 0,
    attributes TEXT DEFAULT '{}'
);

CREATE TABLE IF NOT EXISTS tc_commands (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    deviceId INTEGER DEFAULT 0,
    type TEXT NOT NULL,
    description TEXT,
    attributes TEXT DEFAULT '{}'
);

CREATE TABLE IF NOT EXISTS tc_commands_queue (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    deviceId INTEGER NOT NULL REFERENCES tc_devices(id),
    commandType TEXT NOT NULL,
    attributes TEXT DEFAULT '{}'
);

CREATE TABLE IF NOT EXISTS tc_geofences (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    description TEXT,
    area TEXT NOT NULL,
    calendarId INTEGER,
    attributes TEXT DEFAULT '{}'
);

CREATE TABLE IF NOT EXISTS tc_notifications (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    type TEXT NOT NULL,
    always INTEGER DEFAULT 0,
    notificators TEXT,
    calendarId INTEGER,
    commandId INTEGER,
    attributes TEXT DEFAULT '{}'
);

CREATE TABLE IF NOT EXISTS tc_drivers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    uniqueId TEXT NOT NULL UNIQUE,
    attributes TEXT DEFAULT '{}'
);

CREATE TABLE IF NOT EXISTS tc_maintenances (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    type TEXT NOT NULL,
    start REAL DEFAULT 0,
    period REAL DEFAULT 0,
    attributes TEXT DEFAULT '{}'
);

CREATE TABLE IF NOT EXISTS tc_calendars (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    data TEXT,
    attributes TEXT DEFAULT '{}'
);

CREATE TABLE IF NOT EXISTS tc_attributes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    description TEXT NOT NULL,
    attribute TEXT NOT NULL,
    expression TEXT NOT NULL,
    type TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS tc_statistics (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    captureTime TEXT NOT NULL,
    activeUsers INTEGER DEFAULT 0,
    activeDevices INTEGER DEFAULT 0,
    requests INTEGER DEFAULT 0,
    messagesReceived INTEGER DEFAULT 0,
    messagesStored INTEGER DEFAULT 0,
    geocoderRequests INTEGER DEFAULT 0,
    geolocationRequests INTEGER DEFAULT 0,
    mailSent INTEGER DEFAULT 0,
    smsSent INTEGER DEFAULT 0,
    protocol TEXT,
    attributes TEXT DEFAULT '{}'
);

CREATE TABLE IF NOT EXISTS tc_reports (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    type TEXT NOT NULL,
    description TEXT,
    calendarId INTEGER,
    attributes TEXT DEFAULT '{}'
);

CREATE TABLE IF NOT EXISTS tc_orders (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    description TEXT,
    fromAddress TEXT,
    toAddress TEXT,
    attributes TEXT DEFAULT '{}'
);

-- Permission link tables
CREATE TABLE IF NOT EXISTS tc_user_device (
    userId INTEGER NOT NULL REFERENCES tc_users(id) ON DELETE CASCADE,
    deviceId INTEGER NOT NULL REFERENCES tc_devices(id) ON DELETE CASCADE,
    PRIMARY KEY (userId, deviceId)
);

CREATE TABLE IF NOT EXISTS tc_user_group (
    userId INTEGER NOT NULL REFERENCES tc_users(id) ON DELETE CASCADE,
    groupId INTEGER NOT NULL REFERENCES tc_groups(id) ON DELETE CASCADE,
    PRIMARY KEY (userId, groupId)
);

CREATE TABLE IF NOT EXISTS tc_user_geofence (
    userId INTEGER NOT NULL REFERENCES tc_users(id) ON DELETE CASCADE,
    geofenceId INTEGER NOT NULL REFERENCES tc_geofences(id) ON DELETE CASCADE,
    PRIMARY KEY (userId, geofenceId)
);

CREATE TABLE IF NOT EXISTS tc_user_notification (
    userId INTEGER NOT NULL REFERENCES tc_users(id) ON DELETE CASCADE,
    notificationId INTEGER NOT NULL REFERENCES tc_notifications(id) ON DELETE CASCADE,
    PRIMARY KEY (userId, notificationId)
);

CREATE TABLE IF NOT EXISTS tc_user_calendar (
    userId INTEGER NOT NULL REFERENCES tc_users(id) ON DELETE CASCADE,
    calendarId INTEGER NOT NULL REFERENCES tc_calendars(id) ON DELETE CASCADE,
    PRIMARY KEY (userId, calendarId)
);

CREATE TABLE IF NOT EXISTS tc_user_attribute (
    userId INTEGER NOT NULL REFERENCES tc_users(id) ON DELETE CASCADE,
    attributeId INTEGER NOT NULL REFERENCES tc_attributes(id) ON DELETE CASCADE,
    PRIMARY KEY (userId, attributeId)
);

CREATE TABLE IF NOT EXISTS tc_user_driver (
    userId INTEGER NOT NULL REFERENCES tc_users(id) ON DELETE CASCADE,
    driverId INTEGER NOT NULL REFERENCES tc_drivers(id) ON DELETE CASCADE,
    PRIMARY KEY (userId, driverId)
);

CREATE TABLE IF NOT EXISTS tc_user_command (
    userId INTEGER NOT NULL REFERENCES tc_users(id) ON DELETE CASCADE,
    commandId INTEGER NOT NULL REFERENCES tc_commands(id) ON DELETE CASCADE,
    PRIMARY KEY (userId, commandId)
);

CREATE TABLE IF NOT EXISTS tc_user_maintenance (
    userId INTEGER NOT NULL REFERENCES tc_users(id) ON DELETE CASCADE,
    maintenanceId INTEGER NOT NULL REFERENCES tc_maintenances(id) ON DELETE CASCADE,
    PRIMARY KEY (userId, maintenanceId)
);

CREATE TABLE IF NOT EXISTS tc_user_order (
    userId INTEGER NOT NULL REFERENCES tc_users(id) ON DELETE CASCADE,
    orderId INTEGER NOT NULL REFERENCES tc_orders(id) ON DELETE CASCADE,
    PRIMARY KEY (userId, orderId)
);

CREATE TABLE IF NOT EXISTS tc_user_report (
    userId INTEGER NOT NULL REFERENCES tc_users(id) ON DELETE CASCADE,
    reportId INTEGER NOT NULL REFERENCES tc_reports(id) ON DELETE CASCADE,
    PRIMARY KEY (userId, reportId)
);

CREATE TABLE IF NOT EXISTS tc_user_user (
    userId INTEGER NOT NULL REFERENCES tc_users(id) ON DELETE CASCADE,
    managedUserId INTEGER NOT NULL REFERENCES tc_users(id) ON DELETE CASCADE,
    PRIMARY KEY (userId, managedUserId)
);

CREATE TABLE IF NOT EXISTS tc_group_geofence (
    groupId INTEGER NOT NULL REFERENCES tc_groups(id) ON DELETE CASCADE,
    geofenceId INTEGER NOT NULL REFERENCES tc_geofences(id) ON DELETE CASCADE,
    PRIMARY KEY (groupId, geofenceId)
);

CREATE TABLE IF NOT EXISTS tc_group_notification (
    groupId INTEGER NOT NULL REFERENCES tc_groups(id) ON DELETE CASCADE,
    notificationId INTEGER NOT NULL REFERENCES tc_notifications(id) ON DELETE CASCADE,
    PRIMARY KEY (groupId, notificationId)
);

CREATE TABLE IF NOT EXISTS tc_group_attribute (
    groupId INTEGER NOT NULL REFERENCES tc_groups(id) ON DELETE CASCADE,
    attributeId INTEGER NOT NULL REFERENCES tc_attributes(id) ON DELETE CASCADE,
    PRIMARY KEY (groupId, attributeId)
);

CREATE TABLE IF NOT EXISTS tc_group_driver (
    groupId INTEGER NOT NULL REFERENCES tc_groups(id) ON DELETE CASCADE,
    driverId INTEGER NOT NULL REFERENCES tc_drivers(id) ON DELETE CASCADE,
    PRIMARY KEY (groupId, driverId)
);

CREATE TABLE IF NOT EXISTS tc_group_command (
    groupId INTEGER NOT NULL REFERENCES tc_groups(id) ON DELETE CASCADE,
    commandId INTEGER NOT NULL REFERENCES tc_commands(id) ON DELETE CASCADE,
    PRIMARY KEY (groupId, commandId)
);

CREATE TABLE IF NOT EXISTS tc_group_maintenance (
    groupId INTEGER NOT NULL REFERENCES tc_groups(id) ON DELETE CASCADE,
    maintenanceId INTEGER NOT NULL REFERENCES tc_maintenances(id) ON DELETE CASCADE,
    PRIMARY KEY (groupId, maintenanceId)
);

CREATE TABLE IF NOT EXISTS tc_device_geofence (
    deviceId INTEGER NOT NULL REFERENCES tc_devices(id) ON DELETE CASCADE,
    geofenceId INTEGER NOT NULL REFERENCES tc_geofences(id) ON DELETE CASCADE,
    PRIMARY KEY (deviceId, geofenceId)
);

CREATE TABLE IF NOT EXISTS tc_device_notification (
    deviceId INTEGER NOT NULL REFERENCES tc_devices(id) ON DELETE CASCADE,
    notificationId INTEGER NOT NULL REFERENCES tc_notifications(id) ON DELETE CASCADE,
    PRIMARY KEY (deviceId, notificationId)
);

CREATE TABLE IF NOT EXISTS tc_device_attribute (
    deviceId INTEGER NOT NULL REFERENCES tc_devices(id) ON DELETE CASCADE,
    attributeId INTEGER NOT NULL REFERENCES tc_attributes(id) ON DELETE CASCADE,
    PRIMARY KEY (deviceId, attributeId)
);

CREATE TABLE IF NOT EXISTS tc_device_driver (
    deviceId INTEGER NOT NULL REFERENCES tc_devices(id) ON DELETE CASCADE,
    driverId INTEGER NOT NULL REFERENCES tc_drivers(id) ON DELETE CASCADE,
    PRIMARY KEY (deviceId, driverId)
);

CREATE TABLE IF NOT EXISTS tc_device_command (
    deviceId INTEGER NOT NULL REFERENCES tc_devices(id) ON DELETE CASCADE,
    commandId INTEGER NOT NULL REFERENCES tc_commands(id) ON DELETE CASCADE,
    PRIMARY KEY (deviceId, commandId)
);

CREATE TABLE IF NOT EXISTS tc_device_maintenance (
    deviceId INTEGER NOT NULL REFERENCES tc_devices(id) ON DELETE CASCADE,
    maintenanceId INTEGER NOT NULL REFERENCES tc_maintenances(id) ON DELETE CASCADE,
    PRIMARY KEY (deviceId, maintenanceId)
);

CREATE TABLE IF NOT EXISTS tc_device_order (
    deviceId INTEGER NOT NULL REFERENCES tc_devices(id) ON DELETE CASCADE,
    orderId INTEGER NOT NULL REFERENCES tc_orders(id) ON DELETE CASCADE,
    PRIMARY KEY (deviceId, orderId)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_positions_deviceid ON tc_positions(deviceId);
CREATE INDEX IF NOT EXISTS idx_positions_fixtime ON tc_positions(fixTime);
CREATE INDEX IF NOT EXISTS idx_events_deviceid ON tc_events(deviceId);
CREATE INDEX IF NOT EXISTS idx_events_eventtime ON tc_events(eventTime);
CREATE INDEX IF NOT EXISTS idx_devices_uniqueid ON tc_devices(uniqueId);

-- Insert default server entry
INSERT OR IGNORE INTO tc_servers (id, registration, latitude, longitude, zoom)
VALUES (1, 1, 0, 0, 0);

-- Insert default admin user (password: admin)
INSERT OR IGNORE INTO tc_users (id, name, email, administrator, hashedPassword, salt)
VALUES (1, 'admin', 'admin', 1, 'ECB5A767F603E7A4B187B484F19643FA7B11C5E916F237B8F43FF7FC39FC713D', '000000000000000000000000000F42400000000000000000');
