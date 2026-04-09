use async_trait::async_trait;
use traccar_model::{position_keys, Event, Position};
use crate::{EventHandler, HandlerState};

/// Detects fuel drop and fuel increase events by comparing the current
/// fuel level against a previous value.
pub struct FuelEventHandler;

impl FuelEventHandler {
    pub fn new() -> Self {
        Self
    }
}

#[async_trait]
impl EventHandler for FuelEventHandler {
    fn name(&self) -> &str {
        "fuel"
    }

    async fn analyze_position(
        &self,
        position: &Position,
        state: &dyn HandlerState,
    ) -> Vec<Event> {
        let _fuel_level = match position.get_double(position_keys::KEY_FUEL_LEVEL) {
            Some(v) => v,
            None => return Vec::new(),
        };

        let _drop_threshold = state
            .get_config_int("event.fuelDropThreshold")
            .unwrap_or(0) as f64;

        let _increase_threshold = state
            .get_config_int("event.fuelIncreaseThreshold")
            .unwrap_or(0) as f64;

        // TODO: compare with previous position's fuel level from storage.
        // Generate TYPE_DEVICE_FUEL_DROP or TYPE_DEVICE_FUEL_INCREASE events.
        Vec::new()
    }
}
