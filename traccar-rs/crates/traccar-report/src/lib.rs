pub mod trips;
pub mod route;
pub mod stops;
pub mod events;
pub mod summary;
pub mod combined;

use async_trait::async_trait;
use chrono::{DateTime, Utc};
use std::sync::Arc;
use traccar_storage::Storage;

#[derive(Debug, thiserror::Error)]
pub enum ReportError {
    #[error("Report generation error: {0}")]
    Generate(String),
    #[error("Storage error: {0}")]
    Storage(String),
}

impl From<traccar_storage::StorageError> for ReportError {
    fn from(e: traccar_storage::StorageError) -> Self {
        ReportError::Storage(e.to_string())
    }
}

/// Trait for report generators.
#[async_trait]
pub trait ReportProvider: Send + Sync {
    /// Report type name.
    fn name(&self) -> &str;

    /// Generate a report for the given devices and time range.
    async fn generate(
        &self,
        device_ids: &[i64],
        from: DateTime<Utc>,
        to: DateTime<Utc>,
    ) -> Result<Vec<serde_json::Value>, ReportError>;
}

/// Fetch positions for a set of devices in a time range.
pub(crate) async fn fetch_positions(
    storage: &dyn Storage,
    device_ids: &[i64],
    from: DateTime<Utc>,
    to: DateTime<Utc>,
) -> Result<Vec<serde_json::Value>, ReportError> {
    let mut all = Vec::new();
    for &did in device_ids {
        let cond = traccar_storage::Condition::And(
            Box::new(traccar_storage::Condition::Equals(
                "deviceId".into(),
                serde_json::json!(did),
            )),
            Box::new(traccar_storage::Condition::Between(
                "fixTime".into(),
                serde_json::json!(from.to_rfc3339()),
                serde_json::json!(to.to_rfc3339()),
            )),
        );
        let request = traccar_storage::Request::new(traccar_storage::Columns::All)
            .with_condition(cond)
            .with_order(traccar_storage::QueryOrder {
                column: "fixTime".into(),
                descending: false,
                limit: 0,
                offset: 0,
            });
        let rows = storage.get_objects("tc_positions", &request).await?;
        all.extend(rows);
    }
    Ok(all)
}
