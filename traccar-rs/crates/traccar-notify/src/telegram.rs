use async_trait::async_trait;
use traccar_model::{Event, Position, User};

use crate::{Notificator, NotifyError};

/// Sends notifications via the Telegram Bot API.
pub struct TelegramNotificator {
    pub bot_token: String,
    client: reqwest::Client,
}

impl TelegramNotificator {
    pub fn new(bot_token: &str) -> Self {
        Self {
            bot_token: bot_token.to_string(),
            client: reqwest::Client::new(),
        }
    }
}

#[async_trait]
impl Notificator for TelegramNotificator {
    fn name(&self) -> &str {
        "telegram"
    }

    async fn send_notification(
        &self,
        event: &Event,
        position: &Position,
        user: &User,
    ) -> Result<(), NotifyError> {
        let chat_id = user
            .attributes
            .get("telegramChatId")
            .and_then(|v| v.as_str())
            .ok_or_else(|| NotifyError::Config("User has no telegramChatId".into()))?;

        let text = format!(
            "Event: {}\nDevice: {}\nPosition: {:.6}, {:.6}",
            event.event_type, event.device_id, position.latitude, position.longitude
        );

        let url = format!(
            "https://api.telegram.org/bot{}/sendMessage",
            self.bot_token
        );

        tracing::info!(chat_id = %chat_id, "Sending Telegram notification");

        self.client
            .post(&url)
            .json(&serde_json::json!({
                "chat_id": chat_id,
                "text": text,
            }))
            .send()
            .await
            .map_err(NotifyError::Http)?
            .error_for_status()
            .map_err(|e| NotifyError::Send(e.to_string()))?;

        Ok(())
    }
}
