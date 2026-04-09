use async_trait::async_trait;
use chrono::{DateTime, Utc};
use std::sync::Arc;
use traccar_storage::Storage;

use crate::{ReportError, ReportProvider};

/// Events report: returns all events for the given devices in the time range.
pub struct EventsReport {
    storage: Arc<dyn Storage>,
}

impl EventsReport {
    pub fn new(storage: Arc<dyn Storage>) -> Self {
        Self { storage }
    }
}

#[async_trait]
impl ReportProvider for EventsReport {
    fn name(&self) -> &str {
        "events"
    }

    async fn generate(
        &self,
        device_ids: &[i64],
        from: DateTime<Utc>,
        to: DateTime<Utc>,
    ) -> Result<Vec<serde_json::Value>, ReportError> {
        tracing::debug!("Generating events report for {} devices", device_ids.len());
        let mut all = Vec::new();
        for &did in device_ids {
            let cond = traccar_storage::Condition::And(
                Box::new(traccar_storage::Condition::Equals(
                    "deviceId".into(),
                    serde_json::json!(did),
                )),
                Box::new(traccar_storage::Condition::Between(
                    "eventTime".into(),
                    serde_json::json!(from.to_rfc3339()),
                    serde_json::json!(to.to_rfc3339()),
                )),
            );
            let request = traccar_storage::Request::new(traccar_storage::Columns::All)
                .with_condition(cond);
            let rows = self.storage.get_objects("tc_events", &request).await?;
            all.extend(rows);
        }
        Ok(all)
    }
}
