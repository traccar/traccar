use async_trait::async_trait;
use traccar_model::{Event, Position};

use crate::{EventForwarder, ForwardError};

/// Forwards position data to Redis (via PUBLISH or stream append).
pub struct RedisForwarder {
    pub url: String,
    pub channel: String,
}

impl RedisForwarder {
    pub fn new(url: &str, channel: &str) -> Self {
        Self {
            url: url.to_string(),
            channel: channel.to_string(),
        }
    }
}

#[async_trait]
impl EventForwarder for RedisForwarder {
    fn name(&self) -> &str {
        "redis"
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
            url = %self.url,
            channel = %self.channel,
            device_id = position.device_id,
            "Publishing to Redis (stub)"
        );

        // A full implementation would use the redis crate.
        let _ = payload;
        Ok(())
    }
}
