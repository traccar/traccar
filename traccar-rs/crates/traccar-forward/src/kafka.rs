use async_trait::async_trait;
use traccar_model::{Event, Position};

use crate::{EventForwarder, ForwardError};

/// Forwards position data to Apache Kafka.
pub struct KafkaForwarder {
    pub brokers: String,
    pub topic: String,
}

impl KafkaForwarder {
    pub fn new(brokers: &str, topic: &str) -> Self {
        Self {
            brokers: brokers.to_string(),
            topic: topic.to_string(),
        }
    }
}

#[async_trait]
impl EventForwarder for KafkaForwarder {
    fn name(&self) -> &str {
        "kafka"
    }

    async fn forward(
        &self,
        position: &Position,
        event: Option<&Event>,
    ) -> Result<(), ForwardError> {
        let payload = serde_json::json!({
            "position": position,
            "event": event,
        });

        tracing::debug!(
            brokers = %self.brokers,
            topic = %self.topic,
            device_id = position.device_id,
            "Publishing to Kafka (stub)"
        );

        // A full implementation would use rdkafka or a similar client.
        let _ = payload;
        Ok(())
    }
}
