pub mod command;
pub mod email;
pub mod firebase;
pub mod pushover;
pub mod sms;
pub mod telegram;
pub mod traccar_client;
pub mod web;

use async_trait::async_trait;
use traccar_model::{Event, Position, User};

#[derive(Debug, thiserror::Error)]
pub enum NotifyError {
    #[error("Failed to send notification: {0}")]
    Send(String),
    #[error("Configuration error: {0}")]
    Config(String),
    #[error("HTTP error: {0}")]
    Http(#[from] reqwest::Error),
}

/// Trait implemented by each notification channel.
#[async_trait]
pub trait Notificator: Send + Sync {
    /// Channel name (e.g. "mail", "sms", "telegram").
    fn name(&self) -> &str;

    /// Send a notification for the given event to a single user.
    async fn send_notification(
        &self,
        event: &Event,
        position: &Position,
        user: &User,
    ) -> Result<(), NotifyError>;
}

/// Routes events to the appropriate notification channels.
pub struct NotificationManager {
    notificators: Vec<Box<dyn Notificator>>,
}

impl NotificationManager {
    pub fn new() -> Self {
        Self {
            notificators: Vec::new(),
        }
    }

    /// Register a notificator.
    pub fn add(&mut self, notificator: Box<dyn Notificator>) {
        self.notificators.push(notificator);
    }

    /// Dispatch an event to notificators whose names appear in `channels`.
    pub async fn notify(
        &self,
        channels: &[&str],
        event: &Event,
        position: &Position,
        users: &[User],
    ) {
        for notificator in &self.notificators {
            if !channels.contains(&notificator.name()) {
                continue;
            }
            for user in users {
                if let Err(e) = notificator.send_notification(event, position, user).await {
                    tracing::error!(
                        channel = notificator.name(),
                        user_id = user.id,
                        "Notification failed: {}",
                        e
                    );
                }
            }
        }
    }

    /// Process a batch of (event, position) pairs, notifying all registered channels.
    pub async fn update_events(&self, events: &[(Event, Position)], users: &[User]) {
        let all_names: Vec<&str> = self.notificators.iter().map(|n| n.name()).collect();
        for (event, position) in events {
            self.notify(&all_names, event, position, users).await;
        }
    }
}

impl Default for NotificationManager {
    fn default() -> Self {
        Self::new()
    }
}
