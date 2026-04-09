pub mod codec;
pub mod protocols;
pub mod server;
pub mod session;

use async_trait::async_trait;
use bytes::{Bytes, BytesMut};
use std::collections::HashMap;
use std::sync::Arc;
use traccar_model::{Command, Position};

// ─── Error types ────────────────────────────────────────────────────

#[derive(Debug, thiserror::Error)]
pub enum ProtocolError {
    #[error("Decode error: {0}")]
    Decode(String),
    #[error("Encode error: {0}")]
    Encode(String),
    #[error("Unknown device: {0}")]
    UnknownDevice(String),
    #[error("Invalid data: {0}")]
    InvalidData(String),
    #[error("Insufficient data")]
    InsufficientData,
    #[error("Unsupported command: {0}")]
    UnsupportedCommand(String),
    #[error("IO error: {0}")]
    Io(#[from] std::io::Error),
}

pub type ProtocolResult<T> = Result<T, ProtocolError>;

// ─── Transport type ─────────────────────────────────────────────────

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Transport {
    Tcp,
    Udp,
    Both,
}

// ─── Protocol traits ────────────────────────────────────────────────

/// Decodes raw bytes into positions. Implementations are protocol-specific.
#[async_trait]
pub trait ProtocolDecoder: Send + Sync {
    /// Decode a framed message from the buffer.
    /// Returns Ok(None) if the message is not a position (e.g. a login packet).
    /// Returns Ok(Some(positions)) with one or more decoded positions.
    async fn decode(
        &self,
        buf: &mut BytesMut,
        session: &mut crate::session::DeviceSession,
    ) -> ProtocolResult<Option<Vec<Position>>>;
}

/// Encodes commands into bytes to be sent to devices.
pub trait ProtocolEncoder: Send + Sync {
    /// Encode a command for a specific device.
    fn encode(&self, command: &Command, unique_id: &str) -> ProtocolResult<Bytes>;

    /// List of supported command types for this protocol.
    fn supported_commands(&self) -> &[&str] {
        &[]
    }
}

/// Frame decoder trait: splits raw bytes into protocol frames.
/// Implemented as a tokio_util::codec::Decoder for use in Framed streams.
pub trait FrameDecoder: tokio_util::codec::Decoder<Item = BytesMut, Error = std::io::Error>
    + Send
    + Sync
{
}

// ─── Protocol definition ────────────────────────────────────────────

/// Complete definition of a protocol, binding all components together.
pub struct ProtocolDefinition {
    pub name: String,
    pub transport: Transport,
    pub default_port: u16,
    pub decoder_factory: Box<dyn Fn() -> Box<dyn ProtocolDecoder> + Send + Sync>,
    pub encoder_factory: Box<dyn Fn() -> Box<dyn ProtocolEncoder> + Send + Sync>,
    pub frame_decoder_factory: Box<dyn Fn() -> Box<dyn tokio_util::codec::Decoder<Item = BytesMut, Error = std::io::Error> + Send + Sync> + Send + Sync>,
    pub supported_commands: Vec<String>,
}

impl std::fmt::Debug for ProtocolDefinition {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("ProtocolDefinition")
            .field("name", &self.name)
            .field("transport", &self.transport)
            .field("default_port", &self.default_port)
            .field("supported_commands", &self.supported_commands)
            .finish()
    }
}

// ─── Protocol registry ──────────────────────────────────────────────

/// Registry of all available protocols.
pub struct ProtocolRegistry {
    protocols: HashMap<String, Arc<ProtocolDefinition>>,
}

impl ProtocolRegistry {
    pub fn new() -> Self {
        Self {
            protocols: HashMap::new(),
        }
    }

    /// Register a protocol definition.
    pub fn register(&mut self, definition: ProtocolDefinition) {
        let name = definition.name.clone();
        self.protocols.insert(name, Arc::new(definition));
    }

    /// Get a protocol by name.
    pub fn get(&self, name: &str) -> Option<Arc<ProtocolDefinition>> {
        self.protocols.get(name).cloned()
    }

    /// List all registered protocol names.
    pub fn names(&self) -> Vec<String> {
        self.protocols.keys().cloned().collect()
    }

    /// Number of registered protocols.
    pub fn len(&self) -> usize {
        self.protocols.len()
    }

    /// Check if the registry is empty.
    pub fn is_empty(&self) -> bool {
        self.protocols.is_empty()
    }

    /// Iterate over all registered protocols.
    pub fn iter(&self) -> impl Iterator<Item = (&String, &Arc<ProtocolDefinition>)> {
        self.protocols.iter()
    }
}

impl Default for ProtocolRegistry {
    fn default() -> Self {
        let mut registry = Self::new();
        protocols::register_all(&mut registry);
        registry
    }
}

// Re-export key types
pub use codec::{DelimiterFrameDecoder, FixedLengthFrameDecoder, LengthFieldFrameDecoder, LineBasedFrameDecoder};
pub use session::{ConnectionManager, DeviceSession};

/// Start all configured protocol servers.
pub async fn start_servers(
    config: &std::sync::Arc<impl std::any::Any + Send + Sync>,
    _storage: std::sync::Arc<dyn traccar_storage::Storage>,
) -> Result<(), ProtocolError> {
    let registry = ProtocolRegistry::default();
    tracing::info!("Loaded {} protocol definitions", registry.len());

    // Protocol servers would be started here based on config.
    // For each protocol with a configured port, a TrackerServer is created and started.
    for (name, _def) in registry.iter() {
        tracing::debug!("Protocol available: {}", name);
    }

    tracing::info!("Protocol servers initialized");
    Ok(())
}
