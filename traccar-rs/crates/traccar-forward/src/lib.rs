pub mod json;
pub mod kafka;
pub mod mqtt;
pub mod amqp;
pub mod redis;

use async_trait::async_trait;
use traccar_model::{Event, Position};

#[derive(Debug, thiserror::Error)]
pub enum ForwardError {
    #[error("Forward error: {0}")]
    Send(String),
}

#[async_trait]
pub trait EventForwarder: Send + Sync {
    fn name(&self) -> &str;
    async fn forward(&self, position: &Position, event: Option<&Event>) -> Result<(), ForwardError>;
}
