use async_trait::async_trait;
use std::time::Duration;
use crate::{ScheduledTask, ScheduleError};

pub struct DeviceInactiveTask;
impl DeviceInactiveTask { pub fn new() -> Self { Self } }

#[async_trait]
impl ScheduledTask for DeviceInactiveTask {
    fn name(&self) -> &str { "device_inactive" }
    fn interval(&self) -> Duration { Duration::from_secs(300) }
    async fn run(&self) -> Result<(), ScheduleError> {
        tracing::debug!("Running device_inactive task");
        Ok(())
    }
}
