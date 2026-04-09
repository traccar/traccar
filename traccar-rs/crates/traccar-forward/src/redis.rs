use async_trait::async_trait;
use traccar_model::{Event, Position};
use crate::{EventForwarder, ForwardError};

pub struct RedisForwarder;
impl RedisForwarder { pub fn new() -> Self { Self } }

#[async_trait]
impl EventForwarder for RedisForwarder {
    fn name(&self) -> &str { "redis" }
    async fn forward(&self, _position: &Position, _event: Option<&Event>) -> Result<(), ForwardError> {
        tracing::debug!("Forwarding via {}", self.name());
        Ok(())
    }
}
