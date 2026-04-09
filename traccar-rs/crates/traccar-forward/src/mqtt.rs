use async_trait::async_trait;
use traccar_model::{Event, Position};

use crate::{EventForwarder, ForwardError};

/// Forwards position data via MQTT.
pub struct MqttForwarder {
    pub broker_url: String,
    pub topic: String,
}

impl MqttForwarder {
    pub fn new(broker_url: &str, topic: &str) -> Self {
        Self {
            broker_url: broker_url.to_string(),
            topic: topic.to_string(),
        }
    }
}

#[async_trait]
impl EventForwarder for MqttForwarder {
    fn name(&self) -> &str {
        "mqtt"
    }

    async fn forward(
        &self,
        position: &Position,
        event: Option<&Event>,
    ) -> Result<(), ForwardError> {
        let payload = serde_json::to_string(&serde_json::json!({
            "position": position,
            "event": event,
        }))
        .map_err(|e| ForwardError::Send(e.to_string()))?;

        tracing::debug!(
            broker = %self.broker_url,
            topic = %self.topic,
            device_id = position.device_id,
            "Publishing to MQTT (stub)"
        );

        // A full implementation would use rumqttc or paho-mqtt.
        let _ = payload;
        Ok(())
    }
}
