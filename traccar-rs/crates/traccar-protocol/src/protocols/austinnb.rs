use bytes::BytesMut;
use traccar_model::Position;
use crate::ProtocolError;

pub struct AustinnbDecoder;

impl AustinnbDecoder {
    pub const PROTOCOL_NAME: &str = "austinnb";

    pub fn new() -> Self {
        Self
    }

    pub fn decode(&self, _buf: &mut BytesMut) -> Result<Vec<Position>, ProtocolError> {
        tracing::debug!("Decoding {} protocol data", Self::PROTOCOL_NAME);
        Ok(vec![])
    }
}
