pub mod trips;
pub mod route;
pub mod stops;
pub mod events;
pub mod summary;
pub mod combined;

use async_trait::async_trait;
use chrono::{DateTime, Utc};

#[derive(Debug, thiserror::Error)]
pub enum ReportError {
    #[error("Report error: {0}")]
    Generate(String),
}

#[async_trait]
pub trait ReportProvider: Send + Sync {
    fn name(&self) -> &str;
    async fn generate(
        &self,
        device_ids: &[i64],
        from: DateTime<Utc>,
        to: DateTime<Utc>,
    ) -> Result<Vec<serde_json::Value>, ReportError>;
}
