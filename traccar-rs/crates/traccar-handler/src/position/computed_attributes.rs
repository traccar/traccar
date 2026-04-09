use async_trait::async_trait;
use traccar_model::Position;
use crate::{HandlerError, HandlerState, PositionHandler};

/// Evaluates user-defined computed attribute expressions and stores
/// the results as position attributes.
///
/// In a full implementation this would load `Attribute` definitions from
/// the database, evaluate their expressions against the position, and
/// set the computed values.
pub struct ComputedAttributesHandler;

impl ComputedAttributesHandler {
    pub fn new() -> Self {
        Self
    }
}

#[async_trait]
impl PositionHandler for ComputedAttributesHandler {
    fn name(&self) -> &str {
        "computed_attributes"
    }

    async fn handle_position(
        &self,
        _position: &mut Position,
        _state: &dyn HandlerState,
    ) -> Result<bool, HandlerError> {
        // TODO: load Attribute definitions, evaluate expressions, and set
        // computed values on the position.
        Ok(true)
    }
}
