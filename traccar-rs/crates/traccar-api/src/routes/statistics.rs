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
    Router::new().route("/api/statistics", get(list))
}

#[derive(Deserialize)]
pub struct StatisticsQuery {
    pub from: Option<String>,
    pub to: Option<String>,
}

async fn list(
    State(state): State<Arc<AppState>>,
    Query(query): Query<StatisticsQuery>,
) -> Result<Json<Vec<serde_json::Value>>, StatusCode> {
    let mut request = traccar_storage::Request::new(traccar_storage::Columns::All);
    if let (Some(from), Some(to)) = (&query.from, &query.to) {
        request = request.with_condition(traccar_storage::Condition::Between(
            "captureTime".into(),
            serde_json::json!(from),
            serde_json::json!(to),
        ));
    }
    let result = state
        .storage
        .get_objects("tc_statistics", &request)
        .await
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    Ok(Json(result))
}
