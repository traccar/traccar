use async_trait::async_trait;
use traccar_model::{Event, Position};

use crate::{EventForwarder, ForwardError};

/// Forwards position data as JSON via HTTP POST.
pub struct JsonForwarder {
    url: String,
    client: reqwest::Client,
}

impl JsonForwarder {
    pub fn new(url: &str) -> Self {
        Self {
            url: url.to_string(),
            client: reqwest::Client::new(),
        }
    }
}

#[async_trait]
impl EventForwarder for JsonForwarder {
    fn name(&self) -> &str {
        "json"
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

        self.client
            .post(&self.url)
            .json(&payload)
            .send()
            .await
            .map_err(ForwardError::Http)?
            .error_for_status()
            .map_err(|e| ForwardError::Send(e.to_string()))?;

        Ok(())
    }
}
