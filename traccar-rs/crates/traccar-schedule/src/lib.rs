pub mod statistics;
pub mod device_inactive;
pub mod health_check;

use async_trait::async_trait;
use std::sync::Arc;
use std::time::Duration;
use traccar_storage::Storage;

#[derive(Debug, thiserror::Error)]
pub enum ScheduleError {
    #[error("Schedule error: {0}")]
    Task(String),
}

#[async_trait]
pub trait ScheduledTask: Send + Sync {
    fn name(&self) -> &str;
    fn interval(&self) -> Duration;
    async fn run(&self) -> Result<(), ScheduleError>;
}

pub async fn run_scheduler(_config: &(dyn std::any::Any + Send + Sync), _storage: Arc<dyn Storage>) {
    tracing::info!("Scheduler started");

    let tasks: Vec<Box<dyn ScheduledTask>> = vec![
        Box::new(statistics::StatisticsTask::new()),
        Box::new(device_inactive::DeviceInactiveTask::new()),
        Box::new(health_check::HealthCheckTask::new()),
    ];

    let mut handles = Vec::new();
    for task in tasks {
        let interval = task.interval();
        let name = task.name().to_string();
        handles.push(tokio::spawn(async move {
            let mut ticker = tokio::time::interval(interval);
            loop {
                ticker.tick().await;
                if let Err(e) = task.run().await {
                    tracing::error!("Scheduled task {} failed: {}", name, e);
                }
            }
        }));
    }

    // Wait forever
    futures::future::pending::<()>().await;
}
