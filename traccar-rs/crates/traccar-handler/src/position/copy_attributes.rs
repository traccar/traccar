use async_trait::async_trait;
use traccar_model::Position;
use crate::{HandlerError, HandlerState, PositionHandler};

/// Copies configured device attributes onto each position.
///
/// The set of attribute keys to copy is read from the config key
/// `processing.copyAttributes` as a comma-separated list.
pub struct CopyAttributesHandler;

impl CopyAttributesHandler {
    pub fn new() -> Self {
        Self
    }
}

#[async_trait]
impl PositionHandler for CopyAttributesHandler {
    fn name(&self) -> &str {
        "copy_attributes"
    }

    async fn handle_position(
        &self,
        _position: &mut Position,
        state: &dyn HandlerState,
    ) -> Result<bool, HandlerError> {
        // Read list of attributes to copy from device to position
        let _keys = state
            .get_config_string("processing.copyAttributes")
            .unwrap_or_default();

        // TODO: look up device attributes from storage and copy specified keys
        // onto the position. For now this is a no-op stub.

        Ok(true)
    }
}
