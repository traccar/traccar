use async_trait::async_trait;
use traccar_model::{Event, Position, User};

use crate::{Notificator, NotifyError};

/// Sends notifications to the Traccar Client mobile app via Firebase.
pub struct TraccarClientNotificator {
    client: reqwest::Client,
}

impl TraccarClientNotificator {
    pub fn new() -> Self {
        Self {
            client: reqwest::Client::new(),
        }
    }
}

impl Default for TraccarClientNotificator {
    fn default() -> Self {
        Self::new()
    }
}

#[async_trait]
impl Notificator for TraccarClientNotificator {
    fn name(&self) -> &str {
        "traccar"
    }

    async fn send_notification(
        &self,
        event: &Event,
        _position: &Position,
        user: &User,
    ) -> Result<(), NotifyError> {
        let token = user
            .attributes
            .get("notificationToken")
            .and_then(|v| v.as_str())
            .ok_or_else(|| NotifyError::Config("User has no notificationToken".into()))?;

        tracing::info!(user_id = user.id, "Sending Traccar Client notification");

        self.client
            .post("https://www.traccar.org/push/")
            .json(&serde_json::json!({
                "token": token,
                "title": "Traccar",
                "body": format!("{} on device {}", event.event_type, event.device_id),
            }))
            .send()
            .await
            .map_err(NotifyError::Http)?;

        Ok(())
    }
}
