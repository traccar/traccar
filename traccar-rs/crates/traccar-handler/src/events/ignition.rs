use async_trait::async_trait;
use traccar_model::{position_keys, Event, Position};
use crate::{EventHandler, HandlerState};

/// Generates ignition-on and ignition-off events when the ignition
/// attribute changes between positions.
pub struct IgnitionEventHandler;

impl IgnitionEventHandler {
    pub fn new() -> Self {
        Self
    }
}

#[async_trait]
impl EventHandler for IgnitionEventHandler {
    fn name(&self) -> &str {
        "ignition"
    }

    async fn analyze_position(
        &self,
        position: &Position,
        _state: &dyn HandlerState,
    ) -> Vec<Event> {
        let ignition = match position.get_boolean(position_keys::KEY_IGNITION) {
            Some(v) => v,
            None => return Vec::new(),
        };

        // TODO: compare with previous position's ignition state from storage.
        // For now, emit based on current value only as a placeholder.
        let _ = ignition;

        Vec::new()
    }
}
