pub mod multicast;
pub mod redis;

use async_trait::async_trait;
use traccar_model::{Event, Position};

#[derive(Debug, thiserror::Error)]
pub enum BroadcastError {
    #[error("Broadcast error: {0}")]
    Send(String),
}

#[async_trait]
pub trait BroadcastService: Send + Sync {
    async fn start(&self) -> Result<(), BroadcastError> { Ok(()) }
    async fn stop(&self) -> Result<(), BroadcastError> { Ok(()) }
    async fn update_position(&self, position: &Position) -> Result<(), BroadcastError>;
    async fn update_event(&self, event: &Event) -> Result<(), BroadcastError>;
}

pub struct NullBroadcast;

#[async_trait]
impl BroadcastService for NullBroadcast {
    async fn update_position(&self, _position: &Position) -> Result<(), BroadcastError> { Ok(()) }
    async fn update_event(&self, _event: &Event) -> Result<(), BroadcastError> { Ok(()) }
}
