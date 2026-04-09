use async_trait::async_trait;
use chrono::{Duration, Utc};
use traccar_model::Position;
use crate::{HandlerError, HandlerState, PositionHandler};

/// Filters positions older than a configurable threshold (in seconds).
pub struct OutdatedHandler {
    threshold_seconds: i64,
}

impl OutdatedHandler {
    pub fn new(threshold_seconds: i64) -> Self {
        Self {
            threshold_seconds: if threshold_seconds > 0 {
                threshold_seconds
            } else {
                600 // 10 minutes default
            },
        }
    }
}

#[async_trait]
impl PositionHandler for OutdatedHandler {
    fn name(&self) -> &str {
        "outdated"
    }

    async fn handle_position(
        &self,
        position: &mut Position,
        _state: &dyn HandlerState,
    ) -> Result<bool, HandlerError> {
        let now = Utc::now();
        let age = now.signed_duration_since(position.fix_time);

        if age > Duration::seconds(self.threshold_seconds) {
            tracing::debug!(
                device_id = position.device_id,
                age_secs = age.num_seconds(),
                "Position is outdated, marking"
            );
            position.outdated = true;
        }

        Ok(true)
    }
}
