use async_trait::async_trait;
use traccar_model::{position_keys, Event, Position};
use crate::{EventHandler, HandlerState};

/// Generates device-moving and device-stopped events based on the
/// motion attribute set by the MotionHandler in the position pipeline.
pub struct MotionEventHandler;

impl MotionEventHandler {
    pub fn new() -> Self {
        Self
    }
}

#[async_trait]
impl EventHandler for MotionEventHandler {
    fn name(&self) -> &str {
        "motion"
    }

    async fn analyze_position(
        &self,
        position: &Position,
        _state: &dyn HandlerState,
    ) -> Vec<Event> {
        let motion = match position.get_boolean(position_keys::KEY_MOTION) {
            Some(v) => v,
            None => return Vec::new(),
        };

        // TODO: compare with device's previous motion state and apply
        // debounce logic (motion streak, distance threshold, time threshold).
        // For now return empty -- a full implementation would track state
        // on the Device struct.
        let _ = motion;

        Vec::new()
    }
}
