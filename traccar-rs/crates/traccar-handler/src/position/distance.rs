use async_trait::async_trait;
use traccar_model::{position_keys, Position};
use crate::{HandlerError, HandlerState, PositionHandler};

pub struct DistanceHandler;

impl DistanceHandler {
    pub fn new() -> Self {
        Self
    }
}

#[async_trait]
impl PositionHandler for DistanceHandler {
    fn name(&self) -> &str {
        "distance"
    }

    async fn handle_position(
        &self,
        position: &mut Position,
        _state: &dyn HandlerState,
    ) -> Result<bool, HandlerError> {
        if position.has_attribute(position_keys::KEY_TOTAL_DISTANCE) {
            return Ok(true);
        }

        let distance = position
            .get_double(position_keys::KEY_DISTANCE)
            .unwrap_or(0.0);
        let total = position
            .get_double(position_keys::KEY_TOTAL_DISTANCE)
            .unwrap_or(0.0);
        position.set(position_keys::KEY_TOTAL_DISTANCE, total + distance);

        Ok(true)
    }
}
