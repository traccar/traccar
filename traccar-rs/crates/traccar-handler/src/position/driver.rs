use async_trait::async_trait;
use traccar_model::{position_keys, Position};
use crate::{HandlerError, HandlerState, PositionHandler};

/// Resolves the driver unique ID from the position attributes.
///
/// If a driver identification event is present (e.g. iButton or RFID),
/// the handler ensures the driver unique ID attribute is set.
pub struct DriverHandler;

impl DriverHandler {
    pub fn new() -> Self {
        Self
    }
}

#[async_trait]
impl PositionHandler for DriverHandler {
    fn name(&self) -> &str {
        "driver"
    }

    async fn handle_position(
        &self,
        position: &mut Position,
        _state: &dyn HandlerState,
    ) -> Result<bool, HandlerError> {
        // If a driver unique ID is already present, nothing to do.
        if position.has_attribute(position_keys::KEY_DRIVER_UNIQUE_ID) {
            return Ok(true);
        }

        // If a card attribute is present, use it as the driver unique ID.
        if let Some(card) = position.get_string(position_keys::KEY_CARD) {
            if !card.is_empty() {
                position.set(position_keys::KEY_DRIVER_UNIQUE_ID, card);
            }
        }

        Ok(true)
    }
}
