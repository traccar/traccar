use async_trait::async_trait;
use chrono::{DateTime, Utc};
use crate::{ReportProvider, ReportError};

pub struct TripsReport;
impl TripsReport { pub fn new() -> Self { Self } }

#[async_trait]
impl ReportProvider for TripsReport {
    fn name(&self) -> &str { "trips" }
    async fn generate(&self, _device_ids: &[i64], _from: DateTime<Utc>, _to: DateTime<Utc>) -> Result<Vec<serde_json::Value>, ReportError> {
        tracing::debug!("Generating {} report", self.name());
        Ok(vec![])
    }
}
