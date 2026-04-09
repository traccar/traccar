use chrono::{DateTime, Utc};
use dashmap::DashMap;
use serde_json::Value;
use std::collections::HashMap;
use std::sync::Arc;

// ─── Device session ─────────────────────────────────────────────────

/// Tracks the state of a connected device within a specific protocol session.
#[derive(Debug, Clone)]
pub struct DeviceSession {
    /// Internal device ID (from database).
    pub device_id: i64,
    /// Device's unique identifier (IMEI or similar).
    pub unique_id: String,
    /// Protocol name this session belongs to.
    pub protocol: String,
    /// Device model (if known).
    pub model: Option<String>,
    /// Timestamp of the last message received from this device.
    pub last_activity: DateTime<Utc>,
    /// Protocol-specific local state storage.
    pub local_state: HashMap<String, Value>,
}

impl DeviceSession {
    /// Create a new device session.
    pub fn new(device_id: i64, unique_id: String, protocol: String) -> Self {
        Self {
            device_id,
            unique_id,
            protocol,
            model: None,
            last_activity: Utc::now(),
            local_state: HashMap::new(),
        }
    }

    /// Create an unauthenticated session (device_id = 0) for protocols that
    /// identify themselves before sending positions.
    pub fn unauthenticated(protocol: &str) -> Self {
        Self {
            device_id: 0,
            unique_id: String::new(),
            protocol: protocol.to_string(),
            model: None,
            last_activity: Utc::now(),
            local_state: HashMap::new(),
        }
    }

    /// Check if the session has been authenticated (device resolved).
    pub fn is_authenticated(&self) -> bool {
        self.device_id > 0
    }

    /// Update the last activity timestamp.
    pub fn touch(&mut self) {
        self.last_activity = Utc::now();
    }

    /// Set a local state value.
    pub fn set_state<V: Into<Value>>(&mut self, key: &str, value: V) {
        self.local_state.insert(key.to_string(), value.into());
    }

    /// Get a local state value.
    pub fn get_state(&self, key: &str) -> Option<&Value> {
        self.local_state.get(key)
    }

    /// Get a local state string value.
    pub fn get_state_string(&self, key: &str) -> Option<String> {
        self.local_state.get(key).and_then(|v| match v {
            Value::String(s) => Some(s.clone()),
            _ => Some(v.to_string()),
        })
    }

    /// Get a local state integer value.
    pub fn get_state_i64(&self, key: &str) -> Option<i64> {
        self.local_state.get(key).and_then(|v| match v {
            Value::Number(n) => n.as_i64(),
            _ => None,
        })
    }

    /// Set the device model.
    pub fn set_model(&mut self, model: &str) {
        self.model = Some(model.to_string());
    }
}

// ─── Connection manager ─────────────────────────────────────────────

/// Connection key combining remote address info and protocol.
#[derive(Debug, Clone, Hash, PartialEq, Eq)]
pub struct ConnectionKey {
    pub remote_address: String,
    pub protocol: String,
}

impl ConnectionKey {
    pub fn new(remote_address: String, protocol: String) -> Self {
        Self {
            remote_address,
            protocol,
        }
    }
}

/// Manages device sessions across all protocols and connections.
/// Uses DashMap for concurrent access without requiring mutable references.
pub struct ConnectionManager {
    /// Map from connection key to device session.
    sessions_by_connection: DashMap<ConnectionKey, DeviceSession>,
    /// Map from device unique_id to connection key (reverse lookup).
    connections_by_device: DashMap<String, ConnectionKey>,
    /// Timeout for inactive sessions in seconds.
    session_timeout_secs: i64,
}

impl ConnectionManager {
    pub fn new(session_timeout_secs: i64) -> Self {
        Self {
            sessions_by_connection: DashMap::new(),
            connections_by_device: DashMap::new(),
            session_timeout_secs,
        }
    }

    /// Register or update a device session for a connection.
    pub fn register_session(&self, key: ConnectionKey, session: DeviceSession) {
        let unique_id = session.unique_id.clone();
        self.sessions_by_connection.insert(key.clone(), session);
        if !unique_id.is_empty() {
            self.connections_by_device.insert(unique_id, key);
        }
    }

