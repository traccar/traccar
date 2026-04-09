use async_trait::async_trait;
use chrono::{DateTime, Utc};
use crate::{ReportProvider, ReportError};

pub struct SummaryReport;
impl SummaryReport { pub fn new() -> Self { Self } }

#[async_trait]
impl ReportProvider for SummaryReport {
    fn name(&self) -> &str { "summary" }
    async fn generate(&self, _device_ids: &[i64], _from: DateTime<Utc>, _to: DateTime<Utc>) -> Result<Vec<serde_json::Value>, ReportError> {
        tracing::debug!("Generating {} report", self.name());
        Ok(vec![])
    }
}
