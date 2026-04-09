use async_trait::async_trait;
use traccar_model::Position;
use crate::{HandlerError, HandlerState, PositionHandler};

/// Persists positions to the database.
///
/// This is typically the last handler in the position processing pipeline.
/// In a full implementation it would use the storage layer to insert or update
/// the position row.
pub struct DatabaseHandler;

impl DatabaseHandler {
    pub fn new() -> Self {
        Self
    }
}

#[async_trait]
impl PositionHandler for DatabaseHandler {
    fn name(&self) -> &str {
        "database"
    }

    async fn handle_position(
        &self,
        position: &mut Position,
        _state: &dyn HandlerState,
    ) -> Result<bool, HandlerError> {
        // TODO: persist the position via traccar-storage.
        tracing::debug!(
            device_id = position.device_id,
            protocol = %position.protocol,
            "Would save position to database (not yet implemented)"
        );

        Ok(true)
    }
}
