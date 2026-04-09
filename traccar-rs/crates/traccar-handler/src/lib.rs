pub mod position;
pub mod events;
pub mod post_process;

use async_trait::async_trait;
use traccar_model::{Event, Position};

/// Position handler: processes/transforms a position as it flows through the pipeline.
/// Returns Ok(true) to continue processing, Ok(false) to filter out.
#[async_trait]
pub trait PositionHandler: Send + Sync {
    fn name(&self) -> &str;
    async fn handle_position(
        &self,
        position: &mut Position,
        state: &dyn HandlerState,
    ) -> Result<bool, HandlerError>;
}

/// Event handler: analyzes a position and generates events.
#[async_trait]
pub trait EventHandler: Send + Sync {
    fn name(&self) -> &str;
    async fn analyze_position(
        &self,
        position: &Position,
        state: &dyn HandlerState,
    ) -> Vec<Event>;
}

/// Shared state interface for handlers.
pub trait HandlerState: Send + Sync {
    fn get_config_string(&self, key: &str) -> Option<String>;
    fn get_config_bool(&self, key: &str) -> bool;
    fn get_config_int(&self, key: &str) -> Option<i64>;
}

#[derive(Debug, thiserror::Error)]
pub enum HandlerError {
    #[error("Handler error: {0}")]
    General(String),
    #[error("Storage error: {0}")]
    Storage(String),
    #[error("Geocoder error: {0}")]
    Geocoder(String),
}
