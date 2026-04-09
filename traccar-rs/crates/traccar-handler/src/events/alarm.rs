use async_trait::async_trait;
use traccar_model::{position_keys, Event, Position};
use crate::{EventHandler, HandlerState};

/// Generates alarm events when a position carries an alarm attribute.
pub struct AlarmEventHandler;

impl AlarmEventHandler {
    pub fn new() -> Self {
        Self
    }
}

#[async_trait]
impl EventHandler for AlarmEventHandler {
    fn name(&self) -> &str {
        "alarm"
    }

    async fn analyze_position(
        &self,
        position: &Position,
        _state: &dyn HandlerState,
    ) -> Vec<Event> {
        if let Some(alarm) = position.get_string(position_keys::KEY_ALARM) {
            if !alarm.is_empty() {
                let mut event = Event::new(Event::TYPE_ALARM, position);
                event.attributes = serde_json::json!({ "alarm": alarm });
                return vec![event];
            }
        }
        Vec::new()
    }
}
