use async_trait::async_trait;
use chrono::{Duration, Utc};
use traccar_model::Position;
use crate::{HandlerError, HandlerState, PositionHandler};

pub struct TimeHandler {
    future_limit_seconds: i64,
}

impl TimeHandler {
    pub fn new(future_limit_seconds: i64) -> Self {
        Self {
            future_limit_seconds: if future_limit_seconds > 0 {
                future_limit_seconds
            } else {
                86400 // 24 hours default
            },
        }
    }
}

#[async_trait]
impl PositionHandler for TimeHandler {
    fn name(&self) -> &str {
        "time"
    }

    async fn handle_position(
        &self,
        position: &mut Position,
        _state: &dyn HandlerState,
    ) -> Result<bool, HandlerError> {
        let now = Utc::now();
        let future_limit = now + Duration::seconds(self.future_limit_seconds);

        // Filter out positions with future timestamps
        if position.fix_time > future_limit {
            tracing::warn!(
                "Position fix time {} is too far in the future, filtering",
                position.fix_time
            );
            return Ok(false);
        }

        Ok(true)
    }
}
