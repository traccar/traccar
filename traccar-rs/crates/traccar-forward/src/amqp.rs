use async_trait::async_trait;
use traccar_model::{Event, Position};

use crate::{EventForwarder, ForwardError};

/// Forwards position data via AMQP (e.g. RabbitMQ).
pub struct AmqpForwarder {
    pub url: String,
    pub exchange: String,
    pub routing_key: String,
}

impl AmqpForwarder {
    pub fn new(url: &str, exchange: &str, routing_key: &str) -> Self {
        Self {
            url: url.to_string(),
            exchange: exchange.to_string(),
            routing_key: routing_key.to_string(),
        }
    }
}

#[async_trait]
impl EventForwarder for AmqpForwarder {
    fn name(&self) -> &str {
        "amqp"
    }

    async fn forward(
        &self,
        position: &Position,
        event: Option<&Event>,
    ) -> Result<(), ForwardError> {
        let payload = serde_json::to_vec(&serde_json::json!({
            "position": position,
            "event": event,
        }))
        .map_err(|e| ForwardError::Send(e.to_string()))?;

        tracing::debug!(
            url = %self.url,
            exchange = %self.exchange,
            device_id = position.device_id,
            "Publishing to AMQP (stub)"
        );

        // A full implementation would use lapin or amqprs.
        let _ = payload;
        Ok(())
    }
}
