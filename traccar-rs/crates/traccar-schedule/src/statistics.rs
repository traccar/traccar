use async_trait::async_trait;
use std::time::Duration;
use crate::{ScheduledTask, ScheduleError};

pub struct StatisticsTask;
impl StatisticsTask { pub fn new() -> Self { Self } }

#[async_trait]
impl ScheduledTask for StatisticsTask {
    fn name(&self) -> &str { "statistics" }
    fn interval(&self) -> Duration { Duration::from_secs(3600) }
    async fn run(&self) -> Result<(), ScheduleError> {
        tracing::debug!("Running statistics collection task");
        Ok(())
    }
}
