use async_trait::async_trait;
use traccar_model::{position_keys, Event, Position};
use crate::{EventHandler, HandlerState};

/// Generates a driver-changed event when the driver unique ID attribute
/// changes between consecutive positions.
pub struct DriverEventHandler;

impl DriverEventHandler {
    pub fn new() -> Self {
        Self
    }
}

#[async_trait]
impl EventHandler for DriverEventHandler {
    fn name(&self) -> &str {
        "driver"
    }

    async fn analyze_position(
        &self,
        position: &Position,
        _state: &dyn HandlerState,
    ) -> Vec<Event> {
        // TODO: compare against previous position's driver ID from storage.
        // For now, just check whether a driver ID is newly present.
        if let Some(driver_id) = position.get_string(position_keys::KEY_DRIVER_UNIQUE_ID) {
            if !driver_id.is_empty() {
                // In a full implementation we would only fire when the driver
                // actually changed from the previous position.
                let _ = driver_id;
            }
        }
        Vec::new()
    }
}
