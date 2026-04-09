use async_trait::async_trait;
use traccar_model::Position;
use crate::{HandlerError, HandlerState, PositionHandler};

/// Runs after all event analysis is complete.
///
/// Responsible for final bookkeeping such as updating device status,
/// flushing caches, and sending notifications.
pub struct PostProcessHandler;

impl PostProcessHandler {
    pub fn new() -> Self {
        Self
    }
}

#[async_trait]
impl PositionHandler for PostProcessHandler {
    fn name(&self) -> &str {
        "post_process"
    }

    async fn handle_position(
        &self,
        position: &mut Position,
        _state: &dyn HandlerState,
    ) -> Result<bool, HandlerError> {
        // TODO: update device last-update timestamp, flush notification
        // queue, etc.
        tracing::trace!(
            device_id = position.device_id,
            "Post-processing complete"
        );
        Ok(true)
    }
}
