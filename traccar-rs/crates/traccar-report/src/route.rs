use async_trait::async_trait;
use chrono::{DateTime, Utc};
use std::sync::Arc;
use traccar_storage::Storage;

use crate::{ReportError, ReportProvider};

/// Route report: returns all positions for the given devices in the time range.
pub struct RouteReport {
    storage: Arc<dyn Storage>,
}

impl RouteReport {
    pub fn new(storage: Arc<dyn Storage>) -> Self {
        Self { storage }
    }
}

#[async_trait]
impl ReportProvider for RouteReport {
    fn name(&self) -> &str {
        "route"
    }

    async fn generate(
        &self,
        device_ids: &[i64],
        from: DateTime<Utc>,
        to: DateTime<Utc>,
    ) -> Result<Vec<serde_json::Value>, ReportError> {
        tracing::debug!("Generating route report for {} devices", device_ids.len());
        crate::fetch_positions(self.storage.as_ref(), device_ids, from, to).await
    }
}
