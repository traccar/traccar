use async_trait::async_trait;
use chrono::{DateTime, Utc};
use crate::{ReportProvider, ReportError};

pub struct StopsReport;
impl StopsReport { pub fn new() -> Self { Self } }

#[async_trait]
impl ReportProvider for StopsReport {
    fn name(&self) -> &str { "stops" }
    async fn generate(&self, _device_ids: &[i64], _from: DateTime<Utc>, _to: DateTime<Utc>) -> Result<Vec<serde_json::Value>, ReportError> {
        tracing::debug!("Generating {} report", self.name());
        Ok(vec![])
    }
}
