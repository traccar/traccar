use async_trait::async_trait;
use traccar_model::{Event, Position, User};

use crate::{Notificator, NotifyError};

/// Sends a device command as a notification action (e.g. engine stop on alarm).
pub struct CommandNotificator;

impl CommandNotificator {
    pub fn new() -> Self {
        Self
    }
}

impl Default for CommandNotificator {
    fn default() -> Self {
        Self::new()
    }
}

#[async_trait]
impl Notificator for CommandNotificator {
    fn name(&self) -> &str {
        "command"
    }

    async fn send_notification(
        &self,
        event: &Event,
        _position: &Position,
        _user: &User,
    ) -> Result<(), NotifyError> {
        // In a full implementation this would look up the notification's
        // associated command and queue it for the device.
        tracing::info!(
            device_id = event.device_id,
            event_type = %event.event_type,
            "Command notification triggered"
        );
        Ok(())
    }
}
