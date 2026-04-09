use async_trait::async_trait;
use traccar_model::{position_keys, Position};
use crate::{HandlerError, HandlerState, PositionHandler};

pub struct MotionHandler {
    threshold: f64,
}

impl MotionHandler {
    pub fn new(threshold: f64) -> Self {
        Self {
            threshold: if threshold > 0.0 { threshold } else { 0.01 },
        }
    }
}

#[async_trait]
impl PositionHandler for MotionHandler {
    fn name(&self) -> &str {
        "motion"
    }

    async fn handle_position(
        &self,
        position: &mut Position,
        _state: &dyn HandlerState,
    ) -> Result<bool, HandlerError> {
        let motion = position.speed > self.threshold;
        position.set(position_keys::KEY_MOTION, motion);
        Ok(true)
    }
}
