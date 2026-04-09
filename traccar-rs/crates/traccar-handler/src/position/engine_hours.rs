use async_trait::async_trait;
use traccar_model::{position_keys, Position};
use crate::{HandlerError, HandlerState, PositionHandler};

/// Calculates cumulative engine hours based on ignition state.
///
/// When ignition is on, the handler accumulates running time in the
/// `hours` position attribute.
pub struct EngineHoursHandler;

impl EngineHoursHandler {
    pub fn new() -> Self {
        Self
    }
}

#[async_trait]
impl PositionHandler for EngineHoursHandler {
    fn name(&self) -> &str {
        "engine_hours"
    }

    async fn handle_position(
        &self,
        position: &mut Position,
        _state: &dyn HandlerState,
    ) -> Result<bool, HandlerError> {
        // Only compute if ignition data is present and hours are not already set
        if position.has_attribute(position_keys::KEY_HOURS) {
            return Ok(true);
        }

        let ignition = position
            .get_boolean(position_keys::KEY_IGNITION)
            .unwrap_or(false);

        if ignition {
            // TODO: compute delta from previous position's timestamp and
            // accumulate into total engine hours. Requires access to stored
            // previous position. Stub sets hours to 0.
            if !position.has_attribute(position_keys::KEY_HOURS) {
                position.set(position_keys::KEY_HOURS, 0_i64);
            }
        }

        Ok(true)
    }
}
