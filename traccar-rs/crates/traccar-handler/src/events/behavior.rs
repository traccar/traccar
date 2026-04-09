use async_trait::async_trait;
use traccar_model::{Event, Position};
use crate::{EventHandler, HandlerState};

/// Detects harsh driving behavior events such as hard acceleration,
/// hard braking, and hard cornering based on position attributes.
pub struct BehaviorEventHandler;

impl BehaviorEventHandler {
    pub fn new() -> Self {
        Self
    }
}

#[async_trait]
impl EventHandler for BehaviorEventHandler {
    fn name(&self) -> &str {
        "behavior"
    }

    async fn analyze_position(
        &self,
        _position: &Position,
        _state: &dyn HandlerState,
    ) -> Vec<Event> {
        // TODO: check acceleration / g-sensor data against thresholds
        // and generate appropriate alarm events.
        Vec::new()
    }
}
