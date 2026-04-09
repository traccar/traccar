use async_trait::async_trait;
use traccar_model::{Event, Position, User};

use crate::{Notificator, NotifyError};

/// Sends push notifications via Firebase Cloud Messaging.
pub struct FirebaseNotificator {
    pub server_key: String,
    client: reqwest::Client,
}

impl FirebaseNotificator {
    pub fn new(server_key: &str) -> Self {
        Self {
            server_key: server_key.to_string(),
            client: reqwest::Client::new(),
        }
    }
}

#[async_trait]
impl Notificator for FirebaseNotificator {
    fn name(&self) -> &str {
        "firebase"
    }

    async fn send_notification(
        &self,
        event: &Event,
        _position: &Position,
        user: &User,
    ) -> Result<(), NotifyError> {
        let fcm_token = user
            .attributes
            .get("firebaseToken")
            .and_then(|v| v.as_str())
            .ok_or_else(|| NotifyError::Config("User has no firebaseToken".into()))?;

        tracing::info!(user_id = user.id, "Sending Firebase notification");

        self.client
            .post("https://fcm.googleapis.com/fcm/send")
            .header("Authorization", format!("key={}", self.server_key))
            .json(&serde_json::json!({
                "to": fcm_token,
                "notification": {
                    "title": "Traccar",
                    "body": format!("{} on device {}", event.event_type, event.device_id),
                },
            }))
            .send()
            .await
            .map_err(NotifyError::Http)?
            .error_for_status()
            .map_err(|e| NotifyError::Send(e.to_string()))?;

        Ok(())
    }
}
