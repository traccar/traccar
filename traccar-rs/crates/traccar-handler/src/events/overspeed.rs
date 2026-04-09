use async_trait::async_trait;
use traccar_model::{position_keys, Event, Position};
use crate::{EventHandler, HandlerState};

/// Generates an overspeed event when the position's speed exceeds the
/// configured or per-geofence speed limit.
pub struct OverspeedEventHandler;

impl OverspeedEventHandler {
    pub fn new() -> Self {
        Self
    }
}

#[async_trait]
impl EventHandler for OverspeedEventHandler {
    fn name(&self) -> &str {
        "overspeed"
    }

    async fn analyze_position(
        &self,
        position: &Position,
        state: &dyn HandlerState,
    ) -> Vec<Event> {
        let speed_limit = position
            .get_double(position_keys::KEY_SPEED_LIMIT)
            .or_else(|| state.get_config_int("event.overspeed.limit").map(|v| v as f64));

        let limit = match speed_limit {
            Some(l) if l > 0.0 => l,
            _ => return Vec::new(),
        };

        let min_duration = state
            .get_config_int("event.overspeed.minimalDuration")
            .unwrap_or(0);

        // TODO: track overspeed duration on the Device struct and only fire
        // after the minimal duration has elapsed.
        let _ = min_duration;

        if position.speed > limit {
            let mut event = Event::new(Event::TYPE_DEVICE_OVERSPEED, position);
            event.attributes = serde_json::json!({
                "speed": position.speed,
                "speedLimit": limit,
            });
            return vec![event];
        }

        Vec::new()
    }
}
