use async_trait::async_trait;
use traccar_model::Position;
use crate::{HandlerError, HandlerState, PositionHandler};

/// Filters out duplicate or invalid positions based on configurable criteria.
///
/// Configuration keys:
/// - `filter.invalid`: filter positions with valid=false
/// - `filter.zero`: filter positions at (0,0)
/// - `filter.duplicate`: filter positions with identical coordinates
/// - `filter.accuracy`: max allowed accuracy value (meters)
/// - `filter.speed`: max allowed speed value (knots)
pub struct FilterHandler;

impl FilterHandler {
    pub fn new() -> Self {
        Self
    }
}

#[async_trait]
impl PositionHandler for FilterHandler {
    fn name(&self) -> &str {
        "filter"
    }

    async fn handle_position(
        &self,
        position: &mut Position,
        state: &dyn HandlerState,
    ) -> Result<bool, HandlerError> {
        // Filter invalid positions
        if state.get_config_bool("filter.invalid") && !position.valid {
            tracing::debug!(
                device_id = position.device_id,
                "Filtering invalid position"
            );
            return Ok(false);
        }

        // Filter zero-coordinate positions
        if state.get_config_bool("filter.zero")
            && position.latitude == 0.0
            && position.longitude == 0.0
        {
            tracing::debug!(
                device_id = position.device_id,
                "Filtering zero-coordinate position"
            );
            return Ok(false);
        }

        // Filter by accuracy threshold
        if let Some(max_accuracy) = state.get_config_int("filter.accuracy") {
            if position.accuracy > 0.0 && position.accuracy > max_accuracy as f64 {
                tracing::debug!(
                    device_id = position.device_id,
                    accuracy = position.accuracy,
                    max = max_accuracy,
                    "Filtering position with poor accuracy"
                );
                return Ok(false);
            }
        }

        // Filter by maximum speed
        if let Some(max_speed) = state.get_config_int("filter.speed") {
            if position.speed > max_speed as f64 {
                tracing::debug!(
                    device_id = position.device_id,
                    speed = position.speed,
                    max = max_speed,
                    "Filtering position with excessive speed"
                );
                return Ok(false);
            }
        }

        Ok(true)
    }
}
