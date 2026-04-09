use async_trait::async_trait;
use traccar_model::{Event, Position};
use crate::{EventHandler, HandlerState};

/// Generates geofence-enter and geofence-exit events by comparing the
/// current position's geofence_ids with those of the previous position.
pub struct GeofenceEventHandler;

impl GeofenceEventHandler {
    pub fn new() -> Self {
        Self
    }
}

#[async_trait]
impl EventHandler for GeofenceEventHandler {
    fn name(&self) -> &str {
        "geofence"
    }

    async fn analyze_position(
        &self,
        position: &Position,
        _state: &dyn HandlerState,
    ) -> Vec<Event> {
        let current_ids = match &position.geofence_ids {
            Some(ids) => ids.clone(),
            None => Vec::new(),
        };

        // TODO: fetch previous position's geofence_ids from storage.
        let previous_ids: Vec<i64> = Vec::new();

        let mut events = Vec::new();

        // Detect entries (in current but not in previous)
        for &id in &current_ids {
            if !previous_ids.contains(&id) {
                let mut event = Event::new(Event::TYPE_GEOFENCE_ENTER, position);
                event.geofence_id = id;
                events.push(event);
            }
        }

        // Detect exits (in previous but not in current)
        for &id in &previous_ids {
            if !current_ids.contains(&id) {
                let mut event = Event::new(Event::TYPE_GEOFENCE_EXIT, position);
                event.geofence_id = id;
                events.push(event);
            }
        }

        events
    }
}
