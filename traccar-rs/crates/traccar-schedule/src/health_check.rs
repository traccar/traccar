use async_trait::async_trait;
use std::time::Duration;
use crate::{ScheduledTask, ScheduleError};

pub struct HealthCheckTask;
impl HealthCheckTask { pub fn new() -> Self { Self } }

#[async_trait]
impl ScheduledTask for HealthCheckTask {
    fn name(&self) -> &str { "health_check" }
    fn interval(&self) -> Duration { Duration::from_secs(60) }
    async fn run(&self) -> Result<(), ScheduleError> {
        tracing::debug!("Running health_check task");
        Ok(())
    }
}
