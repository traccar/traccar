use async_trait::async_trait;
use traccar_model::{Event, Position};
use crate::{BroadcastError, BroadcastService};

pub struct MulticastBroadcast;
impl MulticastBroadcast { pub fn new() -> Self { Self } }

#[async_trait]
impl BroadcastService for MulticastBroadcast {
    async fn update_position(&self, _position: &Position) -> Result<(), BroadcastError> { Ok(()) }
    async fn update_event(&self, _event: &Event) -> Result<(), BroadcastError> { Ok(()) }
}
