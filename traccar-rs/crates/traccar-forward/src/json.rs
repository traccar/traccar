use async_trait::async_trait;
use traccar_model::{Event, Position};
use crate::{EventForwarder, ForwardError};

pub struct JsonForwarder;
impl JsonForwarder { pub fn new() -> Self { Self } }

#[async_trait]
impl EventForwarder for JsonForwarder {
    fn name(&self) -> &str { "json" }
    async fn forward(&self, _position: &Position, _event: Option<&Event>) -> Result<(), ForwardError> {
        tracing::debug!("Forwarding via {}", self.name());
        Ok(())
    }
}
