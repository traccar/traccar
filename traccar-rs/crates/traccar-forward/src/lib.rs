pub mod json;
pub mod kafka;
pub mod mqtt;
pub mod amqp;
pub mod redis;

use async_trait::async_trait;
use traccar_model::{Event, Position};

#[derive(Debug, thiserror::Error)]
pub enum ForwardError {
    #[error("Forward error: {0}")]
    Send(String),
    #[error("HTTP error: {0}")]
    Http(#[from] reqwest::Error),
    #[error("Configuration error: {0}")]
    Config(String),
}

/// Trait for forwarding position and event data to external systems.
#[async_trait]
pub trait EventForwarder: Send + Sync {
    /// Forwarder name for logging.
    fn name(&self) -> &str;

    /// Forward a position (and optional event) to the external system.
    async fn forward(&self, position: &Position, event: Option<&Event>) -> Result<(), ForwardError>;
}

/// Manager that dispatches to multiple forwarders.
pub struct ForwardManager {
    forwarders: Vec<Box<dyn EventForwarder>>,
}

impl ForwardManager {
    pub fn new() -> Self {
        Self {
            forwarders: Vec::new(),
        }
    }

    pub fn add(&mut self, forwarder: Box<dyn EventForwarder>) {
        self.forwarders.push(forwarder);
    }

    /// Forward a position to all registered forwarders.
    pub async fn forward_position(&self, position: &Position, event: Option<&Event>) {
        for fwd in &self.forwarders {
            if let Err(e) = fwd.forward(position, event).await {
                tracing::error!(forwarder = fwd.name(), "Forward failed: {}", e);
            }
        }
    }
}

impl Default for ForwardManager {
    fn default() -> Self {
        Self::new()
    }
}
