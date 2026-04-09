use async_trait::async_trait;
use traccar_model::Position;
use crate::{HandlerError, HandlerState, PositionHandler};

/// Validates and optionally corrects latitude/longitude hemisphere.
///
/// Configuration keys:
/// - `hemisphere.latitude`: force "N" or "S"
/// - `hemisphere.longitude`: force "E" or "W"
pub struct HemisphereHandler;

impl HemisphereHandler {
    pub fn new() -> Self {
        Self
    }
}

#[async_trait]
impl PositionHandler for HemisphereHandler {
    fn name(&self) -> &str {
        "hemisphere"
    }

    async fn handle_position(
        &self,
        position: &mut Position,
        state: &dyn HandlerState,
    ) -> Result<bool, HandlerError> {
        if let Some(lat_hemi) = state.get_config_string("hemisphere.latitude") {
            match lat_hemi.to_uppercase().as_str() {
                "N" => position.latitude = position.latitude.abs(),
                "S" => position.latitude = -(position.latitude.abs()),
                _ => {}
            }
        }

        if let Some(lon_hemi) = state.get_config_string("hemisphere.longitude") {
            match lon_hemi.to_uppercase().as_str() {
                "E" => position.longitude = position.longitude.abs(),
                "W" => position.longitude = -(position.longitude.abs()),
                _ => {}
            }
        }

        Ok(true)
    }
}
