use async_trait::async_trait;
use traccar_model::{Event, Position, User};

use crate::{Notificator, NotifyError};

/// Pushes event notifications to connected WebSocket clients.
pub struct WebNotificator;

impl WebNotificator {
    pub fn new() -> Self {
        Self
    }
}

impl Default for WebNotificator {
    fn default() -> Self {
        Self::new()
    }
}

#[async_trait]
impl Notificator for WebNotificator {
    fn name(&self) -> &str {
        "web"
    }

    async fn send_notification(
        &self,
        event: &Event,
        position: &Position,
        user: &User,
    ) -> Result<(), NotifyError> {
        // In a full implementation this would push the event payload through
        // the WebSocketManager to the user's active connections.
        tracing::debug!(
            user_id = user.id,
            event_type = %event.event_type,
            device_id = event.device_id,
            lat = position.latitude,
            lon = position.longitude,
            "Pushing web notification"
        );
        Ok(())
    }
}
