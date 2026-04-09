use async_trait::async_trait;
use traccar_model::{Event, Position, User};
use crate::{Notificator, NotifyError};

pub struct Notifier;

impl Notifier {
    pub fn new() -> Self { Self }
}

#[async_trait]
impl Notificator for Notifier {
    fn name(&self) -> &str { "pushover" }
    async fn send_notification(&self, _event: &Event, _position: &Position, _user: &User) -> Result<(), NotifyError> {
        tracing::debug!("Sending {} notification", self.name());
        Ok(())
    }
}
