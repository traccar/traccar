use async_trait::async_trait;
use traccar_model::Position;
use crate::{HandlerError, HandlerState, PositionHandler};

/// Forwards positions to an external HTTP endpoint.
///
/// When enabled via `forward.enable`, each position is serialized as JSON
/// and POSTed to the URL specified in `forward.url`.
pub struct PositionForwardingHandler;

impl PositionForwardingHandler {
    pub fn new() -> Self {
        Self
    }
}

#[async_trait]
impl PositionHandler for PositionForwardingHandler {
    fn name(&self) -> &str {
        "forwarding"
    }

    async fn handle_position(
        &self,
        position: &mut Position,
        state: &dyn HandlerState,
    ) -> Result<bool, HandlerError> {
        if !state.get_config_bool("forward.enable") {
            return Ok(true);
        }

        let url = match state.get_config_string("forward.url") {
            Some(u) if !u.is_empty() => u,
            _ => return Ok(true),
        };

        // TODO: actually POST the position JSON to the configured URL.
        tracing::debug!(
            device_id = position.device_id,
            url = %url,
            "Would forward position (not yet implemented)"
        );

        Ok(true)
    }
}
