use bytes::BytesMut;
use traccar_model::Position;
use crate::ProtocolError;

pub struct TlvDecoder;

impl TlvDecoder {
    pub const PROTOCOL_NAME: &str = "tlv";

    pub fn new() -> Self {
        Self
    }

    pub fn decode(&self, _buf: &mut BytesMut) -> Result<Vec<Position>, ProtocolError> {
        tracing::debug!("Decoding {} protocol data", Self::PROTOCOL_NAME);
        Ok(vec![])
    }
}
