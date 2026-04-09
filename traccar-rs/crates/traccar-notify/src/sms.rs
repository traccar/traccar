use async_trait::async_trait;
use traccar_model::{Event, Position, User};

use crate::{Notificator, NotifyError};

/// Sends SMS notifications via an HTTP gateway API.
pub struct SmsNotificator {
    pub api_url: String,
    client: reqwest::Client,
}

impl SmsNotificator {
    pub fn new(api_url: &str) -> Self {
        Self {
            api_url: api_url.to_string(),
            client: reqwest::Client::new(),
        }
    }
}

#[async_trait]
impl Notificator for SmsNotificator {
    fn name(&self) -> &str {
        "sms"
    }

    async fn send_notification(
        &self,
        event: &Event,
        _position: &Position,
        user: &User,
    ) -> Result<(), NotifyError> {
        let phone = user
            .phone
            .as_deref()
            .ok_or_else(|| NotifyError::Config("User has no phone number".into()))?;

        let message = format!(
            "Traccar: {} on device {}",
            event.event_type, event.device_id
        );

        tracing::info!(phone = %phone, "Sending SMS notification");

        self.client
            .post(&self.api_url)
            .json(&serde_json::json!({
                "phone": phone,
                "message": message,
            }))
            .send()
            .await
            .map_err(NotifyError::Http)?
            .error_for_status()
            .map_err(|e| NotifyError::Send(e.to_string()))?;

        Ok(())
    }
}