    /// Get a device session by connection key.
    pub fn get_session(&self, key: &ConnectionKey) -> Option<DeviceSession> {
        self.sessions_by_connection.get(key).map(|s| s.clone())
    }

    /// Get a device session by unique device ID.
    pub fn get_session_by_device(&self, unique_id: &str) -> Option<DeviceSession> {
        self.connections_by_device
            .get(unique_id)
            .and_then(|key| self.sessions_by_connection.get(key.value()).map(|s| s.clone()))
    }

    /// Remove a session by connection key.
    pub fn remove_session(&self, key: &ConnectionKey) -> Option<DeviceSession> {
        if let Some((_, session)) = self.sessions_by_connection.remove(key) {
            self.connections_by_device.remove(&session.unique_id);
            Some(session)
        } else {
            None
        }
    }

    /// Update the last activity time for a session.
    pub fn touch_session(&self, key: &ConnectionKey) {
        if let Some(mut session) = self.sessions_by_connection.get_mut(key) {
            session.touch();
        }
    }

    /// Remove expired sessions and return the list of removed unique IDs.
    pub fn cleanup_expired(&self) -> Vec<String> {
        let now = Utc::now();
        let mut expired_keys = Vec::new();

        for entry in self.sessions_by_connection.iter() {
            let elapsed = now.signed_duration_since(entry.value().last_activity);
            if elapsed.num_seconds() > self.session_timeout_secs {
                expired_keys.push(entry.key().clone());
            }
        }

        let mut removed_devices = Vec::new();
        for key in expired_keys {
            if let Some(session) = self.remove_session(&key) {
                tracing::info!(
                    protocol = %session.protocol,
                    device = %session.unique_id,
                    "Session expired and removed"
                );
                removed_devices.push(session.unique_id);
            }
        }

        removed_devices
    }

    /// Get the number of active sessions.
    pub fn active_session_count(&self) -> usize {
        self.sessions_by_connection.len()
    }

    /// Get the number of connected devices.
    pub fn connected_device_count(&self) -> usize {
        self.connections_by_device.len()
    }
}

impl Default for ConnectionManager {
    fn default() -> Self {
        Self::new(600) // 10 minute default timeout
    }
}

/// Shared connection manager wrapped in Arc for thread-safe sharing.
pub type SharedConnectionManager = Arc<ConnectionManager>;

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_device_session_creation() {
        let session = DeviceSession::new(42, "123456789012345".to_string(), "gps103".to_string());
        assert_eq!(session.device_id, 42);
        assert_eq!(session.unique_id, "123456789012345");
        assert!(session.is_authenticated());
    }

    #[test]
    fn test_unauthenticated_session() {
        let session = DeviceSession::unauthenticated("gps103");
        assert_eq!(session.device_id, 0);
        assert!(!session.is_authenticated());
    }

    #[test]
    fn test_local_state() {
        let mut session = DeviceSession::new(1, "test".to_string(), "test".to_string());
        session.set_state("key1", "value1");
        session.set_state("count", 42i64);

        assert_eq!(session.get_state_string("key1").unwrap(), "value1");
        assert_eq!(session.get_state_i64("count").unwrap(), 42);
    }

    #[test]
    fn test_connection_manager() {
        let mgr = ConnectionManager::new(600);
        let key = ConnectionKey::new("127.0.0.1:5000".to_string(), "gps103".to_string());
        let session = DeviceSession::new(1, "imei123".to_string(), "gps103".to_string());

        mgr.register_session(key.clone(), session);
        assert_eq!(mgr.active_session_count(), 1);

        let retrieved = mgr.get_session(&key).unwrap();
        assert_eq!(retrieved.unique_id, "imei123");

        let by_device = mgr.get_session_by_device("imei123").unwrap();
        assert_eq!(by_device.device_id, 1);

        mgr.remove_session(&key);
        assert_eq!(mgr.active_session_count(), 0);
    }
}
