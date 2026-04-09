use async_trait::async_trait;
use traccar_model::{Event, Position};
use crate::{EventForwarder, ForwardError};

pub struct KafkaForwarder;
impl KafkaForwarder { pub fn new() -> Self { Self } }

#[async_trait]
impl EventForwarder for KafkaForwarder {
    fn name(&self) -> &str { "kafka" }
    async fn forward(&self, _position: &Position, _event: Option<&Event>) -> Result<(), ForwardError> {
        tracing::debug!("Forwarding via {}", self.name());
        Ok(())
    }
}
