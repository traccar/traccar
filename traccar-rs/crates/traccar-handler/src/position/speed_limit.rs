use async_trait::async_trait;
use traccar_model::{position_keys, Position};
use crate::{HandlerError, HandlerState, PositionHandler};

/// Applies a speed limit from configuration or map data and stores it
/// as an attribute on the position.
pub struct SpeedLimitHandler;

impl SpeedLimitHandler {
    pub fn new() -> Self {
        Self
    }
}

#[async_trait]
impl PositionHandler for SpeedLimitHandler {
    fn name(&self) -> &str {
        "speed_limit"
    }

    async fn handle_position(
        &self,
        position: &mut Position,
        state: &dyn HandlerState,
    ) -> Result<bool, HandlerError> {
        // If a global speed limit is configured, apply it
        if let Some(limit) = state.get_config_int("speedLimit.value") {
            if !position.has_attribute(position_keys::KEY_SPEED_LIMIT) {
                position.set(position_keys::KEY_SPEED_LIMIT, limit as f64);
            }
        }

        Ok(true)
    }
}
