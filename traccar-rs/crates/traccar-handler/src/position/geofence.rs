use async_trait::async_trait;
use traccar_model::Position;
use crate::{HandlerError, HandlerState, PositionHandler};

/// Checks geofence entry/exit and updates the position's geofence_ids.
///
/// In a full implementation this would look up geofences assigned to the device
/// and test whether the position falls inside each one.
pub struct GeofenceHandler;

impl GeofenceHandler {
    pub fn new() -> Self {
        Self
    }
}

#[async_trait]
impl PositionHandler for GeofenceHandler {
    fn name(&self) -> &str {
        "geofence"
    }

    async fn handle_position(
        &self,
        position: &mut Position,
        _state: &dyn HandlerState,
    ) -> Result<bool, HandlerError> {
        // TODO: look up device geofences from storage and test containment.
        // For now, preserve any existing geofence_ids and always continue.
        if position.geofence_ids.is_none() {
            position.geofence_ids = Some(Vec::new());
        }

        Ok(true)
    }
}
