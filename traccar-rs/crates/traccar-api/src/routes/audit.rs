use axum::{
    extract::{Query, State},
    http::StatusCode,
    routing::get,
    Json, Router,
};
use serde::Deserialize;
use std::sync::Arc;

use crate::AppState;

pub fn routes() -> Router<Arc<AppState>> {
    Router::new().route("/api/audit", get(list))
}

#[derive(Deserialize)]
pub struct AuditQuery {
    pub from: Option<String>,
    pub to: Option<String>,
    pub user_id: Option<i64>,
}

/// GET /audit - list audit log entries.
async fn list(
    State(state): State<Arc<AppState>>,
    Query(query): Query<AuditQuery>,
) -> Result<Json<Vec<serde_json::Value>>, StatusCode> {
    // Audit logs are stored in a dedicated table (or the events table filtered by type).
    let mut conditions: Vec<traccar_storage::Condition> = Vec::new();

    if let (Some(from), Some(to)) = (&query.from, &query.to) {
        conditions.push(traccar_storage::Condition::Between(
            "eventTime".into(),
            serde_json::json!(from),
            serde_json::json!(to),
        ));
    }
    if let Some(uid) = query.user_id {
        conditions.push(traccar_storage::Condition::Equals(
            "userId".into(),
            serde_json::json!(uid),
        ));
    }

    let mut request = traccar_storage::Request::new(traccar_storage::Columns::All);
    if let Some(cond) = traccar_storage::Condition::merge(conditions) {
        request = request.with_condition(cond);
    }

    // Fall back to events table for audit data
    let result = state
        .storage
        .get_objects("tc_events", &request)
        .await
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    Ok(Json(result))
}
