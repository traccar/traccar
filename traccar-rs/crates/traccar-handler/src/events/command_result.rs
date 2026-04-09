use async_trait::async_trait;
use traccar_model::{position_keys, Event, Position};
use crate::{EventHandler, HandlerState};

/// Generates a command result event when a position carries a result attribute,
/// indicating that a device has responded to a command.
pub struct CommandResultEventHandler;

impl CommandResultEventHandler {
    pub fn new() -> Self {
        Self
    }
}

#[async_trait]
impl EventHandler for CommandResultEventHandler {
    fn name(&self) -> &str {
        "command_result"
    }

    async fn analyze_position(
        &self,
        position: &Position,
        _state: &dyn HandlerState,
    ) -> Vec<Event> {
        if let Some(result) = position.get_string(position_keys::KEY_RESULT) {
            if !result.is_empty() {
                let mut event = Event::new(Event::TYPE_COMMAND_RESULT, position);
                event.attributes = serde_json::json!({ "result": result });
                return vec![event];
            }
        }
        Vec::new()
    }
}
