use async_trait::async_trait;
use traccar_model::Position;
use crate::{HandlerError, HandlerState, PositionHandler};

/// Performs reverse geocoding to fill in the position's address field.
///
/// This is a placeholder implementation. A full version would call an external
/// geocoding service (Nominatim, Google, etc.) via HTTP.
pub struct GeocoderHandler;

impl GeocoderHandler {
    pub fn new() -> Self {
        Self
    }
}

#[async_trait]
impl PositionHandler for GeocoderHandler {
    fn name(&self) -> &str {
        "geocoder"
    }

    async fn handle_position(
        &self,
        position: &mut Position,
        state: &dyn HandlerState,
    ) -> Result<bool, HandlerError> {
        // Skip if geocoding is disabled
        if !state.get_config_bool("geocoder.enable") {
            return Ok(true);
        }

        // Skip if position already has an address
        if position.address.is_some() {
            return Ok(true);
        }

        // Skip invalid positions
        if position.latitude == 0.0 && position.longitude == 0.0 {
            return Ok(true);
        }

        // TODO: call external geocoding service
        tracing::trace!(
            device_id = position.device_id,
            lat = position.latitude,
            lon = position.longitude,
            "Reverse geocoding not yet implemented"
        );

        Ok(true)
    }
}
