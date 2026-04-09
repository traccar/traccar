use async_trait::async_trait;
use traccar_model::{Event, Position};
use crate::{EventForwarder, ForwardError};

pub struct MqttForwarder;
impl MqttForwarder { pub fn new() -> Self { Self } }

#[async_trait]
impl EventForwarder for MqttForwarder {
    fn name(&self) -> &str { "mqtt" }
    async fn forward(&self, _position: &Position, _event: Option<&Event>) -> Result<(), ForwardError> {
        tracing::debug!("Forwarding via {}", self.name());
        Ok(())
    }
}
