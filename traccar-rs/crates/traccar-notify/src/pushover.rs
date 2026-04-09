use async_trait::async_trait;
use traccar_model::{Event, Position, User};

use crate::{Notificator, NotifyError};

/// Sends notifications via the Pushover API.
pub struct PushoverNotificator {
    pub api_token: String,
    client: reqwest::Client,
}

impl PushoverNotificator {
    pub fn new(api_token: &str) -> Self {
        Self {
            api_token: api_token.to_string(),
            client: reqwest::Client::new(),
        }
    }
}

#[async_trait]
impl Notificator for PushoverNotificator {
    fn name(&self) -> &str {
        "pushover"
    }

    async fn send_notification(
        &self,
        event: &Event,
        _position: &Position,
        user: &User,
    ) -> Result<(), NotifyError> {
        let user_key = user
            .attributes
            .get("pushoverUserKey")
            .and_then(|v| v.as_str())
            .ok_or_else(|| NotifyError::Config("User has no pushoverUserKey".into()))?;

        tracing::info!(user_id = user.id, "Sending Pushover notification");

        self.client
            .post("https://api.pushover.net/1/messages.json")
            .form(&[
                ("token", self.api_token.as_str()),
                ("user", user_key),
                ("title", "Traccar"),
                ("message", &format!("{} on device {}", event.event_type, event.device_id)),
            ])
            .send()
            .await
            .map_err(NotifyError::Http)?
            .error_for_status()
            .map_err(|e| NotifyError::Send(e.to_string()))?;

        Ok(())
    }
}
